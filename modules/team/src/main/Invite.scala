package lila.team

import org.joda.time.DateTime

import lila.user.User

case class Invite(
    _id: String,
    team: String,
    user: String,
    message: String,
    date: DateTime
) {

  def id = _id
}

object Invite {

  def makeId(team: String, user: String) = user + "@" + team

  def make(team: String, user: String, message: String): Request = new Request(
    _id = makeId(team, user),
    user = user,
    team = team,
    message = message.trim,
    date = DateTime.now
  )

  sealed abstract class InviteMessage(val canInvite: Boolean, val message: String)
  object InviteMessage {
    case object MustCoach extends InviteMessage(false, "邀请人必须是教练")
    case object CoachNotFound extends InviteMessage(false, "教练不存在")
    case object Disabled extends InviteMessage(false, "俱乐部已关闭")
    case object Joined extends InviteMessage(false, "教练已经加入")
    case object Inviting extends InviteMessage(false, "请勿重复邀请")
    case object Requested extends InviteMessage(false, "教练已经申请")
  }

}

case class InviteWithUser(invite: Invite, user: User) {
  def id = invite.id
  def message = invite.message
  def date = invite.date
  def team = invite.team
}
