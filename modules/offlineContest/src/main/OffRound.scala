package lila.offlineContest

case class OffRound(
    id: OffRound.ID,
    no: OffRound.No,
    contestId: OffRound.ID,
    status: OffRound.Status,
    boards: Option[Int]
) {

  def isCreated = status == OffRound.Status.Created
  def isPairing = status == OffRound.Status.Pairing
  def isPublished = status == OffRound.Status.Published
  def isPublishResult = status == OffRound.Status.PublishResult
  def isOverPairing = status >= OffRound.Status.Pairing

  override def toString: String = s"$contestId $no"
}

object OffRound {

  type ID = String
  type No = Int

  private[offlineContest] sealed abstract class Status(val id: Int, val name: String) extends Ordered[Status] {
    def compare(other: Status) = Integer.compare(id, other.id)
    def is(s: Status): Boolean = this == s
    def is(f: Status.type => Status): Boolean = is(f(Status))
  }

  private[offlineContest] object Status {
    case object Created extends Status(10, "准备中")
    case object Pairing extends Status(20, "匹配完成")
    case object Published extends Status(40, "发布对战表")
    case object PublishResult extends Status(70, "发布成绩")
    val all = List(Created, Pairing, Published, PublishResult)

    val byId = all map { v => (v.id, v) } toMap
    def apply(id: Int): Status = byId get id err s"Bad Status $id"
  }

  def make(
    no: OffRound.No,
    contestId: OffRound.ID
  ) = OffRound(
    id = makeId(contestId, no),
    no = no,
    contestId = contestId,
    status = OffRound.Status.Created,
    boards = None
  )

  def makeId(contestId: OffContest.ID, no: OffRound.No) = contestId + "@" + no

}
