package lila.recall

import chess.format.FEN
import play.api.libs.json._
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.importer.ImportData
import lila.tree.Node.partitionTreeJsonWriter
import scalaz.{ Failure, Success }

private final class GameJson(
    asyncCache: lila.memo.AsyncCache.Builder,
    lightUserApi: lila.user.LightUserApi,
    importer: lila.importer.Importer
) {

  val plies = 500

  def apply(gameId: Game.ID, pgn: Option[String] = None): Fu[JsObject] =
    if (gameId == Game.syntheticId) {
      generate(Game.makeSyntheticGame, plies)
    } else if (gameId == "temporary") {
      ImportData(pgn | "", none).preprocess(user = none) match {
        case Success(p) => {
          val game = p.game.withId("temporary")
          generate(game, plies, p.initialFen)
        }
        case Failure(_) => fufail("valid pgn failed")
      }
    } else {
      GameRepo.game(gameId).flatten(s"Missing recall game $gameId!") flatMap {
        generate(_, plies)
      }
    }

  private def generate(game: Game, plies: Int, initialFen: Option[FEN] = None): Fu[JsObject] =
    (game.variant == chess.variant.FromPosition).?? {
      if (initialFen.isDefined) {
        fuccess(initialFen)
      } else {
        GameRepo.initialFen(game)
      }
    } flatMap { initialFen =>
      lightUserApi preloadMany game.userIds map { _ =>
        val perfType = lila.rating.PerfType orDefault PerfPicker.key(game)
        val tree = TreeBuilder(game, plies, initialFen)
        Json.obj(
          "id" -> game.id,
          "turns" -> game.turns,
          "perf" -> Json.obj(
            "icon" -> perfType.iconChar.toString,
            "name" -> perfType.name
          ),
          "rated" -> game.rated,
          "players" -> JsArray(game.players.map { p =>
            Json.obj(
              "userId" -> p.userId,
              "name" -> lila.game.Namer.playerText(p, withRating = true)(lightUserApi.sync),
              "color" -> p.color.name
            )
          }),
          "treeParts" -> partitionTreeJsonWriter.writes(tree)
        ).add("clock", game.clock.map(_.config.show))
      }
    }

}
