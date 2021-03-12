package lila.puzzle

import lila.user.User

case class PuzzleBattlePlayer(
    playerId: User.ID,
    status: PuzzleBattlePlayer.Status,
    isWinner: Option[Boolean] = None,
    seconds: Option[Long] = None,
    avgTime: Option[Long] = None,
    nb: Option[Int] = None,
    win: Option[Int] = None,
    loss: Option[Int] = None,
    maxRating: Option[Int] = None,
    winStreaks: Option[Int] = None,
    score: Option[Int] = None,
    scoreDiff: Option[Int] = None,
    index: Option[Int] = None,
    indexDiff: Option[Int] = None
) {

}

object PuzzleBattlePlayer {

  type PlayerMap = Map[User.ID, PuzzleBattlePlayer]

  sealed abstract class Status(val id: Int, val name: String) extends Ordered[Status] {

    def compare(other: Status) = id compare other.id

    def is(s: Status): Boolean = this == s

    def is(f: Status.type => Status): Boolean = is(f(Status))
  }

  object Status {

    case object Created extends Status(10, "创建")
    case object Started extends Status(20, "开始")
    case object Timeout extends Status(30, "时间结束")
    case object Strikeout extends Status(40, "三振出局")
    case object WinAborted extends Status(50, "获胜终止")

    val all = List(Created, Started, Timeout, Strikeout, WinAborted)

    val byId = all map { v => (v.id, v) } toMap

    def apply(id: Int): Option[Status] = byId get id
  }

}

case class PuzzleBattlePlayers(players: PuzzleBattlePlayer.PlayerMap) {

  def +(player: PuzzleBattlePlayer) = copy(players = players + (player.playerId -> player))

  def get = players.get _
}

