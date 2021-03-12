package views.html.user.show

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.game.{ Game, Pov }
import lila.user.User

import controllers.routes

object gamesContent {

  def apply(
    u: User,
    nbs: lila.app.mashup.UserInfo.NbGames,
    pager: Paginator[Game],
    filters: lila.app.mashup.GameFilterMenu,
    filterName: String
  )(implicit ctx: Context) = frag(

    div(cls := "number-menu number-menu--tabs menu-box-pop", id := "games")(
      filters.list.map { f =>
        a(
          cls := s"nm-item to-${f.name}${(filters.current == f) ?? " active"}",
          href := routes.User.games(u.username, f.name)
        )(userGameFilterTitle(u, nbs, f))
      }
    ),
    nbs.crosstable.ifTrue(filters.current.name == "me").map {
      views.html.game.crosstable(_, none)
    },
    div(cls := "search__result")(
      if (filterName == "search") {
        val permalink = a(rel := "nofollow", href := routes.User.games(u.username, filterName))("永久链接")
        if (pager.nbResults > 0) frag(
          div(cls := "search__status")(
            strong(pager.nbResults.localize, " 对局被找到"),
            " • ",
            permalink
          ),
          div(cls := "search__rows")(
            pagerNext(pager, np => routes.User.games(u.username, filterName, np).url) | div(cls := "none"),
            views.html.game.widgets(pager.currentPageResults, user = u.some, ownerLink = ctx is u)
          )
        )
        else div(cls := "search__status")(strong("没有对局被找到"), " • ", permalink)
      } else
        div(cls := List(
          "games infinitescroll" -> true,
          "now-playing center" -> (filterName == "playing" && pager.nbResults > 2)
        ))(
          pagerNext(pager, np => routes.User.games(u.username, filterName, np).url) | div(cls := "none"),
          if (filterName == "playing" && pager.nbResults > 2)
            pager.currentPageResults.flatMap { Pov(_, u) }.map { p =>
            a(href := gameLink(p), cls := "paginated")(
              gameFen(p, withLink = false),
              views.html.game.bits.vstext(p)(ctx.some)
            )
          }
          else views.html.game.widgets(pager.currentPageResults, user = u.some, ownerLink = ctx is u)
        )
    )
  )
}
