package lila.importer

import org.joda.time.DateTime
import lila.game.{ Game, GameRepo }

final class PuzzleGameImporter(gameImporter: Importer) {

  private val masterGameEncodingFixedAt = new DateTime(2016, 3, 9, 0, 0)

  def apply(id: Game.ID, pgn: String): Fu[Game] =
    GameRepo game id flatMap {
      case Some(game) if !game.isPgnImport || game.createdAt.isAfter(masterGameEncodingFixedAt) => fuccess(game)
      case _ => (GameRepo remove id) >> {
        gameImporter(
          ImportData(pgn, none),
          user = "lichess".some,
          forceId = id.some
        )
      }
    }

}
