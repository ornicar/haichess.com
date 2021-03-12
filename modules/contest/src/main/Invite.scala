package lila.contest

import org.joda.time.DateTime
import lila.user.User

case class Invite(
    id: String,
    contestId: Contest.ID,
    contestName: String,
    userId: String,
    status: Invite.InviteStatus,
    date: DateTime
) {

}

object Invite {

  type ID = String

  def make(
    contestId: String,
    contestName: String,
    userId: String
  ): Invite = new Invite(
    id = makeId(contestId, userId),
    contestId = contestId,
    contestName = contestName,
    userId = userId,
    status = InviteStatus.Invited,
    date = DateTime.now
  )

  private[contest] def makeId(contestId: String, userId: String) = userId + "@" + contestId

  sealed abstract class InviteStatus(val id: String, val name: String)
  object InviteStatus {
    case object Invited extends InviteStatus("invited", "待接受邀请")
    case object Joined extends InviteStatus("joined", "已加入")
    case object Refused extends InviteStatus("refused", "已拒绝")

    val all = List(Invited, Joined, Refused)
    val byId = all map { v => (v.id, v) } toMap
    def apply(id: String): InviteStatus = byId get id err s"Bad Status $id"
    def applyByAccept(accept: Boolean): InviteStatus = if (accept) Joined else Refused
  }

}

case class InviteWithUser(invite: Invite, user: User) {
  def id = invite.id
  def date = invite.date
  def status = invite.status
  def profile = user.profileOrDefault
  def processed = status != Invite.InviteStatus.Invited
}
