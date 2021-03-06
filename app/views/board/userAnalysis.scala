package views.html.board

import play.api.libs.json.{ Json, JsObject }

import chess.variant.Crazyhouse

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.rating.PerfType.iconByVariant

import controllers.routes

object userAnalysis {

  def apply(data: JsObject, pov: lila.game.Pov)(implicit ctx: Context) = views.html.base.layout(
    title = trans.analysis.txt(),
    moreCss = frag(
      cssTag("analyse.free"),
      pov.game.variant == Crazyhouse option cssTag("analyse.zh"),
      !pov.game.synthetic && pov.game.playable && ctx.me.flatMap(pov.game.player).isDefined option cssTag("analyse.forecast"),
      ctx.blind option cssTag("round.nvui")
    ),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJsUnsafe(s"""lichess=lichess||{};lichess.user_analysis=${
        safeJsonValue(Json.obj(
          "data" -> data,
          "i18n" -> userAnalysisI18n(
            withForecast = !pov.game.synthetic && pov.game.playable && ctx.me.flatMap(pov.game.player).isDefined
          ),
          "explorer" -> Json.obj(
            "endpoint" -> explorerEndpoint,
            "tablebaseEndpoint" -> tablebaseEndpoint
          )
        ))
      }""")
    ),
    chessground = false,
    openGraph = lila.app.ui.OpenGraph(
      title = "Chess analysis board",
      url = s"$netBaseUrl${routes.UserAnalysis.index.url}",
      description = "Analyse chess positions and variations on an interactive chess board"
    ).some,
    zoomable = true
  ) {
      main(cls := "analyse")(
        pov.game.synthetic option st.aside(cls := "analyse__side")(
          views.html.base.bits.mselect(
            "analyse-variant",
            span(cls := "text", dataIcon := iconByVariant(pov.game.variant))("标准国际象棋"),
            chess.variant.Variant.all.filter(_ == chess.variant.Standard).map { v =>
              a(
                dataIcon := iconByVariant(v),
                cls := (pov.game.variant == v).option("current"),
                href := routes.UserAnalysis.parse(v.key)
              )("标准国际象棋")
            }
          )
        ),
        div(cls := "analyse__board main-board")(chessgroundBoard),
        div(cls := "analyse__tools"),
        div(cls := "analyse__controls")
      )
    }
}
