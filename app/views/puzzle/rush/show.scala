package views.html.puzzle.rush

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import play.api.libs.json.{ JsArray, JsObject, Json }
import lila.common.String.html.safeJsonValue
import controllers.routes
import lila.puzzle.PuzzleRush

object show {

  def apply(
    user: JsObject,
    pref: JsObject,
    rush: Option[JsObject] = None,
    rounds: Option[JsArray] = None,
    threeMinutesMode: Option[PuzzleRush] = None,
    fiveMinutesMode: Option[PuzzleRush] = None,
    survivalMode: Option[PuzzleRush] = None,
    mode: Option[PuzzleRush.Mode] = None,
    auto: Boolean = false,
    notAccept: Boolean = true
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "战术冲刺",
      moreCss = cssTag("puzzleRush"),
      moreJs = frag(
        jsAt(s"compiled/lichess.puzzleRush${isProd ?? (".min")}.js"),
        embedJsUnsafe(s"""
lichess = lichess || {};
lichess.puzzleRush = ${
          safeJsonValue(Json.obj(
            "user" -> user,
            "pref" -> pref,
            "auto" -> auto,
            "notAccept" -> notAccept
          ).add("rush" -> rush)
            .add("rounds" -> rounds)
            .add("threeMinutesMode", threeMinutesMode.map(_.id))
            .add("fiveMinutesMode", fiveMinutesMode.map(_.id))
            .add("survivalMode", survivalMode.map(_.id))
            .add("mode", mode.map(_.id)))
        }""")
      ),
      chessground = false,
      zoomable = true
    ) {
        frag(
          main(cls := "puzzleRush"),
          div(cls := "min-puzzle none")
        )
      }

}
