package views.html.resource

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import controllers.routes
import lila.game.Game
import lila.user.User
import play.api.data.Form
import play.mvc.Call

object game {

  def liked(form: Form[_], user: User, pager: Paginator[Game], tags: Set[String])(implicit ctx: Context) = layout(
    title = "收藏的对局",
    active = "liked",
    form = form,
    user = user,
    pager = pager,
    call = routes.Resource.gameLiked()
  ) {
      st.form(
        cls := "search_form",
        action := s"${routes.Resource.gameLiked()}#results",
        method := "GET"
      )(
          table(
            tr(
              td(
                form3.tags(form, "tags", tags)
              ),
              td(cls := "action")(
                select(cls := "select")(
                  option(value := "")("选择"),
                  option(value := "all")("选中所有"),
                  option(value := "none")("取消选择")
                ),
                select(cls := "action")(
                  option(value := "")("操作"),
                  option(value := "unlike")("取消收藏")
                )
              )
            )
          )
        )
    }

  def imported(form: Form[_], user: User, pager: Paginator[Game], tags: Set[String])(implicit ctx: Context) = layout(
    title = "导入的对局",
    active = "imported",
    form = form,
    user = user,
    pager = pager,
    call = routes.Resource.gameImported()
  ) {
      st.form(
        cls := "search_form",
        action := s"${routes.Resource.gameImported()}#results",
        method := "GET"
      )(
          table(
            tr(
              td(
                form3.tags(form, "tags", tags)
              ),
              td(cls := "action")(
                pager.nbResults > 0 option frag(
                  select(cls := "select")(
                    option(value := "")("选择"),
                    option(value := "all")("选中所有"),
                    option(value := "none")("取消选择")
                  ),
                  select(cls := "action")(
                    option(value := "")("操作"),
                    option(value := "delete")("删除")
                  )
                )
              )
            )
          )
        )
    }

  def search(form: Form[_], user: User, pager: Paginator[Game])(implicit ctx: Context) = layout(
    title = "高级搜索",
    active = "search",
    form = form,
    user = user,
    pager = pager,
    call = routes.Resource.gameSearch()
  ) {
      views.html.search.user(user, form, routes.Resource.gameSearch())
    }

  private[resource] def layout(
    title: String,
    active: String,
    form: Form[_],
    user: User,
    pager: Paginator[Game],
    call: Call
  )(formFrag: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreJs = frag(
      active == "search" option flatpickrTag,
      active == "search" option jsTag("search.js"),
      infiniteScrollTag,
      jsTag("resource.js")
    ),
    moreCss = frag(
      active == "search" option cssTag("user.show.search"),
      cssTag("resource")
    )
  ) {
      var url = call.url.concat("?a=1")
      form.data.foreach {
        case (key, value) =>
          url = url.concat("&").concat(key).concat("=").concat(value)
      }

      main(cls := "page-menu resource")(
        st.aside(cls := "page-menu__menu")(
          div(cls := "resource-nav subnav")(
            menuLinks(active)
          )
        ),
        div(cls := "page-menu__content box")(
          formFrag,
          paginate(pager, call, active, form, user)
        )
      )
    }

  private[resource] def paginate(pager: Paginator[Game], call: Call, active: String, form: Form[_], user: User)(implicit ctx: Context) = {
    var url = call.url.concat("?a=1")
    form.data.foreach {
      case (key, value) =>
        url = url.concat("&").concat(key).concat("=").concat(value)
    }

    if (pager.currentPageResults.isEmpty) div(cls := "no-more")(
      iconTag("4"),
      p("没有更多了")
    )
    else div(cls := "games infinitescroll")(
      pagerNext(pager, np => addQueryParameter(url, "page", np)) | div(cls := "none"),
      views.html.game.widgets(pager.currentPageResults, user = user.some,
        ownerLink = true, linkBlank = true, showFullMoves = (active == "imported"), showCheckbox = (active == "imported"))
    )
  }

  private[resource] def menuLinks(active: String)(implicit ctx: Context) = {
    def activeCls(c: String) = cls := (c == active).option("active")
    val accept = ctx.me.??(_.hasResource)
    frag(
      a(activeCls("imported"), href := routes.Resource.gameImported())("导入的对局"),
      accept option frag(
        a(activeCls("liked"), href := routes.Resource.gameLiked())("收藏的对局"),
        a(activeCls("search"), href := routes.Resource.gameSearch())("高级搜索")
      ),
      !accept option frag(
        a(cls := "liked disabled")(span("收藏的对局"), br, span(cls := "onlyMember")("会员可用")),
        a(cls := "search disabled")(span("高级搜索"), br, span(cls := "onlyMember")("会员可用"))
      )
    )
  }
}
