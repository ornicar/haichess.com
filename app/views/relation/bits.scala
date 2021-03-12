package views.html.relation

import play.api.mvc.Call
import lila.game.Game
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.relation.Related
import lila.user.User

import controllers.routes

object bits {

  def followers(u: User, pag: Paginator[Related], nbFollowing: Int, game: Option[Game.ID])(implicit ctx: Context) =
    layout(s"${u.username} • ${trans.nbFollowers.pluralSameTxt(pag.nbResults)}")(
      div(cls := "box__top")(
        h1(userLink(u, withOnline = false)),
        div(cls := "actions")(
          trans.nbFollowers.pluralSame(pag.nbResults),
          " ", amp, " ",
          a(href := routes.Relation.following(u.username, 1, game))(trans.nbFollowing.pluralSame(nbFollowing))
        )
      ),
      pagTable(pag, routes.Relation.followers(u.username), game)
    )

  def following(u: User, pag: Paginator[Related], nbFollowers: Int, game: Option[Game.ID])(implicit ctx: Context) =
    layout(s"${u.username} • ${trans.nbFollowing.pluralSameTxt(pag.nbResults)}")(
      div(cls := "box__top")(
        h1(userLink(u, withOnline = false)),
        div(cls := "actions")(
          trans.nbFollowing.pluralSame(pag.nbResults),
          " ", amp, " ",
          a(href := routes.Relation.followers(u.username, 1, game))(trans.nbFollowers.pluralSame(nbFollowers))
        )
      ),
      pagTable(pag, routes.Relation.following(u.username), game)
    )

  def blocks(u: User, pag: Paginator[Related])(implicit ctx: Context) =
    layout(s"${u.username} • ${trans.blocks.pluralSameTxt(pag.nbResults)}")(
      div(cls := "box__top")(
        h1(userLink(u, withOnline = false)),
        div(cls := "actions")(
          trans.blocks.pluralSame(pag.nbResults)
        )
      ),
      pagTable(pag, routes.Relation.blocks(), none)
    )

  def layout(title: String)(content: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("relation"),
      moreJs = infiniteScrollTag
    ) {
        main(cls := "box page-small")(content)
      }

  private def pagTable(pager: Paginator[Related], call: Call, game: Option[Game.ID])(implicit ctx: Context) =
    table(cls := "slist")(
      if (pager.nbResults > 0)
        tbody(cls := "infinitescroll")(
        pagerNextTable(pager, np => addQueryParameter(call.url, "page", np)),
        pager.currentPageResults.map { r =>
          tr(cls := "paginated")(
            td(userLink(r.user)),
            td(showBestPerf(r.user)),
            td(trans.nbGames.pluralSame(r.user.count.game)),
            td(actions(r.user.id, relation = r.relation, followable = r.followable, blocked = false, game = game))
          )
        }
      )
      else tbody(tr(td(colspan := 2)("None found.", br)))
    )

  def forbidden(u: User)(implicit ctx: Context) =
    views.html.site.message(
      title = s"${u.username}的好友列表是受保护的",
      back = true
    )(
      p(style := "display:flex;")("对不起， 您不能查看 ", userLink(u, withOnline = false), "的好友列表"),
      br,
      p(
        "或许可以让他们修改",
        a(href := routes.Pref.form("privacy"))("隐私设置"),
        " ？"
      )
    )
}
