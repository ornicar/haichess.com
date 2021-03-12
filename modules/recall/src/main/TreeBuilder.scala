package lila.recall

import chess.format.{ FEN, Forsyth, Uci, UciCharPair }
import chess.opening.FullOpeningDB
import lila.game.Game
import lila.tree

object TreeBuilder {

  def apply(game: Game, plies: Int, initialFen: Option[FEN]): tree.Root = {
    chess.Replay.gameMoveWhileValid(game.pgnMoves take plies, initialFen.map(_.value) | Forsyth.initial, game.variant) match {
      case (init, games, error) =>
        error foreach logChessError(game.id)
        val fen = Forsyth >> init
        val root = tree.Root(
          ply = init.turns,
          fen = fen,
          check = init.situation.check,
          opening = FullOpeningDB findByFen fen,
          crazyData = None
        )
        def makeBranch(index: Int, g: chess.Game, m: Uci.WithSan) = {
          val fen = Forsyth >> g
          tree.Branch(
            id = UciCharPair(m.uci),
            ply = g.turns,
            move = m,
            fen = fen,
            check = g.situation.check,
            opening = FullOpeningDB findByFen fen,
            crazyData = None
          )
        }
        games.zipWithIndex.reverse match {
          case Nil => root
          case ((g, m), i) :: rest => root prependChild rest.foldLeft(makeBranch(i + 1, g, m)) {
            case (node, ((g, m), i)) => makeBranch(i + 1, g, m) prependChild node
          }
        }
    }
  }

  private val logChessError = (id: String) => (err: String) =>
    logger.warn(s"TreeBuilder https://haichess.com/$id ${err.lines.toList.headOption}")
}
