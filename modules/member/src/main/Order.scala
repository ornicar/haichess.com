package lila.member

import lila.user.User
import org.joda.time.DateTime

case class Order(
    _id: String, // 订单编号(no)
    typ: ProductType,
    totalAmount: BigDecimal, // 订单总金额
    payAmount: BigDecimal, // 应付金额（实际支付金额）
    amounts: OrderAmounts, // 优惠金额
    payWay: PayWay, // 支付方式（支付宝、微信）
    status: OrderStatus, // 订单状态（待付款；已取消；待发货（已付款）；已完成；已关闭；）
    createAt: DateTime, // 提交时间
    createBy: User.ID, // 提交者
    payAt: Option[DateTime], // 支付时间
    deliverAt: Option[DateTime], // 发货时间
    note: Option[String], // 订单备注
    products: OrderProducts
) {

  def id = _id

  def isCreate = status == OrderStatus.Create
  def isPayCompleted = status == OrderStatus.PayCompleted
  def isCompleted = status == OrderStatus.Completed
  def isCanceled = status == OrderStatus.Canceled
  def isClosed = status == OrderStatus.Closed

  def desc = s"${products.productName}${(typ == ProductType.VirtualMemberCard).??("卡")}（${products.productItemName}）"

  def descWithCount = desc + "× " + products.count
}

case class OrderAmounts(
    silverDays: Option[Int], // 银牌会员抵扣天数
    silverDaysAmount: Option[BigDecimal], // 银牌会员抵扣金额
    promotions: OrderPromotions, // 折扣抵扣
    points: Option[Int], // 使用积分
    pointsAmount: Option[BigDecimal], // 积分抵扣金额
    coupon: Option[String], // 优惠券码
    couponAmount: Option[BigDecimal], // 优惠券抵扣金额
    inviteUser: Option[String], // 邀请码（User.ID）
    inviteUserAmount: Option[BigDecimal] // 邀请码抵扣金额
) {

  def isEmpty = {
    val zero = BigDecimal(0.00)
    (silverDaysAmount.isEmpty || silverDaysAmount.??(_ == zero)) &&
      (pointsAmount.isEmpty || pointsAmount.??(_ == zero)) &&
      (couponAmount.isEmpty || couponAmount.??(_ == zero)) &&
      (inviteUserAmount.isEmpty || inviteUserAmount.??(_ == zero)) &&
      !promotions.nonEmpty
  }
}

case class OrderProducts(
    productId: String, // 商品ID
    productName: String, // 商品名称
    productItemCode: String, // 规格编码
    productItemName: String, // 规格名称
    price: BigDecimal, // 单价
    attrs: Map[String, String],
    count: Int
)

case class OrderPromotions(
    pricePromotion: Option[OrderPricePromotion],
    memberPromotion: Option[OrderMemberPromotion],
    ladderPromotion: Option[OrderLadderPromotion],
    fullReducePromotion: Option[OrderFullReducePromotion]
) {

  val all = List(pricePromotion, memberPromotion, ladderPromotion, fullReducePromotion).filter(_.nonEmpty).map(_.get)

  def nonEmpty = all.nonEmpty

  def outPrice(sourcePrice: BigDecimal) = {
    val p1 = fullReducePromotion.map(_.outPrice)
    val p2 = ladderPromotion.map(_.outPrice)
    val p3 = memberPromotion.map(_.outPrice)
    val p4 = pricePromotion.map(_.outPrice)

    if (p1.isDefined) p1.get
    else if (p2.isDefined) p2.get
    else if (p3.isDefined) p3.get
    else if (p4.isDefined) p4.get
    else sourcePrice
  }

}

object OrderPromotions {

  def of(promotions: Promotions, sourcePrice: BigDecimal, count: Int, user: User) = {
    val priceIn = sourcePrice
    val pricePromotion = promotions.pricePromotions.find(_.fit(priceIn, count, user)).map { promotion =>
      OrderPricePromotion(
        inPrice = priceIn,
        outPrice = promotion.calc(priceIn, count, user),
        promotion = promotion
      )
    }
    val priceOut = pricePromotion.map(_.outPrice) | priceIn

    val memberIn = priceOut
    val memberPromotion = promotions.memberPromotions.find(_.fit(memberIn, count, user)).map { promotion =>
      OrderMemberPromotion(
        inPrice = memberIn,
        outPrice = promotion.calc(memberIn, count, user),
        promotion = promotion
      )
    }
    val memberOut = memberPromotion.map(_.outPrice) | memberIn

    val ladderIn = memberOut
    val ladderPromotion = promotions.ladderPromotions.find(_.fit(ladderIn, count, user)).map { promotion =>
      OrderLadderPromotion(
        inPrice = ladderIn,
        outPrice = promotion.calc(ladderIn, count, user),
        promotion = promotion
      )
    }
    val ladderOut = ladderPromotion.map(_.outPrice) | ladderIn

    val fullReduceIn = ladderOut
    val fullReducePromotion = promotions.fullReducePromotions.find(_.fit(fullReduceIn, count, user)).map { promotion =>
      OrderFullReducePromotion(
        inPrice = fullReduceIn,
        outPrice = promotion.calc(fullReduceIn, count, user),
        promotion = promotion
      )
    }
    val fullReduceOut = fullReducePromotion.map(_.outPrice) | fullReduceIn

    OrderPromotions(
      pricePromotion = pricePromotion,
      memberPromotion = memberPromotion,
      ladderPromotion = ladderPromotion,
      fullReducePromotion = fullReducePromotion
    )
  }
}

case class OrderPricePromotion(
    inPrice: BigDecimal,
    outPrice: BigDecimal,
    promotion: PricePromotion
)

case class OrderMemberPromotion(
    inPrice: BigDecimal,
    outPrice: BigDecimal,
    promotion: MemberPromotion
)

case class OrderLadderPromotion(
    inPrice: BigDecimal,
    outPrice: BigDecimal,
    promotion: LadderPromotion
)

case class OrderFullReducePromotion(
    inPrice: BigDecimal,
    outPrice: BigDecimal,
    promotion: FullReducePromotion
)

sealed abstract class PayWay(val id: String, val name: String)
object PayWay {
  case object Alipay extends PayWay("alipay", "支付宝")
  case object WeChat extends PayWay("wechat", "微信")

  val all = List(Alipay)

  def choices = all.map(d => d.id -> d.name)
  def apply(id: String): PayWay = all.find(_.id == id) err s"can not find PayWay $id"
}

sealed abstract class OrderStatus(val id: Int, val name: String) {
  def isComplete = id >= 50
}
object OrderStatus {
  case object Create extends OrderStatus(10, "待付款")
  case object PayCompleted extends OrderStatus(20, "待发货（已付款）")
  case object Completed extends OrderStatus(50, "已完成")
  case object Canceled extends OrderStatus(60, "已取消")
  case object Closed extends OrderStatus(70, "已关闭")

  val all = List(Create, PayCompleted, Completed, Canceled, Closed)
  def apply(id: Int): OrderStatus = all.find(_.id == id) err s"can not find OrderStatus $id"
}
