package lila.member

import lila.user.{ Member, MemberLevel, User }
import play.api.libs.json.{ JsArray, JsObject, Json }
import scala.math.BigDecimal.RoundingMode

object JsonView {

  def priceJson(p: OrderAmountInfo) =
    Json.obj(
      "count" -> p.count,
      "price" -> priceOf(p.price),
      "totalPrice" -> priceOf(p.totalPrice),
      "payPrice" -> priceOf(p.payPrice),
      "priceAfterSilver" -> priceOf(p.priceAfterSilver),
      "afterPromotionPrice" -> priceOf(p.priceAfterPromotions),
      "silverDays" -> (p.amounts.silverDays | 0),
      "silverDaysAmount" -> (p.amounts.silverDaysAmount.map(priceOf) | "0"),
      "points" -> (p.amounts.points | 0),
      "maxPoints" -> p.maxPoints,
      "pointsAmount" -> (p.amounts.pointsAmount.map(priceOf) | "0"),
      "coupon" -> p.amounts.coupon,
      "couponAmount" -> (p.amounts.couponAmount.map(priceOf) | "0"),
      "inviteUser" -> (p.amounts.inviteUser | ""),
      "inviteUserAmount" -> (p.amounts.inviteUserAmount.map(priceOf) | "0")
    )

  def memberJson(u: User) = {
    val member = u.memberOrDefault
    Json.obj(
      "levels" -> levelsJson(member),
      "code" -> member.code,
      "points" -> member.points,
      "coin" -> member.coin
    )
  }

  def levelsJson(member: Member) = {
    var levelsJson = Json.obj()
    member.levels.map /*.filter(_._2.level.id > 10)*/ .foreach {
      case (k, levelWithExpire) => {
        val level = levelWithExpire.level
        val dayPrice = MemberLevel.dayPriceOf(k)
        levelsJson = levelsJson ++ Json.obj(k -> Json.obj(
          "code" -> k,
          "name" -> level.name,
          "expired" -> levelWithExpire.expired,
          "expireAt" -> levelWithExpire.expireAt.toString("yyyy-MM-dd HH:mm:ss"),
          "dayPrice" -> priceOf(dayPrice),
          "remainderDays" -> levelWithExpire.remainderDays
        ))
      }
    }
    levelsJson
  }

  def productsJson(list: List[Product]) = {
    var productsJson = Json.obj()
    list.foreach { product =>
      productsJson = productsJson ++ Json.obj(
        product.id -> productJson(product)
      )
    }
    productsJson
  }

  def productJson(p: Product): JsObject = Json.obj(
    "id" -> p.id,
    "name" -> p.name,
    "unit" -> p.unit,
    "desc" -> p.desc,
    "items" -> itemsJson(p.itemList)
  )

  def itemsJson(items: List[Item]) = {
    var itemsJson = Json.obj()
    items.foreach { item =>
      itemsJson = itemsJson ++ itemJson(item)
    }
    itemsJson
  }

  def itemJson(item: Item) = {
    Json.obj(
      item.code -> Json.obj(
        "code" -> item.code,
        "name" -> item.name,
        "price" -> priceOf(item.price),
        "isPoint" -> item.isPoint,
        "isCoupon" -> item.isCoupon,
        "isInviteUser" -> item.isInviteUser,
        "isSilverMember" -> item.isSilverMember,
        "promotions" -> Json.obj(
          "pricePromotions" -> JsArray(
            item.promotions.pricePromotions.map { promotion =>
              Json.obj(
                "sTime" -> promotion.sTime.toString("yyyy-MM-dd HH:mm:ss"),
                "eTime" -> promotion.eTime.toString("yyyy-MM-dd HH:mm:ss"),
                "price" -> priceOf(promotion.price)
              )
            }
          ),
          "memberPromotions" -> JsArray(
            item.promotions.memberPromotions.map { promotion =>
              Json.obj(
                "level" -> promotion.level,
                "price" -> priceOf(promotion.price)
              )
            }
          ),
          "ladderPromotions" -> JsArray(
            item.promotions.ladderPromotions.map { promotion =>
              Json.obj(
                "fullCount" -> promotion.fullCount,
                "discount" -> promotion.discount.toString
              )
            }
          ),
          "fullReducePromotions" -> JsArray(
            item.promotions.fullReducePromotions.map { promotion =>
              Json.obj(
                "fullPrice" -> promotion.fullPrice.toString,
                "reducePrice" -> promotion.reducePrice.toString
              )
            }
          )
        ),
        "attrs" -> itemAttrs(item.attrs)
      )
    )
  }

  def itemAttrs(attrs: Map[String, Any]) = {
    var attrsJson = Json.obj()
    attrs.foreach {
      case (k, v) => attrsJson = attrsJson ++ Json.obj(
        k -> v.toString
      )
    }
    attrsJson
  }

  private def priceOf(d: BigDecimal) = d.setScale(2, RoundingMode.DOWN).toString()

}
