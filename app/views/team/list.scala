package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.team.Team
import controllers.routes

object list {

  def search(text: String, teams: Paginator[Team])(implicit ctx: Context) = list(
    name = trans.search.txt() + " \"" + text + "\"",
    teams = teams,
    nextPageUrl = n => routes.Team.search(text, n).url,
    tab = "all",
    search = text
  )

  def all(teams: Paginator[Team])(implicit ctx: Context) = list(
    name = trans.teams.txt(),
    teams = teams,
    nextPageUrl = n => routes.Team.all(n).url,
    tab = "all"
  )

  def mine(teams: List[Team.TeamWithMember])(implicit ctx: Context) =
    bits.layout(title = trans.myTeams.txt()) {
      main(cls := "team-list page-menu")(
        bits.menu("mine".some),
        div(cls := "page-menu__content box")(
          h1(trans.myTeams()),
          table(cls := "slist slist-pad")(
            if (teams.size > 0) tbody(teams.map(bits.teamWithMemberTr(_)))
            else noTeam()
          )
        )
      )
    }

  private def noTeam()(implicit ctx: Context) = tbody(
    tr(td(colspan := "2")(
      br,
      trans.noTeamFound()
    ))
  )

  private def list(
    name: String,
    teams: Paginator[lila.team.Team],
    nextPageUrl: Int => String,
    tab: String,
    search: String = ""
  )(implicit ctx: Context) =
    bits.layout(title = "%s - page %d".format(name, teams.currentPage)) {
      main(cls := "team-list page-menu")(
        bits.menu("all".some),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(
            st.form(cls := "search", action := routes.Team.search(), method := "get")(
              input(st.name := "text", st.placeholder := "搜索 “完整名称” 或 “编号”", st.value := search),
              submitButton(cls := "button", dataIcon := "y")
            )
          ),
          table(cls := "slist slist-pad")(
            if (teams.nbResults > 0) tbody(cls := "infinitescroll")(
              pagerNext(teams, nextPageUrl),
              tr,
              teams.currentPageResults map { bits.teamTr(_) }
            )
            else noTeam()
          )
        )
      )
    }
}
