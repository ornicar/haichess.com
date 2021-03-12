package lila.member

import lila.user.User
import lila.user.MemberLevel
import org.joda.time.{ DateTime, Period, PeriodType }

case class Product(
    _id: String, // ID
    name: String, // 商品名称
    typ: ProductType, // 商品类型
    unit: String, // 单位
    desc: Option[String], // 商品描述
    publishStatus: PublishStatus, // 上架状态
    items: Items // 规格
) {

  def id = _id

  def itemList = items.map.toList.map(_._2).sortBy(_.sort)

  def item(itemCode: String) = items.get(itemCode)

}

object Product {

  def toMemberLevel(product: Product): MemberLevel = toMemberLevel(product.id)

  def toMemberLevel(productId: String): MemberLevel = {
    if (productId.toLowerCase.contains(MemberLevel.Gold.code)) MemberLevel.Gold
    else if (productId.toLowerCase.contains(MemberLevel.Silver.code)) MemberLevel.Silver
    else MemberLevel.General
  }

}

// code -> Item
case class Items(map: Map[String, Item]) {

  def get(itemCode: String) = map.get(itemCode)

}

case class Item(
    code: String, // 编码
    name: String, // 名称
    price: BigDecimal, // 价格
    isPoint: Boolean, // 是否可以使用积分
    isCoupon: Boolean, // 是否可以使用优惠券
    isInviteUser: Boolean, // 是否可以使用邀请码
    isSilverMember: Boolean, // 是否可以使用银牌会员抵扣
    promotions: Promotions, // 折扣规则
    sort: Int,
    attrs: Map[String, String] = Map.empty
)

sealed abstract class ProductType(val id: String, val name: String, val prefix: String) {
  def all: List[Product]
}
object ProductType {
  case object VirtualMember extends ProductType("virtualMember", "虚拟会员", "UB") {

    def all = List(VmcGold, VmcSilver)

    def defaultDays = Days.Year1

    def defaultProduct = VmcGold

    def productOfLevel(level: String) = all.find(_.id.toLowerCase.contains(level.toLowerCase)).map(_.id)

    def defaultItem = defaultProduct.item(defaultItemCodeOfProduct(defaultProduct))

    def defaultItemOfProduct(product: Product) = product.item(defaultItemCodeOfProduct(product))

    def defaultItemCodeOfProduct(product: Product) = product.id + "@" + defaultDays.id

  }
  case object VirtualMemberCard extends ProductType("virtualMemberCard", "虚拟会员卡", "OB") {

    def all = List(VmCardGold, VmCardSilver)

    def defaultDays = Days.Year1

    def defaultProduct = VmCardGold

    def defaultItem = defaultProduct.item(defaultItemCodeOfProduct(defaultProduct))

    def defaultItemOfProduct(product: Product) = product.item(defaultItemCodeOfProduct(product))

    def defaultItemCodeOfProduct(product: Product) = product.id + "@" + defaultDays.id

  }

  val all = List(VirtualMember, VirtualMemberCard)
  def apply(id: String): ProductType = all.find(_.id == id) err s"can not find ProductType $id"
  def choices = all.map(d => d.id -> d.name)
}

sealed abstract class PublishStatus(val id: String, val name: String)
object PublishStatus {
  case object ON extends PublishStatus("on", "已上架")
  case object OFF extends PublishStatus("off", "已下架")
  val all = List(ON, OFF)
  def apply(id: Int): PublishStatus = all.find(_.id == id) err s"can not find PublishStatus $id"
}

abstract class Promotion {

  def fit(sourcePrice: BigDecimal, count: Int, user: User): Boolean

  def calc(sourcePrice: BigDecimal, count: Int, user: User): BigDecimal
}

case class Promotions(
    pricePromotions: List[PricePromotion] = Nil,
    memberPromotions: List[MemberPromotion] = Nil,
    ladderPromotions: List[LadderPromotion] = Nil,
    fullReducePromotions: List[FullReducePromotion] = Nil
) {

  // 限定条件：达成条件必须从难道易（按顺序放入List中）
  // 每种折扣只能选择一种，多种折扣可以叠加
  /*  def every(sourcePrice: BigDecimal, count: Int, user: User): List[Promotion] =
    (
      pricePromotions.find(_.fit(sourcePrice, count, user)),
      memberPromotions.find(_.fit(sourcePrice, count, user)),
      ladderPromotions.find(_.fit(sourcePrice, count, user)),
      fullReducePromotions.find(_.fit(sourcePrice, count, user))
    ).filter(_.nonEmpty).map(_.get)*/

  /*  def calc(sourcePrice: BigDecimal, count: Int, user: User): BigDecimal = {
    every(sourcePrice, count, user).foldLeft(sourcePrice) {
      case (price, promotion) => promotion.calc(price, count, user)
    }
  }*/

}

// 特惠促销
case class PricePromotion(
    sTime: DateTime, // 开始时间
    eTime: DateTime, // 结束时间
    price: BigDecimal // 促销价格
) extends Promotion {

  def fit(sourcePrice: BigDecimal, count: Int, user: User) = {
    val now = DateTime.now
    now.isBefore(eTime) && now.isAfter(eTime)
  }

  def calc(sourcePrice: BigDecimal, count: Int, user: User) = {
    if (fit(sourcePrice, count, user)) price
    else sourcePrice
  }.max(BigDecimal(0.00))
}

// 会员促销
case class MemberPromotion(
    level: String, // 会员等级（code）
    price: BigDecimal // 促销价格
) extends Promotion {

  def fit(sourcePrice: BigDecimal, count: Int, user: User) =
    user.memberLevel.code == level

  def calc(sourcePrice: BigDecimal, count: Int, user: User) = {
    if (fit(sourcePrice, count, user)) price
    else sourcePrice
  }.max(BigDecimal(0.00))
}

// 阶梯价格
case class LadderPromotion(
    fullCount: Int, // 满足的商品数量
    discount: BigDecimal // 折扣（0.8）
) extends Promotion {

  def name = s"${discount * 10}折"

  def fit(sourcePrice: BigDecimal, count: Int, user: User) =
    count >= fullCount

  def calc(sourcePrice: BigDecimal, count: Int, user: User) = {
    if (fit(sourcePrice, count, user)) sourcePrice * discount
    else sourcePrice
  }.max(BigDecimal(0.00))
}

// 满减价格
case class FullReducePromotion(
    fullPrice: BigDecimal, // 商品满足金额
    reducePrice: BigDecimal // 商品减少金额
) extends Promotion {

  def fit(sourcePrice: BigDecimal, count: Int, user: User) =
    sourcePrice >= fullPrice

  def calc(sourcePrice: BigDecimal, count: Int, user: User) = {
    if (fit(sourcePrice, count, user)) sourcePrice - reducePrice
    else sourcePrice
  }.max(BigDecimal(0.00))
}

object VmcGold extends Product(
  _id = "vmcGold",
  name = "金牌会员",
  typ = ProductType.VirtualMember,
  unit = "个",
  desc = none,
  publishStatus = PublishStatus.ON,
  items = Items(
    Map(
      "vmcGold@" + Days.Month3.id -> Item(
        code = "vmcGold@" + Days.Month3.id,
        name = Days.Month3.name,
        price = MemberLevel.Gold.prices.month * 3,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(),
        sort = 1,
        attrs = Map(
          "days" -> Days.Month3.id,
          "level" -> MemberLevel.Gold.code
        )
      ),
      "vmcGold@" + Days.Month6.id -> Item(
        code = "vmcGold@" + Days.Month6.id,
        name = Days.Month6.name,
        price = MemberLevel.Gold.prices.month * 6,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(),
        sort = 2,
        attrs = Map(
          "days" -> Days.Month6.id,
          "level" -> MemberLevel.Gold.code
        )
      ),
      "vmcGold@" + Days.Year1.id -> Item(
        code = "vmcGold@" + Days.Year1.id,
        name = Days.Year1.name,
        price = MemberLevel.Gold.prices.year,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(),
        sort = 3,
        attrs = Map(
          "days" -> Days.Year1.id,
          "level" -> MemberLevel.Gold.code
        )
      ),
      "vmcGold@" + Days.Year2.id -> Item(
        code = "vmcGold@" + Days.Year2.id,
        name = Days.Year2.name,
        price = MemberLevel.Gold.prices.year * 2,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(
          ladderPromotions = List(
            LadderPromotion(
              fullCount = 1,
              discount = 0.8
            )
          )
        ),
        sort = 4,
        attrs = Map(
          "days" -> Days.Year2.id,
          "level" -> MemberLevel.Gold.code
        )
      ),
      "vmcGold@" + Days.Year3.id -> Item(
        code = "vmcGold@" + Days.Year3.id,
        name = Days.Year3.name,
        price = MemberLevel.Gold.prices.year * 3,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(
          ladderPromotions = List(
            LadderPromotion(
              fullCount = 1,
              discount = 0.7
            )
          )
        ),
        sort = 5,
        attrs = Map(
          "days" -> Days.Year3.id,
          "level" -> MemberLevel.Gold.code
        )
      ),
      "vmcGold@" + Days.Year4.id -> Item(
        code = "vmcGold@" + Days.Year4.id,
        name = Days.Year4.name,
        price = MemberLevel.Gold.prices.year * 4,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(
          ladderPromotions = List(
            LadderPromotion(
              fullCount = 1,
              discount = 0.7
            )
          )
        ),
        sort = 6,
        attrs = Map(
          "days" -> Days.Year4.id,
          "level" -> MemberLevel.Gold.code
        )
      ),
      "vmcGold@" + Days.Forever.id -> Item(
        code = "vmcGold@" + Days.Forever.id,
        name = Days.Forever.name,
        price = MemberLevel.Gold.prices.year * 5,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(
          ladderPromotions = List(
            LadderPromotion(
              fullCount = 1,
              discount = 0.7
            )
          )
        ),
        sort = 7,
        attrs = Map(
          "days" -> Days.Forever.id,
          "level" -> MemberLevel.Gold.code
        )
      )
    )
  )
)

object VmcSilver extends Product(
  _id = "vmcSilver",
  name = "银牌会员",
  typ = ProductType.VirtualMember,
  unit = "个",
  desc = none,
  publishStatus = PublishStatus.ON,
  items = Items(
    Map(
      "vmcSilver@" + Days.Month3.id -> Item(
        code = "vmcSilver@" + Days.Month3.id,
        name = Days.Month3.name,
        price = MemberLevel.Silver.prices.month * 3,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(),
        sort = 1,
        attrs = Map(
          "days" -> Days.Month3.id,
          "level" -> MemberLevel.Silver.code
        )
      ),
      "vmcSilver@" + Days.Month6.id -> Item(
        code = "vmcSilver@" + Days.Month6.id,
        name = Days.Month6.name,
        price = MemberLevel.Silver.prices.month * 6,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(),
        sort = 2,
        attrs = Map(
          "days" -> Days.Month6.id,
          "level" -> MemberLevel.Silver.code
        )
      ),
      "vmcSilver@" + Days.Year1.id -> Item(
        code = "vmcSilver@" + Days.Year1.id,
        name = Days.Year1.name,
        price = MemberLevel.Silver.prices.year,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(),
        sort = 3,
        attrs = Map(
          "days" -> Days.Year1.id,
          "level" -> MemberLevel.Silver.code
        )
      ),
      "vmcSilver@" + Days.Year2.id -> Item(
        code = "vmcSilver@" + Days.Year2.id,
        name = Days.Year2.name,
        price = MemberLevel.Silver.prices.year * 2,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(
          ladderPromotions = List(
            LadderPromotion(
              fullCount = 1,
              discount = 0.8
            )
          )
        ),
        sort = 4,
        attrs = Map(
          "days" -> Days.Year2.id,
          "level" -> MemberLevel.Silver.code
        )
      ),
      "vmcSilver@" + Days.Year3.id -> Item(
        code = "vmcSilver@" + Days.Year3.id,
        name = Days.Year3.name,
        price = MemberLevel.Silver.prices.year * 3,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(
          ladderPromotions = List(
            LadderPromotion(
              fullCount = 1,
              discount = 0.7
            )
          )
        ),
        sort = 5,
        attrs = Map(
          "days" -> Days.Year3.id,
          "level" -> MemberLevel.Silver.code
        )
      ),
      "vmcSilver@" + Days.Year4.id -> Item(
        code = "vmcSilver@" + Days.Year4.id,
        name = Days.Year4.name,
        price = MemberLevel.Silver.prices.year * 4,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(
          ladderPromotions = List(
            LadderPromotion(
              fullCount = 1,
              discount = 0.7
            )
          )
        ),
        sort = 6,
        attrs = Map(
          "days" -> Days.Year4.id,
          "level" -> MemberLevel.Silver.code
        )
      ),
      "vmcSilver@" + Days.Forever.id -> Item(
        code = "vmcSilver@" + Days.Forever.id,
        name = Days.Forever.name,
        price = MemberLevel.Silver.prices.year * 5,
        isPoint = true,
        isCoupon = true,
        isInviteUser = true,
        isSilverMember = true,
        promotions = Promotions(
          ladderPromotions = List(
            LadderPromotion(
              fullCount = 1,
              discount = 0.7
            )
          )
        ),
        sort = 7,
        attrs = Map(
          "days" -> Days.Forever.id,
          "level" -> MemberLevel.Silver.code
        )
      )
    )
  )
)

object VmCardGold extends Product(
  _id = "vmCardGold",
  name = "金牌会员",
  typ = ProductType.VirtualMemberCard,
  unit = "个",
  desc = none,
  publishStatus = PublishStatus.ON,
  items = Items(
    Map(
      "vmCardGold@" + Days.Year1.id -> Item(
        code = "vmCardGold@" + Days.Year1.id,
        name = Days.Year1.name,
        price = BigDecimal(149.00),
        isPoint = true,
        isCoupon = true,
        isInviteUser = false,
        isSilverMember = false,
        promotions = Promotions(),
        sort = 3,
        attrs = Map(
          "days" -> Days.Year1.id,
          "level" -> MemberLevel.Gold.code
        )
      )
    )
  )
)

object VmCardSilver extends Product(
  _id = "vmCardSilver",
  name = "银牌会员",
  typ = ProductType.VirtualMemberCard,
  unit = "个",
  desc = none,
  publishStatus = PublishStatus.ON,
  items = Items(
    Map(
      "vmCardSilver@" + Days.Year1.id -> Item(
        code = "vmCardSilver@" + Days.Year1.id,
        name = Days.Year1.name,
        price = BigDecimal(99.00),
        isPoint = true,
        isCoupon = true,
        isInviteUser = false,
        isSilverMember = false,
        promotions = Promotions(),
        sort = 3,
        attrs = Map(
          "days" -> Days.Year1.id,
          "level" -> MemberLevel.Silver.code
        )
      )
    )
  )
)

sealed abstract class Days(val id: String, val name: String, val period: String, val n: Int) {

  def toDays(date: DateTime) = {
    if (period == "year") {
      val days = new Period(date.getMillis, date.plusYears(n).getMillis, PeriodType.days).getDays
      days
    } else if (period == "month") {
      val days = new Period(date.getMillis, date.plusMonths(n).getMillis, PeriodType.days).getDays
      days
    } else if (period == "day") {
      n
    } else 0
  }

}

object Days {
  case object Month1 extends Days("1month", "1个月", "month", 1)
  case object Month3 extends Days("3month", "3个月", "month", 3)
  case object Month6 extends Days("6month", "6个月", "month", 6)
  case object Year1 extends Days("1year", "1年", "year", 1)
  case object Year2 extends Days("2year", "2年", "year", 2)
  case object Year3 extends Days("3year", "3年", "year", 3)
  case object Year4 extends Days("4year", "4年", "year", 4)
  case object Forever extends Days(lila.user.Member.MaxYear + "year", "5年（永久）", "year", lila.user.Member.MaxYear)

  val all = List(Month1, Month3, Month6, Year1, Year2, Year3, Year4, Forever)
  def apply(id: String): Days = all.find(_.id == id) err s"can not find Days $id"

}
