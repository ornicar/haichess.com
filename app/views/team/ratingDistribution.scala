package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.team.{ Member, MemberWithUser, Team, TeamSetting }
import lila.hub.lightClazz.{ ClazzId, ClazzName }
import play.api.data.Form
import controllers.routes

object ratingDistribution {

  def apply(
    form: Form[_],
    member: Option[Member],
    team: Team,
    clazzs: List[(ClazzId, ClazzName)],
    pager: Paginator[MemberWithUser],
    distributionData: List[Int]
  )(implicit ctx: Context) = {
    views.html.base.layout(
      title = "俱乐部等级分",
      moreCss = cssTag("team"),
      moreJs = frag(
        jsTag("team.ratingDistribution.js"),
        jsTag("chart/teamRatingDistribution.js"),
        embedJsUnsafe(s"""lichess.teamRatingDistributionChart({
  freq: ${distributionData.mkString("[", ",", "]")},
  myRating: ${member.fold("null") { m => m.intRating.fold("null")(r => r.toString) }}
  });""")
      )
    ) {
        main(cls := "ratingDistribution", dataId := team.id)(
          div(cls := "box box-pad distribution")(
            div(cls := "list_action")(
              h1(team.name, "内部等级分分布"),
              form3.select(form("dstClazz"), clazzs, "全部".some)
            ),
            div(cls := "chart_area")(
              div(id := "rating_distribution_chart")(spinner)
            )
          ),
          div(cls := "box box-pad members")(
            div(cls := "list_area")(
              div(cls := "list_action")(
                st.form(rel := "nofollow", action := s"${routes.Team.ratingDistribution(team.id)}#results", method := "GET")(
                  form3.hidden("id", team.id),
                  table(
                    tr(
                      td(form3.input(form("username"))(placeholder := "账号/备注")),
                      td(form3.select(form("clazzId"), clazzs, "全部".some)),
                      td(submitButton(cls := "button")("查询"))
                    )
                  )
                )
              ),
              table(cls := "slist")(
                thead(
                  tr(
                    th("账号"),
                    th("姓名（备注）"),
                    th("等级分"),
                    th("操作")
                  )
                ),
                if (pager.nbResults > 0) {
                  tbody(cls := "infinitescroll")(
                    pagerNextTable(pager, np => nextPageUrl(form, team, np)),
                    pager.currentPageResults.map { mu =>
                      tr(cls := "paginated")(
                        td(userLink(mu.user)),
                        td(mu.viewName),
                        td(mu.member.rating.map(_.intValue.toString) | "-"),
                        td(
                          a(cls := "button button-empty small member-rating", href := routes.Team.memberRatingModal(mu.member.id))("编辑"),
                          a(cls := "button button-empty small")("详情")
                        )
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

  def nextPageUrl(form: Form[_], team: Team, np: Int)(implicit ctx: Context) = {
    var url: String = routes.Team.members(team.id, np).url
    form.data.foreach {
      case (key, value) => url = url.concat("&").concat(key).concat("=").concat(value)
    }
    url
  }

  def ratingEdit(mu: MemberWithUser, form: Form[_])(implicit ctx: Context) = frag(
    div(cls := "modal-content none")(
      h2(mu.user.username, "（", mu.viewName, "）"),
      postForm(cls := "form3 member-rating-modal", action := routes.Team.memberRatingApply(mu.member.id))(
        form3.group(form("rating"), "等级分", help = frag("水平越高对应的等级分越高，取值范围：500~3200").some)(form3.input(_, typ = "number")),
        form3.group(form("note"), "原因说明")(form3.textarea(_)()),
        form3.globalError(form),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交")
        )
      )
    )
  )

}
