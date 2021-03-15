package lila.member

import com.alipay.easysdk.factory.Factory.Payment
import com.alipay.easysdk.kernel.util.ResponseChecker
import lila.common.{ Bus, MaxPerPage }
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.notify.NotifyApi
import lila.user.{ User, UserRepo }
import org.joda.time.DateTime
import lila.hub.actorApi.member.{ MemberBuyPayed, MemberPointsSet }
import lila.member.ProductType.{ VirtualMember, VirtualMemberCard }
import ornicar.scalalib.Random
import scala.math.BigDecimal.RoundingMode

case class OrderApi(
    coll: Coll,
    orderStatusLogApi: OrderStatusLogApi,
    orderPayApi: OrderPayApi,
    memberCardApi: MemberCardApi,
    payTest: Boolean,
    appId: String,
    sellerId: String,
    bus: Bus,
    notifyApi: NotifyApi
) {

  import BSONHandlers.OrderBSONHandler

  def byId(id: String): Fu[Option[Order]] = coll.byId(id)

  def mine(userId: User.ID, typ: Option[ProductType] = none): Fu[List[Order]] = coll.find(
    $doc(
      "createBy" -> userId
    ) ++ typ.??(t => $doc("typ" -> t.id))
  ).sort($sort desc "createAt").list()

  def minePage(userId: User.ID, page: Int, orderSearch: OrderSearch): Fu[Paginator[Order]] = {
    var selector = $doc(
      "createBy" -> userId
    ) ++ orderSearch.typ.??(t => $doc("typ" -> t)) ++ orderSearch.level.??(l => $doc("products.attrs.level" -> l))

    if (orderSearch.dateMin.isDefined || orderSearch.dateMax.isDefined) {
      var dateRange = $doc()
      orderSearch.dateMin foreach { dateMin =>
        dateRange = dateRange ++ $gte(dateMin.withTimeAtStartOfDay())
      }
      orderSearch.dateMax foreach { dateMax =>
        dateRange = dateRange ++ $lte(dateMax.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999))
      }
      selector = selector ++ $doc("createAt" -> dateRange)
    }

    // println(BSONDocument.pretty(selector))

    val adapter = new Adapter[Order](
      collection = coll,
      selector = selector,
      projection = $empty,
      sort = $sort desc "createAt"
    )
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

  def findLastId(typ: ProductType, now: DateTime): Fu[Option[String]] = {
    coll.find($doc("typ" -> typ.id, "createAt" -> ($gte(now.withTimeAtStartOfDay()) ++ $lte(now.withTime(23, 59, 59, 999)))), $id(true))
      .sort($sort desc "_id")
      .uno[Bdoc] map {
        _ flatMap { doc => doc.getAs[String]("_id") }
      }
  }

  def setStatus(order: Order, status: OrderStatus, note: Option[String] = None): Funit =
    coll.update(
      $id(order.id),
      $set("status" -> status.id) ++ note.?? { n =>
        $set("note" -> n)
      }
    ).void >> orderStatusLogApi.setLog(
        order = order,
        status = status,
        userId = status match {
          case OrderStatus.PayCompleted | OrderStatus.Canceled => order.createBy
          case OrderStatus.Completed => "system"
          case _ => "-"
        },
        note = none
      )

  def toPay(data: OrderData, me: User): Fu[String] = {
    val now = DateTime.now
    val productType = ProductType(data.productTyp)
    val prefix = productType.prefix + payTest.??(Random nextString 4) + now.toString("yyyyMMdd")
    findLastId(ProductType(data.productTyp), now) map {
      case None => prefix + "000001"
      case Some(lastId) => {
        if (lastId.contains(prefix)) {
          prefix + "%06d".format(lastId.substring(lastId.length - 6).toInt + 1)
        } else {
          prefix + "000001"
        }
      }
    } flatMap { id =>
      val orderAmountInfo = OrderDirector.calcPrice(CalcPriceData.of(data), me)
      val order = Order(
        _id = id,
        typ = productType,
        totalAmount = orderAmountInfo.totalPrice,
        payAmount = orderAmountInfo.payPrice,
        amounts = orderAmountInfo.amounts,
        payWay = PayWay(data.payWay),
        status = OrderStatus.Create,
        createAt = now,
        createBy = me.id,
        payAt = None,
        deliverAt = None,
        note = None,
        products = OrderProducts(
          productId = orderAmountInfo.productId,
          productName = orderAmountInfo.productName,
          productItemCode = orderAmountInfo.productItemCode,
          productItemName = orderAmountInfo.productItemName,
          price = orderAmountInfo.price,
          attrs = orderAmountInfo.attrs,
          count = data.count
        )
      )
      coll.insert(order) >> pay(order)
    }
  }

  def pay(order: Order): Fu[String] = {
    if (order.payAmount.setScale(2, RoundingMode.DOWN).equals(BigDecimal(0.00))) {
      payCallback(order) inject s"""<script>setTimeout(function(){location.href='/member/order/${order.id}/payReturn'}, 2000);</script>"""
    } else {
      order.payWay match {
        case PayWay.Alipay => alipay(order)
        case PayWay.WeChat => fufail(s"not support wechat pay ${order.id}")
      }
    }
  }

  // 参考文档: https://opendocs.alipay.com/apis/api_1/alipay.trade.page.pay/?scene=API002020081300013629
  def alipay(order: Order): Fu[String] = {
    val expireAt = order.createAt.plusMinutes(if (payTest) 3 else 10)
    val response = Payment.Page()
      .asyncNotify("https://haichess.com/member/order/asyncNotify")
      .optional("goods_type", "0") // 商品主类型：0-虚拟类商品，1-实物类商品
      .optional("time_expire", expireAt.toString("yyyy-MM-dd HH:mm:ss")) // 超时时间
      .pay(
        order.descWithCount,
        order.id,
        if (payTest) "0.01" else priceOf(order.payAmount),
        s"http://haichess.com/member/order/${order.id}/payReturn"
      )

    if (ResponseChecker.success(response)) {
      orderPayApi.addPayData(order) inject response.getBody
    } else {
      fufail(s"alipay ${order.id} ${response.getBody}")
    }
  }

  // 参考文档: https://opendocs.alipay.com/open/270/105902
  def handleAlipayNotify(data: AlipayData, formParams: Map[String, Seq[String]]): Funit = {
    val parmaMap = paramsToUtilMap(formParams)
    val verifyNotify = Payment.Common().verifyNotify(parmaMap)
    if (verifyNotify) {
      byId(data.out_trade_no) flatMap {
        case None => fufail(s"can not find order ${data.out_trade_no}")
        case Some(order) => {
          val amountEquals = data.total_amount.??(amount => if (payTest) amount.equals(BigDecimal(0.01)) else amount.equals(order.totalAmount.setScale(2, RoundingMode.DOWN)))
          val appIdEquals = data.app_id == appId
          val sellerIdEquals = data.seller_id.??(_ == sellerId)
          if (amountEquals && appIdEquals && sellerIdEquals) {
            // WAIT_BUYER_PAY（交易创建，等待买家付款）、TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）
            // logger.info("formParams => " + formParams)
            orderPayApi.setAlipayData(order, data) >>
              (order.isCreate && data.trade_status.??(s => s == "TRADE_SUCCESS" || s == "TRADE_FINISHED")).??(payCallback(order)) >>
              (order.isCreate && data.trade_status.??(_ == "TRADE_CLOSED")).??(setStatus(order, OrderStatus.Canceled))
          } else {
            fufail(s"alipay verify failed, orderId: ${data.out_trade_no}, amountEquals: $amountEquals, appIdEquals: $appIdEquals, sellerIdEquals: $sellerIdEquals")
          }
        }
      }
    } else {
      fufail(s"alipay sign verify failed, orderId: ${data.out_trade_no}, urlParams: $formParams")
    }
  }

  private def paramsToUtilMap(formParams: Map[String, Seq[String]]) = {
    val map: java.util.Map[String, String] = new java.util.HashMap[String, String]()
    formParams.foreach { param =>
      map.put(param._1, param._2.headOption | "")
    }
    map
  }

  def payCallback(order: Order): Funit = {
    setStatus(order, OrderStatus.PayCompleted) >> {
      order.typ match {
        case VirtualMember => member.payCallback(order)
        case VirtualMemberCard => card.payCallback(order)
      }
    }
  }

  def expiredTesting: Funit =
    coll.find(
      $doc(
        "status" -> OrderStatus.Create.id,
        "createAt" $lt DateTime.now.minusMinutes(if (payTest) 3 else 10)
      )
    ).list(100) flatMap { cards =>
        cards.map { card =>
          setStatus(card, OrderStatus.Canceled, "支付超时".some)
        }.sequenceFu.void
      }

  object member {

    def payCallback(order: Order): Funit =
      UserRepo.byId(order.createBy) flatMap {
        case None => fufail(s"can not find user ${order.createBy} of ${order.id}")
        case Some(u) => {
          fuccess {
            bus.publish(
              MemberBuyPayed(
                orderId = order.id,
                userId = order.createBy,
                level = order.products.attrs.get("level") err s"make memberBuyPayed error by order ${order.id}",
                totalAmount = order.totalAmount,
                payAmount = order.payAmount,
                days = OrderDirector.toDays(u, order.products.attrs),
                points = order.amounts.points,
                coupon = order.amounts.coupon,
                inviteUser = order.amounts.inviteUser,
                desc = order.desc
              ), 'memberBuyPayed
            )
          }
        }
      }

    def completed(id: String): Funit =
      byId(id) flatMap {
        case None => fufail(s"order completed! but can not find order of $id")
        case Some(order) => setStatus(order, OrderStatus.Completed)
      }
  }

  object card {

    def payCallback(order: Order): Funit = {
      memberCardApi.addCardByOrder(order) >>- order.amounts.points.?? { points =>
        bus.publish(MemberPointsSet(order.createBy, points * -1, order.id.some), 'memberPointsSet)
      } >> completed(order)
    }

    def completed(order: Order): Funit = setStatus(order, OrderStatus.Completed)
  }

  private def priceOf(d: BigDecimal) = d.setScale(2, RoundingMode.DOWN).toString()

}
