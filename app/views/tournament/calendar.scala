package views.html.tournament

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object calendar {

  def apply(json: play.api.libs.json.JsObject)(implicit ctx: Context) = views.html.base.layout(
    title = "锦标赛赛程表",
    moreJs = frag(
      jsAt(s"compiled/lichess.tournamentCalendar${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""LichessTournamentCalendar.app(document.getElementById('tournament-calendar'), ${
        safeJsonValue(Json.obj(
          "data" -> json,
          "i18n" -> bits.jsI18n()
        ))
      })""")
    ),
    moreCss = cssTag("tournament.calendar")
  ) {
      main(cls := "box")(
        h1("锦标赛赛程表"),
        div(id := "tournament-calendar")
      )
    }
}
