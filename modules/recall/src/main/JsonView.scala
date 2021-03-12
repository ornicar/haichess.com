package lila.recall

import play.api.libs.json._

final class JsonView(
    gameJson: GameJson,
    animationDuration: scala.concurrent.duration.Duration
) {

  def apply(recall: Recall, history: List[Recall], pgn: Option[String] = None): Fu[JsObject] =
    gameJson(recall.gameId, pgn).map { gameJs =>
      Json.obj(
        "game" -> gameJs,
        "recall" -> Json.obj(
          "id" -> recall.id,
          "name" -> recall.name,
          "readonly" -> recall.readonly
        ).add("turns" -> recall.turns)
          .add("color" -> recall.color.map(_.name)),
        "history" -> historyJson(history)
      )
    }

  def pref(p: lila.pref.Pref) = Json.obj(
    "blindfold" -> p.blindfold,
    "coords" -> p.coords,
    "rookCastle" -> p.rookCastle,
    "animation" -> Json.obj(
      "duration" -> p.animationFactor * animationDuration.toMillis
    ),
    "destination" -> p.destination,
    "resizeHandle" -> p.resizeHandle,
    "moveEvent" -> p.moveEvent,
    "highlight" -> p.highlight,
    "is3d" -> p.is3d
  )

  def historyJson(replays: List[Recall]) =
    JsArray(
      replays.map { replay =>
        Json.obj(
          "id" -> replay.id,
          "name" -> replay.name
        )
      }
    )

}

