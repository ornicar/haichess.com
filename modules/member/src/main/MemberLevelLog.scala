package lila.member

import lila.user.User
import org.joda.time.DateTime

case class MemberLevelLog(
    _id: String,
    typ: MemberLevelLog.MemberLevelChangeType,
    userId: User.ID,
    level: lila.user.MemberLevel,
    oldExpireAt: Option[DateTime],
    newExpireAt: Option[DateTime],
    createAt: DateTime,
    desc: String,
    note: Option[String],
    orderId: Option[String],
    cardId: Option[String]
) {

  def id = _id

}

object MemberLevelLog {

  sealed abstract class MemberLevelChangeType(val id: String, val name: String)
  object MemberLevelChangeType {
    case object Signup extends MemberLevelChangeType("signup", "注册赠送")
    case object Buy extends MemberLevelChangeType("buy", "购买会员")
    case object Card extends MemberLevelChangeType("card", "使用会员卡")
    case object BackendGiven extends MemberLevelChangeType("backendGiven", "后台设置")
    val all = List(Signup, Buy, Card, BackendGiven)
    def apply(id: String): MemberLevelChangeType = all.find(_.id == id) err s"can not find MemberLevelChangeType $id"
  }
}

