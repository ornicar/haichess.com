package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.team.{ MemberWithUser, Team, TeamRating }
import play.api.libs.json.JsArray
import controllers.routes

object memberRatingDistribution {

  def apply(
    team: Team,
    mwu: MemberWithUser,
    distributionData: List[Int],
    historyData: JsArray,
    pager: Paginator[TeamRating]
  )(implicit ctx: Context) = {
    views.html.base.layout(
      title = "俱乐部等级分",
      moreCss = cssTag("team"),
      moreJs = frag(
        infiniteScrollTag,
        jsTag("chart/teamRatingDistribution.js"),
        jsTag("chart/teamRatingHistory.js"),
        embedJsUnsafe(s"""lichess.teamRatingDistributionChart({
  freq: ${distributionData.mkString("[", ",", "]")},
  myRating: ${mwu.member.intRating.fold("null")(r => r.toString)}
  });"""),
        embedJsUnsafe(s"""lichess.teamRatingHistoryChart(${historyData.toString});""")
      )
    ) {
        main(cls := "ratingDistribution", dataId := team.id)(
          div(cls := "box box-pad distribution")(
            div(cls := "distribution_action")(
              h1(a(href := routes.Team.ratingDistribution(team.id))(team.name), nbsp, "俱乐部等级分"),
              h2(mwu.viewName, mwu.member.intRating.map(r => s"（$r）"))
            ),
            div(cls := "chart_area")(
              div(id := "rating_distribution_chart")(spinner)
            )
          ),
          div(cls := "box box-pad history")(
            div(cls := "chart_area")(
              div(cls := "search-bar")(
                span(dataType := "month", dataCount := "1")("1月内"),
                span(cls := "active", dataType := "month", dataCount := "3")("3月内"),
                span(dataType := "month", dataCount := "6")("6月内"),
                span(dataType := "ytd")("今年"),
                span(dataType := "year", dataCount := "1")("1年内"),
                span(dataType := "all")("所有")
              ),
              div(id := "rating_history_chart")(spinner)
            )
          ),
          div(cls := "box box-pad ratingLogs")(
            div(cls := "list_area")(
              table(cls := "slist")(
                thead(
                  tr(
                    th("时间"),
                    th("信息"),
                    th("变化"),
                    th("类型")
                  )
                ),
                if (pager.nbResults > 0) {
                  tbody(cls := "infinitescroll")(
                    pagerNextTable(pager, np => routes.Team.ratingDistribution(team.id, np).url),
                    pager.currentPageResults.map { teamRating =>
                      tr(cls := "paginated")(
                        td(teamRating.createAt.toString("yyyy-MM-dd HH:mm")),
                        td(
                          teamRating.typ match {
                            case TeamRating.Typ.Game => teamRating.metaData.gameId.fold(frag(teamRating.note)) { gameId => a(href := routes.Round.watcher(gameId, "white"))(teamRating.note) }
                            case TeamRating.Typ.Contest => teamRating.metaData.contestId.fold(frag(teamRating.note)) { contestId => a(href := s"${routes.Contest.show(contestId)}#round${teamRating.metaData.roundNo | 1}")(teamRating.note) }
                            case TeamRating.Typ.OffContest => teamRating.metaData.contestId.fold(frag(teamRating.note)) { contestId => a(href := s"${routes.OffContest.show(contestId)}#round${teamRating.metaData.roundNo | 1}")(teamRating.note) }
                            case _ => frag(teamRating.note)
                          }
                        ),
                        td(cls := "rating")(
                          teamRating.rating.toInt, "（", span(cls := List("diff" -> true, "minus" -> (teamRating.diff < 0)))(if (teamRating.diff < 0) { teamRating.diffFormat } else { "+" + teamRating.diffFormat }), "）"
                        ),
                        td(teamRating.typ.name)
                      )
                    }
                  )
                } else {
                  tbody(
                    tr(
                      td(colspan := 4)("暂无记录")
                    )
                  )
                }
              )
            )
          )
        )
      }
  }

}
