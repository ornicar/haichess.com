package lila.member

import lila.user.User
import lila.user.MemberLevel
import org.joda.time.DateTime

object OrderDirector {

  val zero = BigDecimal(0.00)
  val yearDays = 365
  val monthDays = 30

  def calcPrice(data: CalcPriceData, user: User) = {
    val product = lockProduct(data)
    val item = lockItem(data)
    val price = item.price

    val totalPrice = price * data.count

    // 1.白银会员抵扣金额
    val silverDays = calcSilverDays(item, user)
    var silverDaysAmount = zero
    var priceAfterSilver = totalPrice
    if (item.isSilverMember && Product.toMemberLevel(data.productId) == lila.user.MemberLevel.Gold) {
      silverDaysAmount = (silverDays / yearDays) * MemberLevel.Silver.prices.year + (silverDays % yearDays / monthDays) * MemberLevel.Silver.prices.month + (silverDays % yearDays % monthDays) * MemberLevel.Silver.prices.day
      priceAfterSilver = (totalPrice - silverDaysAmount).max(zero)
    }

    // 2.优惠金额
    val promotions = OrderPromotions.of(item.promotions, priceAfterSilver, data.count, user)
    val priceAfterPromotions = promotions.outPrice(priceAfterSilver)

    // 3.邀请码金额(一年:50/30, 6个月:25/15, 3个月:12.5/7.5)
    val inviteUserAmount = priceAfterPromotions.min {
      val level = item.attrs.get("level") err "cant get level"
      val max = if (level == MemberLevel.Gold.code) {
        BigDecimal(50.00)
      } else if (level == MemberLevel.Silver.code) {
        BigDecimal(30.00)
      } else zero

      if (data.inviteUser.isDefined) {
        val attr = item.attrs.get("days") | ""
        if (attr.contains("year")) max
        else if (attr.contains("month")) {
          val month = attr.replace("month", "").toInt
          max / 12 * month
        } else zero
      } else zero
    }
    var priceAfterInviteUserAmount = priceAfterPromotions
    if (item.isInviteUser) {
      priceAfterInviteUserAmount = priceAfterPromotions - inviteUserAmount
    }

    // 4.积分金额
    val maxPoints = priceAfterInviteUserAmount.min(BigDecimal(user.memberOrDefault.points)).intValue()
    var points = 0
    var priceAfterPoints = priceAfterInviteUserAmount
    if (item.isPoint) {
      val fillPoints = data.points | 0
      if (data.isPointsChange) {
        points = math.min(maxPoints, fillPoints)
      } else {
        points = maxPoints
      }
      priceAfterPoints = (priceAfterInviteUserAmount - points).max(zero)
    }

    OrderAmountInfo(
      amounts = OrderAmounts(
        silverDays = if (silverDays == 0) None else silverDays.some,
        silverDaysAmount = if (silverDays == 0) None else silverDaysAmount.some,
        promotions = promotions,
        points = if (points == 0) None else points.some,
        pointsAmount = if (points == 0) None else BigDecimal(points).some,
        coupon = None,
        couponAmount = None,
        inviteUser = data.inviteUser,
        inviteUserAmount = data.inviteUser.map(_ => inviteUserAmount)
      ),
      count = data.count,
      price = price,
      totalPrice = totalPrice,
      payPrice = priceAfterPoints,
      priceAfterSilver = priceAfterSilver,
      priceAfterPromotions = priceAfterPromotions,
      priceAfterPoints = priceAfterPoints,
      maxPoints = maxPoints,
      productId = product.id,
      productName = product.name,
      productItemCode = item.code,
      productItemName = item.name,
      attrs = item.attrs
    )
  }

  private def calcSilverDays(item: Item, user: User) = {
    val member = user.memberOrDefault
    val goldLevel = member.goldLevel
    val silverLevel = member.silverLevel

    val buyGoldDays = toDays(user, item.attrs)
    val remainderGoldDays = goldLevel.map(_.remainderChargeDays) | 0
    val remainderSilverDays = silverLevel.map(_.remainderChargeDays) | 0
    val availableSilverDays = math.max(remainderSilverDays - remainderGoldDays, 0)
    if (buyGoldDays > availableSilverDays) {
      availableSilverDays
    } else buyGoldDays
  }

  def toDays(user: User, attrs: Map[String, String]) = {
    val attrOption = attrs.get("days")
    val level = attrs.get("level") err "cant get level"
    attrOption.map { attr =>
      val oldExpireAt = user.memberOrDefault.levels.get(level).map(_.expireAt) | DateTime.now
      Days(attr).toDays(oldExpireAt)
    } getOrElse 0
  }

  def lockProduct(data: CalcPriceData) =
    ProductType(data.productTyp)
      .all
      .find(_.id == data.productId)
      .err(s"can not find product ${data.productId}")

  def lockItem(data: CalcPriceData) =
    lockProduct(data)
      .item(data.itemCode)
      .err(s"can not find item ${data.itemCode}")

}

case class OrderAmountInfo(
    amounts: OrderAmounts,
    count: Int,
    price: BigDecimal,
    totalPrice: BigDecimal,
    payPrice: BigDecimal,
    priceAfterSilver: BigDecimal,
    priceAfterPromotions: BigDecimal,
    priceAfterPoints: BigDecimal,
    maxPoints: Int, // 可以使用的最大积分数量
    productId: String, // 商品ID
    productName: String, // 商品名称
    productItemCode: String, // 规格编码
    productItemName: String, // 规格名称
    attrs: Map[String, String]
)