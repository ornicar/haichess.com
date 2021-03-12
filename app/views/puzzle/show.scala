package views.html.puzzle

import play.api.libs.json.{ JsObject, Json }
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.resource.ThemeQuery
import play.api.data.Form
import controllers.routes
import lila.puzzle.ThemeShow

object show {

  def apply(puzzle: lila.puzzle.Puzzle, data: JsObject, pref: JsObject, themeShow: Option[ThemeShow], notAccept: Boolean = false)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.training.txt(),
      moreCss = cssTag("puzzle"),
      moreJs = frag(
        jsTag("vendor/sparkline.min.js"),
        tagsinputTag,
        drawerTag,
        jsAt(s"compiled/lichess.puzzle${isProd ?? (".min")}.js"),
        embedJsUnsafe(s"""
lichess = lichess || {};
lichess.puzzle = ${
          safeJsonValue(Json.obj(
            "data" -> data.add("notAccept" -> notAccept),
            "pref" -> pref,
            "i18n" -> bits.jsI18n()
          ))
        }""")
      ),
      chessground = false,
      openGraph = lila.app.ui.OpenGraph(
        image = cdnUrl(routes.Export.puzzlePng(puzzle.id).url).some,
        title = s"Chess tactic #${puzzle.id} - ${puzzle.color.name.capitalize} to play",
        url = s"$netBaseUrl${routes.Puzzle.show(puzzle.id).url}",
        description = s"Haichess tactic trainer: " + puzzle.color.fold(
          trans.findTheBestMoveForWhite,
          trans.findTheBestMoveForBlack
        ).txt() + s" Played by ${puzzle.attempts} players."
      ).some,
      zoomable = true
    ) {
        frag(
          themeShow.isDefined option themeSearch(themeShow),
          main(cls := "puzzle")(
            st.aside(cls := "puzzle__side")(
              div(cls := "puzzle__side__metas")(spinner)
            ),
            div(cls := "puzzle__board main-board")(chessgroundBoard),
            div(cls := "puzzle__tools"),
            div(cls := "puzzle__controls"),
            div(cls := "puzzle__history")
          )
        )
      }

  def themeSearch(themeShow: Option[ThemeShow])(implicit ctx: Context): Frag =
    themeShow map { ts =>
      drawer("搜索", "主题搜索") {
        st.form(
          cls := "search_form",
          action := s"${routes.Puzzle.themePuzzle(ts.id)}#results",
          method := "GET"
        )(
            ts.rnf option div(cls := "theme-rnf")("没有搜索到，换个主题吧~"),
            table(
              tr(
                th(label("难度范围")),
                td(
                  div(cls := "half")("从 ", form3.input(ts.searchForm("ratingMin"), "number")),
                  div(cls := "half")("到 ", form3.input(ts.searchForm("ratingMax"), "number"))
                )
              ),
              tr(
                th(label("答案步数")),
                td(
                  div(cls := "half")("从 ", form3.input(ts.searchForm("stepsMin"), "number")),
                  div(cls := "half")("到 ", form3.input(ts.searchForm("stepsMax"), "number"))
                )
              ),
              tr(
                th(label("黑白")),
                td(
                  form3.tagsWithKv(ts.searchForm, "pieceColor", ThemeQuery.pieceColor)
                )
              ),
              tr(
                th(label("阶段")),
                td(
                  form3.tagsWithKv(ts.searchForm, "phase", ThemeQuery.phase)
                )
              ),
              tr(
                th(label("目的")),
                td(
                  form3.tagsWithKv(ts.searchForm, "moveFor", ThemeQuery.moveFor)
                )
              ),
              tr(
                th(label("子力")),
                td(
                  form3.tagsWithKv(ts.searchForm, "strength", ThemeQuery.strength)
                )
              ),
              tr(
                th(label("局面")),
                td(
                  form3.tagsWithKv(ts.searchForm, "chessGame", ThemeQuery.chessGame)
                )
              ),
              tr(
                th(label("技战术")),
                td(
                  form3.tagsWithKv(ts.searchForm, "subject", ThemeQuery.subject)
                )
              ),
              tr(
                th(label("综合")),
                td(
                  form3.tagsWithKv(ts.searchForm, "comprehensive", ThemeQuery.comprehensive)
                )
              ) /*,
              tr(
                th(label("标签")),
                td(
                  form3.tags(ts.searchForm, "tags", ts.markTags)
                )
              )*/
            ),
            div(cls := "drawer-footer")(
              div(cls := "drawer-footer-btn")(
                a(cls := "cancel", drawerHide := true)("取消"),
                submitButton(cls := "button button-empty")("开始练习")
              )
            )
          )
      }
    }

}
