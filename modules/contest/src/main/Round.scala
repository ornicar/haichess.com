package lila.contest

import org.joda.time.DateTime

case class Round(
    id: Round.ID,
    no: Round.No,
    contestId: Contest.ID,
    status: Round.Status,
    startsAt: DateTime,
    actualStartsAt: DateTime,
    finishAt: Option[DateTime],
    boards: Option[Int]
) {

  def shouldStart = actualStartsAt.getMillis <= DateTime.now.getMillis
  def isCreated = status == Round.Status.Created
  def isPairing = status == Round.Status.Pairing
  def isPublished = status == Round.Status.Published
  def isStarted = status == Round.Status.Started
  def isFinished = status == Round.Status.Finished
  def isPublishResult = status == Round.Status.PublishResult
  def isOverPairing = status >= Round.Status.Pairing
  def isOverPublished = status >= Round.Status.Published
  def secondsToStart = (actualStartsAt.getSeconds - nowSeconds).toInt atLeast 0
  def startStatus = secondsToStart |> { s => "%02d:%02d".format(s / 60, s % 60) }

  override def toString: String = s"$contestId $no"
}

object Round {

  type ID = String
  type No = Int

  val beforeStartMinutes = 3

  case class FullInfo(round: Round, contest: Contest) {
    def fullName = s"${contest.fullName} - 第${round.no}轮"
  }

  private[contest] sealed abstract class Status(val id: Int, val desc: String) extends Ordered[Status] {
    def compare(other: Status) = Integer.compare(id, other.id)
    def is(s: Status): Boolean = this == s
    def is(f: Status.type => Status): Boolean = is(f(Status))
  }

  private[contest] object Status {
    case object Created extends Status(10, "准备中")
    case object Pairing extends Status(20, "匹配完成")
    case object Published extends Status(40, "发布对战表")
    case object Started extends Status(50, "比赛中")
    case object Finished extends Status(60, "比赛结束")
    case object PublishResult extends Status(70, "发布成绩")
    val all = List(Created, Pairing, Published, Started, Finished, PublishResult)

    val byId = all map { v => (v.id, v) } toMap
    def apply(id: Int): Status = byId get id err s"Bad Status $id"
  }

  def make(
    no: Round.No,
    contestId: Contest.ID,
    startsAt: DateTime
  ) = Round(
    id = makeId(contestId, no),
    no = no,
    contestId = contestId,
    status = Round.Status.Created,
    startsAt = startsAt,
    actualStartsAt = startsAt,
    finishAt = None,
    boards = None
  )

  def makeId(contestId: Contest.ID, no: Round.No) = contestId + "@" + no

}
