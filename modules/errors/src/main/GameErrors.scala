package lila.errors

import chess.Color
import lila.game.Game
import lila.user.User
import chess.opening.{ Ecopening, FullOpening }
import lila.errors.GameErrors._
import org.joda.time.DateTime
import chess.format.pgn.Glyph

case class GameErrors(
    _id: String, //gameId@ply
    gameId: Game.ID,
    ply: Int,
    fen: String,
    color: Color,
    lastMove: Option[String],
    judgement: Judgement,
    phase: Option[Phase],
    eco: Option[Ecopening],
    gameAt: DateTime,
    opponent: User.ID,
    createAt: DateTime,
    createBy: User.ID
) {

  def id = _id

  def idWithPly = s"$gameId#$ply"

}

object GameErrors {

  def make(
    gameId: Game.ID,
    ply: Int,
    fen: String,
    color: Color,
    lastMove: Option[String],
    judgement: GameErrors.Judgement,
    phase: Option[GameErrors.Phase],
    eco: Option[Ecopening],
    gameAt: DateTime,
    opponent: User.ID,
    userId: User.ID
  ) = GameErrors(
    _id = s"$gameId@$ply",
    gameId = gameId,
    ply = ply,
    fen = fen,
    color = color,
    lastMove = lastMove,
    judgement = judgement,
    phase = phase,
    eco = eco,
    gameAt = gameAt,
    opponent = opponent,
    createAt = DateTime.now,
    createBy = userId
  )

  sealed abstract class Phase(val id: Int, val name: String)
  object Phase {
    case object Opening extends Phase(1, "开局")
    case object Middle extends Phase(2, "中局")
    case object End extends Phase(3, "残局")
    val all = List(Opening, Middle, End)
    def byId = all map { p => (p.id, p) } toMap
    def apply(p: Int) = byId.get(p) err s"can find Phase of $p"
  }

  sealed abstract class Judgement(val glyph: Glyph, val id: String, val name: String)
  object Judgement {
    object Mistake extends Judgement(Glyph.MoveAssessment.mistake, "mistake", "错误")
    object Blunder extends Judgement(Glyph.MoveAssessment.blunder, "blunder", "严重错误")
    val all = List(Mistake, Blunder)
    def byId = all map { j => (j.id, j) } toMap
    def apply(j: String) = byId.get(j) err s"can find Judgement of ${j.toString}"
  }

}