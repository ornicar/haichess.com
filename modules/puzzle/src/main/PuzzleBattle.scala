package lila.puzzle

import org.joda.time.DateTime
import scala.concurrent.duration._

case class PuzzleBattle(
    id: PuzzleBattle.ID,
    players: PuzzleBattlePlayers,
    createTime: DateTime,
    season: Int,
    startTime: Option[DateTime],
    endTime: Option[DateTime],
    mode: PuzzleBattle.Mode,
    status: PuzzleBattle.Status
) {

}

object PuzzleBattle {

  type ID = String

  sealed abstract class Mode(val id: String, val name: String, clock: FiniteDuration, lossNumber: Int) {
    //def isFinished()
  }

  object Mode {
    case object ThreeMinutes extends Mode(id = "threeMinutes", name = "三分钟", clock = 180 seconds, lossNumber = 3)
    case object Challenge extends Mode(id = "challenge", name = "挑战", clock = 180 seconds, lossNumber = 3)
  }

  sealed abstract class Status(val id: Int, val name: String) extends Ordered[Status] {

    def compare(other: Status) = id compare other.id

    def is(s: Status): Boolean = this == s

    def is(f: Status.type => Status): Boolean = is(f(Status))
  }

  object Status {

    case object Created extends Status(10, "创建")
    case object Started extends Status(20, "开始")
    case object Finished extends Status(30, "结束")

    val all = List(Created, Started, Finished)

    val byId = all map { v => (v.id, v) } toMap

    def apply(id: Int): Option[Status] = byId get id
  }

}
