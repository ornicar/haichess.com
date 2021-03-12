package lila.member

import play.api.data._
import play.api.data.Forms._
import lila.common.Form.{ ISODate, ISODateTime2, stringIn }
import org.joda.time.DateTime
import lila.user.User

final class DataForm {

  def order(user: User, discounts: List[(String, String)]) = Form(mapping(
    "productTyp" -> nonEmptyText,
    "productId" -> nonEmptyText,
    "itemCode" -> nonEmptyText,
    "count" -> number(1, 999),
    "points" -> optional(number(0, 10000)),
    "coupon" -> optional(nonEmptyText),
    "inviteUser" -> optional(nonEmptyText
      .verifying("不可以填写自己哦~", user.id != _)
      .verifying("邀请人必须是认证教练或俱乐部", discounts.map(_._1).contains(_))),
    "payWay" -> stringIn(PayWay.choices)
  )(OrderData.apply)(OrderData.unapply).verifying("请选择正确的产品", _ validLevel (user)))

  def calcPrice(user: User, discounts: List[(String, String)]) = Form(mapping(
    "productTyp" -> nonEmptyText,
    "productId" -> nonEmptyText,
    "itemCode" -> nonEmptyText,
    "count" -> number(1, 999),
    "points" -> optional(number(0, 10000)),
    "isPointsChange" -> boolean,
    "coupon" -> optional(nonEmptyText),
    "inviteUser" -> optional(nonEmptyText
      .verifying("不可以填写自己哦~", user.id != _)
      .verifying("邀请人必须是认证教练或俱乐部", discounts.map(_._1).contains(_)))
  )(CalcPriceData.apply)(CalcPriceData.unapply).verifying("请选择正确的产品", _ validLevel (user)))

  val orderSearch = Form(mapping(
    "typ" -> optional(stringIn(ProductType.choices)),
    "level" -> optional(stringIn(lila.user.MemberLevel.choices)),
    "dateMin" -> optional(ISODate.isoDate),
    "dateMax" -> optional(ISODate.isoDate)
  )(OrderSearch.apply)(OrderSearch.unapply))

  val cardLogSearch = Form(mapping(
    "username" -> optional(lila.user.DataForm.historicalUsernameField),
    "level" -> optional(stringIn(lila.user.MemberLevel.choices)),
    "status" -> optional(stringIn(MemberCard.CardStatus.choices)),
    "dateMin" -> optional(ISODate.isoDate),
    "dateMax" -> optional(ISODate.isoDate)
  )(MemberCardLogSearch.apply)(MemberCardLogSearch.unapply))

  val orderAlipayNofify = Form(mapping(
    "trade_no" -> nonEmptyText,
    "app_id" -> nonEmptyText,
    "out_trade_no" -> nonEmptyText,
    "out_biz_no" -> optional(nonEmptyText),
    "buyer_id" -> optional(nonEmptyText),
    "seller_id" -> optional(nonEmptyText),
    "trade_status" -> optional(nonEmptyText),
    "total_amount" -> optional(bigDecimal),
    "receipt_amount" -> optional(bigDecimal),
    "invoice_amount" -> optional(bigDecimal),
    "buyer_pay_amount" -> optional(bigDecimal),
    "point_amount" -> optional(bigDecimal),
    "refund_fee" -> optional(bigDecimal),
    "subject" -> optional(nonEmptyText),
    "body" -> optional(nonEmptyText),
    "gmt_create" -> optional(ISODateTime2.isoDate),
    "gmt_payment" -> optional(ISODateTime2.isoDate),
    "gmt_refund" -> optional(ISODateTime2.isoDate),
    "gmt_close" -> optional(ISODateTime2.isoDate),
    "fund_bill_list" -> optional(nonEmptyText),
    "voucher_detail_list" -> optional(nonEmptyText),
    "passback_params" -> optional(nonEmptyText)
  )(AlipayData.apply)(AlipayData.unapply))

}

case class CalcPriceData(
    productTyp: String,
    productId: String,
    itemCode: String,
    count: Int,
    points: Option[Int],
    isPointsChange: Boolean,
    coupon: Option[String],
    inviteUser: Option[String]
) {

  def validLevel(me: User) = {
    val typ = ProductType(productTyp)
    val ml = Product.toMemberLevel(productId)
    (typ == ProductType.VirtualMemberCard) || (ml.id > me.memberLevel.id || (ml == me.memberLevel && !me.memberOrDefault.lvWithExpire.isForever))
  }
}

object CalcPriceData {

  def of(data: OrderData) = CalcPriceData(
    productTyp = data.productTyp,
    productId = data.productId,
    itemCode = data.itemCode,
    count = data.count,
    points = data.points,
    isPointsChange = false,
    coupon = data.coupon,
    inviteUser = data.inviteUser
  )
}

case class OrderData(
    productTyp: String,
    productId: String,
    itemCode: String,
    count: Int,
    points: Option[Int],
    coupon: Option[String],
    inviteUser: Option[String],
    payWay: String
) {

  def validLevel(me: User) = {
    val typ = ProductType(productTyp)
    val ml = Product.toMemberLevel(productId)
    (typ == ProductType.VirtualMemberCard) || (ml.id > me.memberLevel.id || (ml == me.memberLevel && !me.memberOrDefault.lvWithExpire.isForever))
  }

}

case class OrderSearch(
    typ: Option[String],
    level: Option[String],
    dateMin: Option[DateTime],
    dateMax: Option[DateTime]
)

case class MemberCardLogSearch(
    username: Option[String],
    level: Option[String],
    status: Option[String],
    dateMin: Option[DateTime],
    dateMax: Option[DateTime]
)