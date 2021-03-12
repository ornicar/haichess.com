package lila.errors

import chess.Color
import lila.user.User
import lila.puzzle.Puzzle
import lila.puzzle.PuzzleId
import org.joda.time.DateTime

case class PuzzleErrors(
    _id: String,
    puzzleId: PuzzleId,
    rating: Int,
    depth: Int,
    fen: String,
    color: Color,
    lastMove: Option[String],
    createAt: DateTime,
    createBy: User.ID
) {

  def id = _id

}

object PuzzleErrors {

  def make(
    puzzle: Puzzle,
    userId: User.ID
  ) = PuzzleErrors(
    _id = makeId(userId, puzzle.id),
    puzzleId = puzzle.id,
    rating = puzzle.rating.toInt,
    depth = puzzle.depth,
    fen = puzzle.fenAfterInitialMove | puzzle.fen,
    color = puzzle.color,
    lastMove = puzzle.history.lastOption,
    createAt = DateTime.now,
    createBy = userId
  )

  def makeId(userId: User.ID, puzzleId: PuzzleId) = s"$userId@$puzzleId"

}