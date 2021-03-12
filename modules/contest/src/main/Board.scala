package lila.contest

import chess.Color
import lila.game.Game
import lila.user.User
import org.joda.time.DateTime

case class Board(
    id: Game.ID,
    no: Board.No,
    contestId: Contest.ID,
    roundId: Round.ID,
    roundNo: Round.No,
    status: chess.Status,
    whitePlayer: Board.MiniPlayer,
    blackPlayer: Board.MiniPlayer,
    startsAt: DateTime,
    appt: Boolean = false,
    apptComplete: Boolean = false,
    reminded: Boolean = false,
    turns: Option[Int] = None,
    finishAt: Option[DateTime] = None
) {

  def gameId = id
  def isCreated = status.id == chess.Status.Created.id
  def isStarted = status.id == chess.Status.Started.id
  def isFinished = status.id >= chess.Status.Aborted.id
  def shouldStart = startsAt.getMillis <= DateTime.now.getMillis
  def is(b: Board) = b.id == id
  def secondsToStart = (startsAt.getSeconds - nowSeconds).toInt atLeast 0
  def startStatus = secondsToStart |> { s => "%02d:%02d".format(s / 60, s % 60) }

  def player(color: Color): Board.MiniPlayer =
    color.fold(whitePlayer, blackPlayer)

  def opponentOf(no: Player.No): Option[Player.No] =
    if (no == whitePlayer.no) blackPlayer.no.some
    else if (no == blackPlayer.no) whitePlayer.no.some
    else none

  def colorOf(no: Player.No): Option[Color] =
    if (no == whitePlayer.no) Color.White.some
    else if (no == blackPlayer.no) Color.Black.some
    else none

  def contains(no: Player.No): Boolean =
    whitePlayer.no == no || blackPlayer.no == no

  def contains(userId: User.ID): Boolean =
    whitePlayer.userId == userId || blackPlayer.userId == userId

  def players = List(whitePlayer, blackPlayer)

  def colorOfById(id: User.ID): Color =
    if (id == whitePlayer.id) Color.White
    else if (id == blackPlayer.id) Color.Black
    else Color.White

  def exists(no: Player.No) = players.exists(_.no == no)

  def winner: Option[Board.MiniPlayer] = players find (_.wins)

  def isDraw = winner.isEmpty

  def isWin(no: Player.No) = winner.??(_.no == no)

  def resultShow = status match {
    case chess.Status.Created => "等待开赛"
    case chess.Status.Started => "比赛中"
    case _ => resultFormat
  }

  def resultFormat = {
    if (whitePlayer.wins) "1-0"
    else if (blackPlayer.wins) "0-1"
    else "1/2-1/2"
  }

}

object Board {

  type No = Int

  case class FullInfo(board: Board, round: Round, contest: Contest) {
    def fullName = s"${contest.fullName} - 第${round.no}轮"
    def boardName = s"第${round.no}轮 #${board.no}"

    def canStarted = contest.isStarted && round.isStarted && board.shouldStart
  }

  private[contest] case class MiniPlayer(id: Player.ID, userId: User.ID, no: Player.No, isWinner: Option[Boolean] = None) {
    def wins = isWinner getOrElse false
  }

  private[contest] case class BoardWithPlayer(board: Board, player: Player)

  private[contest] sealed abstract class Outcome(val id: String, val name: String) {

    def isWin = this == Outcome.Win
  }
  private[contest] object Outcome {
    case object Win extends Outcome("win", "胜")
    case object Loss extends Outcome("loss", "负")
    case object Draw extends Outcome("draw", "和")
    case object Bye extends Outcome("bey", "轮空")
    case object NoStart extends Outcome("no-start", "没有移动")
    case object Leave extends Outcome("leave", "离开") // 超过最大NoStart次数-> Leave
    case object Quit extends Outcome("quit", "退赛")
    case object Kick extends Outcome("kick", "踢出")
    case object ManualAbsent extends Outcome("manual-absent", "弃权")
    case object Half extends Outcome("half", "半分轮空") // 棋手中途加入，之前的成绩就是Half

    val all = List(Win, Loss, Draw, Bye, NoStart, Leave, Quit, Kick, ManualAbsent, Half)

    val byId = all map { v => (v.id, v) } toMap

    def apply(id: String): Outcome = byId get id err s"Bad Outcome $id"

  }

}
