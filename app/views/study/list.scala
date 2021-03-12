package views.html
package study

import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.study.Order
import lila.study.Study.WithChaptersAndLiked
import lila.user.User

import controllers.routes

object list {

  def all(pag: Paginator[WithChaptersAndLiked], order: Order)(implicit ctx: Context) = layout(
    title = s"所有研习",
    active = "all",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.all(o)
  )("所有研习")

  def byOwner(pag: Paginator[WithChaptersAndLiked], order: Order, owner: User)(implicit ctx: Context) = layout(
    title = s"被${owner.titleUsername}创建的研习",
    active = "owner",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.byOwner(owner.username, o)
  )(frag(
    userLink(owner),
    "'s studies"
  ))

  def mine(pag: Paginator[WithChaptersAndLiked], order: Order, me: User)(implicit ctx: Context) = layout(
    title = s"我的研习",
    active = "mine",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.mine(o)
  )("我的研习")

  def mineLikes(
    pag: Paginator[WithChaptersAndLiked],
    order: Order,
    me: User
  )(implicit ctx: Context) = layout(
    title = "我收藏的研习",
    active = "mineLikes",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.mineLikes(o)
  )("我收藏的研习")

  def mineMember(pag: Paginator[WithChaptersAndLiked], order: Order, me: User)(implicit ctx: Context) = layout(
    title = s"我参与的研习",
    active = "mineMember",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.mineMember(o)
  )("我参与的研习")

  def minePublic(pag: Paginator[WithChaptersAndLiked], order: Order, me: User)(implicit ctx: Context) = layout(
    title = "我公共的研习",
    active = "minePublic",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.minePublic(o)
  )("我公共的研习")

  def minePrivate(pag: Paginator[WithChaptersAndLiked], order: Order, me: User)(implicit ctx: Context) = layout(
    title = "我私有的研习",
    active = "minePrivate",
    order = order,
    pag = pag,
    searchFilter = "",
    url = o => routes.Study.minePrivate(o)
  )("我私有的研习")

  def search(pag: Paginator[WithChaptersAndLiked], text: String)(implicit ctx: Context) =
    views.html.base.layout(
      title = text,
      moreCss = cssTag("study.index"),
      wrapClass = "full-screen-force",
      moreJs = infiniteScrollTag
    ) {
        main(cls := "page-menu")(
          menu("search", Order.default),
          main(cls := "page-menu__content study-index box")(
            div(cls := "box__top")(
              searchForm(trans.search.txt(), text),
              bits.newForm()
            ),
            paginate(pag, routes.Study.search(text))
          )
        )
      }

  private[study] def paginate(pager: Paginator[WithChaptersAndLiked], url: Call)(implicit ctx: Context) =
    if (pager.currentPageResults.isEmpty) div(cls := "nostudies")(
      iconTag("4"),
      p("没有更多了")
    )
    else div(cls := "studies list infinitescroll")(
      pager.currentPageResults.map { s =>
        div(cls := "study paginated")(bits.widget(s))
      },
      pagerNext(pager, np => addQueryParameter(url.url, "page", np))
    )

  private[study] def menu(active: String, order: Order)(implicit ctx: Context) =
    st.aside(cls := "page-menu__menu subnav")(
      a(cls := active.active("all"), href := routes.Study.all(order.key))("所有研习"),
      ctx.me.map { bits.authLinks(_, active, order) } /*,
      a(cls := "text", dataIcon := "", href := "/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way")("什么是研习?")*/
    )

  private[study] def searchForm(placeholder: String, value: String) =
    form(cls := "search", action := routes.Study.search(), method := "get")(
      input(name := "q", st.placeholder := placeholder, st.value := value),
      submitButton(cls := "button", dataIcon := "y")
    )

  private def layout(
    title: String,
    active: String,
    order: Order,
    pag: Paginator[WithChaptersAndLiked],
    url: controllers.Study.ListUrl,
    searchFilter: String
  )(titleFrag: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = cssTag("study.index"),
    wrapClass = "full-screen-force",
    moreJs = infiniteScrollTag
  ) {
      main(cls := "page-menu")(
        menu(active, order),
        main(cls := "page-menu__content study-index box")(
          div(cls := "box__top")(
            searchForm(title, s"$searchFilter${searchFilter.nonEmpty ?? " "}"),
            views.html.base.bits.mselect(
              "orders",
              span(order.name),
              (if (active == "all") Order.allButOldest else Order.all) map { o =>
                a(href := url(o.key), cls := (order == o).option("current"))(o.name)
              }
            ),
            bits.newForm()
          ),
          paginate(pag, url(order.key))
        )
      )
    }
}
