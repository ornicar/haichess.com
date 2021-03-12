package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.rating.PerfType
import lila.user.User

import controllers.routes

object perfStat {

  def apply(
    u: User,
    rankMap: lila.rating.UserRankMap,
    perfType: lila.rating.PerfType,
    data: play.api.libs.json.JsObject,
    ratingChart: Option[String]
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${u.username} ${perfType.name} 统计",
    robots = false,
    moreJs = frag(
      jsAt("compiled/user.js"),
      ratingChart.map { rc =>
        frag(
          jsTag("chart/ratingHistory.js"),
          embedJsUnsafe(s"lichess.ratingHistoryChart($rc,'${perfType.name}');")
        )
      },
      jsAt(s"compiled/lichess.perfStat${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""$$(function() {
LichessPerfStat(document.querySelector('.perf-stat__content'), {
data: ${safeJsonValue(data)}
});
});""")
    ),
    moreCss = cssTag("perf-stat")
  ) {
      main(cls := s"page-menu")(
        st.aside(cls := "page-menu__menu")(show.side(u, rankMap, perfType.some)),
        div(cls := s"page-menu__content box perf-stat ${perfType.key}")(
          div(cls := "box__top")(
            h1(
              a(href := routes.User.show(u.username))(u.username),
              span(perfType.name, " 统计")
            ),
            div(cls := "box__top__actions")(
              u.perfs(perfType).nb > 0 option a(
                cls := "button button-empty text",
                dataIcon := perfType.iconChar,
                href := s"${routes.User.games(u.username, "search")}?perf=${perfType.id}"
              )("显示棋局"),
              bits.perfTrophies(u, rankMap.filterKeys(perfType==))
            )
          ),
          ratingChart.isDefined option div(cls := "rating-charts")(
            div(cls := "search-bar")(
              span(dataType := "month", dataCount := "1")("1月内"),
              span(cls := "active", dataType := "month", dataCount := "3")("3月内"),
              span(dataType := "month", dataCount := "6")("6月内"),
              span(dataType := "ytd")("今年"),
              span(dataType := "year", dataCount := "1")("1年内"),
              span(dataType := "all")("所有")
            ),
            div(cls := "rating-history")(spinner)
          ),
          div(cls := "box__pad perf-stat__content")
        )
      )
    }
}
