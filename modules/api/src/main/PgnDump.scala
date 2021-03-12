package lila.api

import play.api.libs.iteratee._
import chess.format.FEN
import chess.format.pgn.Pgn
import lila.analyse.{ Analysis, Annotator }
import lila.game.PgnDump.WithFlags
import lila.game.Game

final class PgnDump(
    val dumper: lila.game.PgnDump,
    annotator: Annotator,
    getSimulName: String => Fu[Option[String]],
    getTournamentName: String => Option[String],
    getContestName: String => Option[String]
) {

  def apply(game: Game, initialFen: Option[FEN], analysis: Option[Analysis], flags: WithFlags): Fu[Pgn] =
    dumper(game, initialFen, flags) flatMap { pgn =>
      if (flags.tags) {

        game.simulId.fold(fuccess(pgn)) { id =>
          getSimulName(id) map {
            case None => pgn
            case Some(name) => pgn.withEvent(name)
          }
        }

        fuccess(
          game.tournamentId.fold(pgn) { id =>
            getTournamentName(id).fold(pgn) { name =>
              pgn.withEvent(name)
            }
          }
        )

        fuccess(
          game.contestId.fold(pgn) { id =>
            getContestName(id).fold(pgn) { name =>
              pgn.withEvent(name)
            }
          }
        )

      } else fuccess(pgn)
    } map { pgn =>
      val evaled = analysis.ifTrue(flags.evals).fold(pgn)(addEvals(pgn, _))
      if (flags.literate) annotator(evaled, analysis, game.opening, game.winnerColor, game.status)
      else evaled
    }

  private def addEvals(p: Pgn, analysis: Analysis): Pgn = analysis.infos.foldLeft(p) {
    case (pgn, info) => pgn.updateTurn(info.turn, turn =>
      turn.update(info.color, move => {
        val comment = info.cp.map(_.pawns.toString)
          .orElse(info.mate.map(m => s"#${m.value}"))
        move.copy(
          comments = comment.map(c => s"[%eval $c]").toList ::: move.comments
        )
      }))
  }

  def formatter(flags: WithFlags) =
    Enumeratee.mapM[(Game, Option[FEN], Option[Analysis])].apply[String] {
      case (game, initialFen, analysis) => toPgnString(game, initialFen, analysis, flags)
    }

  def toPgnString(game: Game, initialFen: Option[FEN], analysis: Option[Analysis], flags: WithFlags) =
    apply(game, initialFen, analysis, flags).map { pgn =>
      // merge analysis & eval comments
      // 1. e4 { [%eval 0.17] } { [%clk 0:00:30] }
      // 1. e4 { [%eval 0.17] [%clk 0:00:30] }
      s"$pgn\n\n\n".replaceIf("] } { [", "] [")
    }

  // def exportGamesFromIds(ids: List[String]): Enumerator[String] =
  //   Enumerator.enumerate(ids grouped 50) &>
  //     Enumeratee.mapM[List[String]].apply[List[Game]](GameRepo.gamesFromSecondary) &>
  //     Enumeratee.mapConcat(identity) &>
  //     toPgn(WithFlags())
}
