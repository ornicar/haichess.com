package lila.app
package templating

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.team.Env.{ current => teamEnv }
import controllers.routes

trait TeamHelper {

  private def api = teamEnv.api

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => api.syncBelongsTo(teamId, me.id))

  def teamLinkById(id: String, withIcon: Boolean = true, cssClass: Option[String] = None): Frag =
    api.team(id).awaitSeconds(3).fold[Frag](span("俱乐部已失效")) { team =>
      teamLink(team, withIcon)
    }

  def teamLink(team: lila.team.Team, withIcon: Boolean = true, cssClass: Option[String] = None): Frag =
    div(cls := List("team-link" -> true, ~cssClass -> cssClass.isDefined))(
      withIcon option img(cls := "logo", src := logoUrl(team.logo)),
      a(cls := "name", href := routes.Team.show(team.id))(team.name),
      teamBadge(team.certified)
    )

  val baseUrl2 = s"//${lila.api.Env.current.Net.AssetDomain}"
  val defaultLogo: String = s"${baseUrl2}/assets/images/club-default.png"
  def logoUrl(logo: Option[String]) = logo.fold(defaultLogo) { l =>
    s"$baseUrl2/image/$l"
  }

  val certIcon: Frag = i(cls := "cert", title := "Haichess 认证俱乐部")

  def teamBadge(cert: Boolean = true): Frag = span(cls := "badges")(
    cert option certIcon
  )

  def teamIdToName(id: String): Frag = StringFrag(api.teamName(id).getOrElse(id))

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)
}
