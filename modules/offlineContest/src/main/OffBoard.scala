package lila.offlineContest

import chess.Color
import lila.user.User

case class OffBoard(
    id: OffBoard.ID,
    no: OffBoard.No,
    contestId: OffContest.ID,
    roundId: OffRound.ID,
    roundNo: OffRound.No,
    status: OffBoard.Status,
    whitePlayer: OffBoard.MiniPlayer,
    blackPlayer: OffBoard.MiniPlayer
) {

  def isCreated = status.id == OffBoard.Status.Created.id
  def isStarted = status.id == OffBoard.Status.Started.id
  def isFinished = status.id == OffBoard.Status.Finished.id

  def is(b: OffBoard) = b.id == id

  def player(color: Color): OffBoard.MiniPlayer =
    color.fold(whitePlayer, blackPlayer)

  def opponentOf(no: OffPlayer.No): Option[OffPlayer.No] =
    if (no == whitePlayer.no) blackPlayer.no.some
    else if (no == blackPlayer.no) whitePlayer.no.some
    else none

  def colorOf(no: OffPlayer.No): Option[Color] =
    if (no == whitePlayer.no) Color.White.some
    else if (no == blackPlayer.no) Color.Black.some
    else none

  def contains(no: OffPlayer.No): Boolean =
    whitePlayer.no == no || blackPlayer.no == no

  def contains(userId: User.ID): Boolean =
    whitePlayer.userId == userId || blackPlayer.userId == userId

  def players = List(whitePlayer, blackPlayer)

  def colorOfById(id: User.ID): Color =
    if (id == whitePlayer.id) Color.White
    else if (id == blackPlayer.id) Color.Black
    else Color.White

  def exists(no: OffPlayer.No) = players.exists(_.no == no)

  def winner: Option[OffBoard.MiniPlayer] = players find (_.wins)

  def isDraw = winner.isEmpty

  def isWin(no: OffPlayer.No) = winner.??(_.no == no)

  def resultShow = status match {
    case OffBoard.Status.Created | OffBoard.Status.Started => status.name
    case _ => resultFormat
  }

  def resultFormat = {
    if (whitePlayer.wins) "1-0"
    else if (blackPlayer.wins) "0-1"
    else "1/2-1/2"
  }

}

object OffBoard {

  type ID = String
  type No = Int

  private[offlineContest] sealed abstract class Status(val id: Int, val name: String) extends Ordered[Status] {
    def compare(other: Status) = Integer.compare(id, other.id)
    def is(s: Status): Boolean = this == s
    def is(f: Status.type => Status): Boolean = is(f(Status))
  }

  private[offlineContest] object Status {
    case object Created extends Status(10, "????????????")
    case object Started extends Status(20, "?????????")
    case object Finished extends Status(30, "????????????")
    val all = List(Created, Started, Finished)

    val byId = all map { v => (v.id, v) } toMap
    def apply(id: Int): Status = byId get id err s"Bad Status $id"
  }

  case class FullInfo(board: OffBoard, round: OffRound, offlineContest: OffContest) {
    def fullName = s"${offlineContest.fullName} - ???${round.no}???"
    def boardName = s"???${round.no}??? #${board.no}"
  }

  private[offlineContest] case class MiniPlayer(id: OffPlayer.ID, userId: User.ID, no: OffPlayer.No, isWinner: Option[Boolean] = None) {
    def wins = isWinner getOrElse false
  }

  private[offlineContest] case class BoardWithPlayer(board: OffBoard, player: OffPlayer)

  private[offlineContest] sealed abstract class Outcome(val id: String, val name: String) {

    def isWin = this == Outcome.Win
  }
  private[offlineContest] object Outcome {
    case object Win extends Outcome("win", "???")
    case object Loss extends Outcome("loss", "???")
    case object Draw extends Outcome("draw", "???")
    case object Bye extends Outcome("bey", "??????")
    case object NoStart extends Outcome("no-start", "????????????")
    case object Kick extends Outcome("kick", "??????")
    case object ManualAbsent extends Outcome("manual-absent", "??????")
    case object Half extends Outcome("half", "????????????") // ??????????????????????????????????????????Half

    val all = List(Win, Loss, Draw, Bye, NoStart, Kick, ManualAbsent, Half)

    val byId = all map { v => (v.id, v) } toMap

    def apply(id: String): Outcome = byId get id err s"Bad Outcome $id"

  }

}
