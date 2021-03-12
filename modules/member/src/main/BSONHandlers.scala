package lila.member

import reactivemongo.bson._
import lila.db.dsl.bsonArrayToListHandler
import scala.math.BigDecimal.RoundingMode

private object BSONHandlers {

  import lila.db.dsl.BSONJodaDateTimeHandler

  implicit object BigDecimalHandler extends BSONHandler[BSONString, BigDecimal] {
    def read(b: BSONString) = BigDecimal(b.value)
    def write(d: BigDecimal) = BSONString(d.setScale(2, RoundingMode.DOWN).toString)
  }

  implicit val OrderStatusBSONHandler = new BSONHandler[BSONInteger, OrderStatus] {
    def read(b: BSONInteger): OrderStatus = OrderStatus(b.value)
    def write(x: OrderStatus) = BSONInteger(x.id)
  }

  implicit val PayWayBSONHandler = new BSONHandler[BSONString, PayWay] {
    def read(b: BSONString): PayWay = PayWay(b.value)
    def write(x: PayWay) = BSONString(x.id)
  }

  implicit val ProductTypeBSONHandler = new BSONHandler[BSONString, ProductType] {
    def read(b: BSONString): ProductType = ProductType(b.value)
    def write(x: ProductType) = BSONString(x.id)
  }

  implicit val MemberLevelLogTypeBSONHandler = new BSONHandler[BSONString, MemberLevelLog.MemberLevelChangeType] {
    def read(b: BSONString): MemberLevelLog.MemberLevelChangeType = MemberLevelLog.MemberLevelChangeType(b.value)
    def write(x: MemberLevelLog.MemberLevelChangeType) = BSONString(x.id)
  }

  implicit val UserMemberLevelBSONHandler = new BSONHandler[BSONString, lila.user.MemberLevel] {
    def read(b: BSONString): lila.user.MemberLevel = lila.user.MemberLevel(b.value)
    def write(x: lila.user.MemberLevel) = BSONString(x.code)
  }

  implicit val DaysBSONHandler = new BSONHandler[BSONString, Days] {
    def read(b: BSONString): Days = Days(b.value)
    def write(x: Days) = BSONString(x.id)
  }

  implicit val CardStatusBSONHandler = new BSONHandler[BSONString, MemberCard.CardStatus] {
    def read(b: BSONString): MemberCard.CardStatus = MemberCard.CardStatus(b.value)
    def write(x: MemberCard.CardStatus) = BSONString(x.id)
  }

  implicit val MemberPointsLogTypeBSONHandler = new BSONHandler[BSONString, MemberPointsLog.PointsType] {
    def read(b: BSONString): MemberPointsLog.PointsType = MemberPointsLog.PointsType(b.value)
    def write(x: MemberPointsLog.PointsType) = BSONString(x.id)
  }

  implicit val MemberCardLogTypeBSONHandler = new BSONHandler[BSONString, MemberCardLog.Type] {
    def read(b: BSONString): MemberCardLog.Type = MemberCardLog.Type(b.value)
    def write(x: MemberCardLog.Type) = BSONString(x.id)
  }

  implicit val PricePromotionBSONHandler = Macros.handler[PricePromotion]
  implicit val MemberPromotionBSONHandler = Macros.handler[MemberPromotion]
  implicit val LadderPromotionBSONHandler = Macros.handler[LadderPromotion]
  implicit val FullReducePromotionBSONHandler = Macros.handler[FullReducePromotion]

  implicit val PricePromotionArrayBSONHandler = bsonArrayToListHandler[PricePromotion]
  implicit val MemberPromotionArrayBSONHandler = bsonArrayToListHandler[MemberPromotion]
  implicit val LadderPromotionArrayBSONHandler = bsonArrayToListHandler[LadderPromotion]
  implicit val FullReducePromotionArrayBSONHandler = bsonArrayToListHandler[FullReducePromotion]

  implicit val OrderPricePromotionBSONHandler = Macros.handler[OrderPricePromotion]
  implicit val OrderMemberPromotionBSONHandler = Macros.handler[OrderMemberPromotion]
  implicit val OrderLadderPromotionBSONHandler = Macros.handler[OrderLadderPromotion]
  implicit val OrderFullReducePromotionBSONHandler = Macros.handler[OrderFullReducePromotion]
  implicit val OrderPromotionsBSONHandler = Macros.handler[OrderPromotions]

  implicit val OrderAmountsBSONHandler = Macros.handler[OrderAmounts]
  implicit val OrderProductsBSONHandler = Macros.handler[OrderProducts]

  implicit val OrderBSONHandler = Macros.handler[Order]
  implicit val OrderStatusLogBSONHandler = Macros.handler[OrderStatusLog]

  implicit val MemberPointsLogBSONHandler = Macros.handler[MemberPointsLog]

  implicit val MemberLevelLogBSONHandler = Macros.handler[MemberLevelLog]

  implicit val MemberCardBSONHandler = Macros.handler[MemberCard]

  implicit val MemberCardLogBSONHandler = Macros.handler[MemberCardLog]

  implicit val MemberCardStatusLogBSONHandler = Macros.handler[MemberCardStatusLog]

  implicit val OrderPayAlipayDataBSONHandler = Macros.handler[AlipayData]
  implicit val AlipayDataWithTimeBSONHandler = Macros.handler[AlipayDataWithTime]
  implicit val AlipayDataWithTimeArrayBSONHandler = bsonArrayToListHandler[AlipayDataWithTime]
  implicit val OrderPayBSONHandler = Macros.handler[OrderPay]

  implicit val MemberRecRecordBSONHandler = Macros.handler[MemberActiveRecord]

}
