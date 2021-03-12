package lila.member

import MemberCard._
import lila.user.User
import org.joda.time.DateTime

case class MemberCard(
    _id: String,
    batch: Int,
    level: lila.user.MemberLevel,
    userId: User.ID,
    days: Days,
    validDays: Days,
    status: CardStatus,
    expireAt: DateTime,
    createAt: DateTime,
    createBy: User.ID
) {

  def id = _id

  def expired = expireAt.isBeforeNow

  def isAvailable = isCreate && !expired

  def isCreate = status == MemberCard.CardStatus.Create

  def isUsed = status == MemberCard.CardStatus.Used

  def desc = s"${level.name}（${days.name}）"

}

object MemberCard {

  val defaultValidDays = Days.Month3

  def makeByOrder(o: Order, batch: Int) = {
    val now = DateTime.now
    val level = Product.toMemberLevel(o.products.productId)
    val prefix = level.cap + now.toString("yyyyMMdd") + "%06d".format(batch)
    (1 to o.products.count) map { index =>
      MemberCard(
        _id = prefix + "%03d".format(index),
        batch = batch,
        level = level,
        userId = o.createBy,
        days = o.products.attrs.get("days").map(Days(_)) err s"make card error by order ${o.id}",
        validDays = defaultValidDays,
        status = CardStatus.Create,
        expireAt = now.plusDays(defaultValidDays.toDays(now)),
        createAt = now,
        createBy = o.createBy
      )
    }
  }

  def makeBySignup(userId: User.ID, batch: Int) = {
    val now = DateTime.now
    val level = lila.user.MemberLevel.Gold
    val prefix = level.cap + now.toString("yyyyMMdd") + "%06d".format(batch)
    MemberCard(
      _id = prefix + "%03d".format(1),
      batch = batch,
      level = level,
      userId = userId,
      days = Days.Month1,
      validDays = Days.Forever,
      status = CardStatus.Create,
      expireAt = now.plusDays(Days.Forever.toDays(now)),
      createAt = now,
      createBy = "admin"
    )
  }

  sealed abstract class CardStatus(val id: String, val name: String)
  object CardStatus {
    case object Create extends CardStatus("create", "待使用")
    case object Used extends CardStatus("used", "已使用")
    case object Expired extends CardStatus("expired", "已过期")
    val all = List(Create, Used, Expired)
    def apply(id: String): CardStatus = all.find(_.id == id) err s"can not find CardStatus $id"
    def choices = all.map(d => d.id -> d.name)
  }

}
