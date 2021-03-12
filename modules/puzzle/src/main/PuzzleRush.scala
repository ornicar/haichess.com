package lila.puzzle

import org.joda.time.Period
import lila.user.User
import org.joda.time.DateTime
import ornicar.scalalib.Random
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

case class PuzzleRush(
    id: PuzzleRush.ID,
    userId: User.ID,
    createTime: DateTime,
    season: Int,
    startTime: Option[DateTime] = None,
    endTime: Option[DateTime] = None,
    mode: PuzzleRush.Mode,
    status: PuzzleRush.Status,
    condition: Option[CustomCondition],
    result: Option[PuzzleRush.Result] = None
) {

  def isFinished = status.isFinished()

  def isCustom = mode == PuzzleRush.Mode.Custom
  def customFiniteDuration = FiniteDuration(condition.??(_.minutes), TimeUnit.MINUTES)
  def clockFiniteDuration = if (isCustom) customFiniteDuration else mode.clock

  def elapsed = status match {
    case PuzzleRush.Status.Created => 0L
    case PuzzleRush.Status.Started => DateTime.now.getMillis - startTime.get.getMillis
    case PuzzleRush.Status.Timeout => clockFiniteDuration.toMillis
    case _ => endTime.get.getMillis - startTime.get.getMillis
  }

  def remaining = clockFiniteDuration.toMillis - elapsed

  def remainingSeconds = (remaining / 1000).toInt

  def outOfTime = status.is(PuzzleRush.Status.Started) && elapsed >= clockFiniteDuration.toMillis

  def win = result.??(_.win)

}

object PuzzleRush {

  type ID = String

  def make(user: User, mode: Mode, condition: Option[CustomCondition] = None): PuzzleRush = PuzzleRush(
    id = makeId,
    userId = user.id,
    createTime = DateTime.now(),
    season = makeSeason,
    mode = mode,
    condition = condition,
    status = Status.Created
  )

  def makeId = Random nextString 8
  def makeSeason = DateTime.now().toString("yyyyMM").toInt

  sealed abstract class Mode(val id: String, val name: String, val clock: FiniteDuration, val lossNumber: Int)
  object Mode {

    case object ThreeMinutes extends Mode(id = "threeMinutes", name = "三分钟", clock = 180 seconds, lossNumber = 3)
    case object FiveMinutes extends Mode(id = "fiveMinutes", name = "五分钟", clock = 300 seconds, lossNumber = 3)
    case object Survival extends Mode(id = "survival", name = "生存", clock = Int.MaxValue seconds, lossNumber = 3)
    case object Custom extends Mode(id = "custom", name = "自定义", clock = Int.MaxValue seconds, lossNumber = 3)

    val default = ThreeMinutes

    val all = List(ThreeMinutes, FiveMinutes, Survival, Custom)

    val keys = all map { v => v.id } toSet

    val byId = all map { v => (v.id, v) } toMap

    def apply(id: String): Mode = byId.get(id) err s"Bad Mode $this"

  }

  sealed abstract class Status(val id: Int, val name: String) extends Ordered[Status] {

    def compare(other: Status) = id compare other.id

    def is(s: Status): Boolean = this == s

    def is(f: Status.type => Status): Boolean = is(f(Status))

    def isFinished() = id >= 30

  }

  object Status {

    case object Created extends Status(10, "创建")
    case object Started extends Status(20, "开始")
    case object Timeout extends Status(30, "时间结束")
    case object Strikeout extends Status(40, "三振出局")
    case object Aborted extends Status(50, "终止")

    val all = List(Created, Started, Timeout, Strikeout, Aborted)

    val keys = all map { v => v.id } toSet

    val byId = all map { v => (v.id, v) } toMap

    def apply(id: Int): Status = byId.get(id) err s"Bad Status $this"
  }

  case class Result(
      seconds: Int,
      avgTime: Int,
      nb: Int,
      win: Int,
      loss: Int,
      maxRating: Int,
      winStreaks: Int
  ) {

    val secondsPeriod = new Period(seconds * 1000L)
    val avgTimePeriod = new Period(avgTime * 1000L)
  }

  object Result {

    def apply(puzzleRush: PuzzleRush, rounds: List[PuzzleRound]): Result = {

      val seconds = (puzzleRush.elapsed / 1000).toInt
      val nb = rounds.size
      val win = rounds.foldLeft(0) { (i, r) =>
        r.result.win match {
          case true => i + 1
          case false => i
        }
      }
      val loss = rounds.foldLeft(0) { (i, r) =>
        r.result.win match {
          case true => i
          case false => i + 1
        }
      }
      val maxRating = rounds.foldLeft(0) { (i, r) =>
        (r.puzzleRating > i) match {
          case true => if (r.result.win) r.puzzleRating atLeast i else i
          case false => i
        }
      }
      val winStreaks = rounds.foldLeft((0, 0)) { (x, r) =>
        r.result.win match {
          case true => {
            val x2 = x._2 + 1
            val x1 = x._1 atLeast x2
            (x1, x2)
          }
          case false => (x._1, 0)
        }
      }._1
      val avgTime = if (nb == 0) seconds else Math.ceil(seconds / nb).toInt
      new Result(seconds, avgTime, nb, win, loss, maxRating, winStreaks)
    }

  }

  sealed abstract class Order(val key: String, val name: String)
  object Order {
    case object Updated extends Order("endTime", "时间倒序")
    case object Score extends Order("result.win", "分数倒序")

    val default = Updated
    val all = List(Updated, Score)
    val byKey: Map[String, Order] = all.map { o => o.key -> o }(scala.collection.breakOut)
    def apply(key: String): Order = byKey.getOrElse(key, default)
  }

  object BSONFields {
    val id = "_id"
    val userId = "userId"
    val createTime = "createTime"
    val season = "season"
    val startTime = "startTime"
    val endTime = "endTime"
    val mode = "mode"
    val status = "status"
    val condition = "condition"
    val result = "result"
    val win = s"$result.win"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler

  private[puzzle] implicit val ModeBSONHandler = new BSONHandler[BSONString, Mode] {
    def read(b: BSONString) = Mode.byId get b.value err s"Invalid PuzzleRush Mode ${b.value}"
    def write(m: Mode) = BSONString(m.id)
  }

  private[puzzle] implicit val StatusBSONHandler = new BSONHandler[BSONInteger, Status] {
    def read(b: BSONInteger) = Status.byId get b.value err s"Invalid PuzzleRush Status ${b.value}"
    def write(m: Status) = BSONInteger(m.id)
  }

  private[puzzle] implicit val ResultBSONHandler = Macros.handler[Result]
  private[puzzle] implicit val ConditionBSONHandler = Macros.handler[CustomCondition]
  implicit val PuzzleRushBSONHandler = new BSON[PuzzleRush] {
    import BSONFields._

    def reads(r: BSON.Reader): PuzzleRush = {
      PuzzleRush(
        id = r.str(id),
        userId = r.str(userId),
        createTime = r.get[DateTime](createTime),
        season = r.int(season),
        startTime = r.getO[DateTime](startTime),
        endTime = r.getO[DateTime](endTime),
        mode = r.get[Mode](mode),
        status = r.get[Status](status),
        condition = r.getO[CustomCondition](condition),
        result = r.getO[Result](result)
      )
    }

    def writes(w: BSON.Writer, o: PuzzleRush) = BSONDocument(
      id -> o.id,
      userId -> o.userId,
      createTime -> o.createTime,
      season -> o.season,
      startTime -> o.startTime,
      endTime -> o.endTime,
      mode -> o.mode,
      result -> o.result,
      status -> o.status,
      condition -> o.condition,
      result -> o.result
    )
  }

}
