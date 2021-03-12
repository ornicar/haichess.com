package views.html.team

import lila.api.Context
import lila.app.templating.Environment.{ _ }
import lila.app.ui.ScalatagsTemplate._
import lila.team.Team
import controllers.routes

object bits {

  def menu(currentTab: Option[String])(implicit ctx: Context) = ~currentTab |> { tab =>
    st.nav(cls := "page-menu__menu subnav")(
      (ctx.teamNbRequests > 0) option
        a(cls := tab.active("requests"), href := routes.Team.requests())(
          ctx.teamNbRequests, " 个加入请求"
        ),
      ctx.me.??(_.canTeam) option
        a(cls := tab.active("mine"), href := routes.Team.mine())(
          trans.myTeams()
        ),
      a(cls := tab.active("all"), href := routes.Team.all())(
        trans.allTeams()
      ),
      ctx.me.?? { u => u.canTeam && !u.hasTeam } option
        a(cls := tab.active("form"), href := routes.Team.form())(
          trans.newTeam()
        ),
      ctx.me.??(_.hasTeam) option
        a(cls := "disabled")(
          trans.newTeam()
        )
    )
  }

  private[team] def teamTr(t: Team)(implicit ctx: Context) =
    tr(cls := "paginated")(
      td(
        div(cls := List("subject" -> true))(
          teamLink(t, cssClass = "medium".some),
          t.disabled option span(cls := "closed")("已关闭")
        ),
        shorten(t.description, 200)
      ),
      td(cls := "info")(
        p(trans.nbMembers.plural(t.nbMembers, t.nbMembers.localize))
      )
    )

  private[team] def teamWithMemberTr(twm: Team.TeamWithMember)(implicit ctx: Context) = {
    val t = twm.team
    tr(cls := "paginated")(
      td(
        div(cls := List("subject" -> true))(
          teamLink(t, cssClass = "medium".some),
          t.disabled option span(cls := "closed")("已关闭")
        ),
        shorten(t.description, 200)
      ),
      td(cls := "info")(
        p(trans.nbMembers.plural(t.nbMembers, t.nbMembers.localize)),
        p(
          b(twm.member.role.name)
        )
      )
    )
  }

  private[team] def layout(
    title: String,
    evenMoreCss: Frag = emptyFrag,
    evenMoreJs: Frag = emptyFrag,
    openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = frag(
        cssTag("team"),
        evenMoreCss
      ),
      moreJs = frag(
        infiniteScrollTag,
        evenMoreJs
      ),
      openGraph = openGraph
    )(body)
}
