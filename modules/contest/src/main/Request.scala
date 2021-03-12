package lila.contest

import org.joda.time.DateTime
import lila.user.User

case class Request(
    id: String,
    contestId: Contest.ID,
    contestName: String,
    userId: String,
    message: String,
    status: Request.RequestStatus,
    date: DateTime
) {

}

object Request {

  type ID = String

  def make(
    contestId: String,
    contestName: String,
    userId: String,
    message: String
  ): Request = new Request(
    id = makeId(contestId, userId),
    contestId = contestId,
    contestName = contestName,
    userId = userId,
    message = message.trim,
    status = RequestStatus.Invited,
    date = DateTime.now
  )

  private[contest] def makeId(contestId: String, userId: String) = userId + "@" + contestId

  sealed abstract class RequestStatus(val id: String, val name: String)
  object RequestStatus {
    case object Invited extends RequestStatus("invited", "待审核")
    case object Joined extends RequestStatus("joined", "已加入")
    case object Refused extends RequestStatus("refused", "已拒绝")

    val all = List(Invited, Joined, Refused)
    val byId = all map { v => (v.id, v) } toMap
    def apply(id: String): RequestStatus = byId get id err s"Bad Status $id"
    def applyByAccept(accept: Boolean): RequestStatus = if (accept) Joined else Refused
  }

}

case class RequestWithUser(request: Request, user: User) {
  def id = request.id
  def message = request.message
  def date = request.date
  def status = request.status
  def processed = status != Request.RequestStatus.Invited
  def profile = user.profileOrDefault
}
