package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object admin {

  def changeOwner(t: lila.team.Team, userIds: Iterable[lila.user.User.ID])(implicit ctx: Context) = {

    val title = s"转移${t.name}俱乐部管理员"

    bits.layout(title = title) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          p("您想让谁成为这个俱乐部的管理员？"),
          br, br,
          postForm(cls := "kick", action := routes.Team.changeOwner(t.id))(
            userIds.toList.sorted.map { userId =>
              button(name := "userId", cls := "button button-empty button-no-upper confirm", value := userId)(
                usernameOrId(userId)
              )
            }
          )
        )
      )
    }
  }

  def kick(t: lila.team.Team, userIds: Iterable[lila.user.User.ID])(implicit ctx: Context) = {

    val title = s"踢出俱乐部${t.name}"

    bits.layout(title = title) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          p("您想将谁提出俱乐部？"),
          br, br,
          postForm(cls := "kick", action := routes.Team.kick(t.id))(
            userIds.toList.sorted.map { userId =>
              button(name := "userId", cls := "button button-empty button-no-upper confirm", value := userId)(
                input(tpe := "hidden", name := "url", value := routes.Team.kickForm(t.id)),
                usernameOrId(userId)
              )
            }
          )
        )
      )
    }
  }
}
