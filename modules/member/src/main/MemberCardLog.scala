package lila.member

import lila.member.MemberCard.CardStatus
import lila.user.User
import org.joda.time.DateTime

case class MemberCardLog(
    _id: String,
    cardId: String,
    level: lila.user.MemberLevel,
    days: Days,
    validDays: Days,
    status: CardStatus,
    expireAt: DateTime,
    typ: MemberCardLog.Type,
    oldUserId: User.ID,
    newUserId: User.ID,
    note: Option[String],
    createAt: DateTime
) {

  def id = _id

}

object MemberCardLog {

  sealed abstract class Type(val id: String, val name: String)
  object Type {
    case object CoachOrTeam extends Type("coachOrTeam", "教练/俱乐部转赠")
    case object BackendGiven extends Type("backendGiven", "后台赠送")
    case object BackendRecycle extends Type("backendRecycle", "后台收回")
    case object SignupGiven extends Type("signupGiven", "注册赠送")
    val all = List(CoachOrTeam, BackendGiven, BackendRecycle, SignupGiven)
    def apply(id: String): Type = all.find(_.id == id) err s"can not find MemberCardLog Type $id"
  }
}

