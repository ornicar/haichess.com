package views.html.offlineContest

import play.api.mvc.Call
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.offlineContest.OffContest
import controllers.routes

object list {

  type PageUrl = (Option[Int], String) => Call
  type QueryPageUrl = Option[Int] => Call

  def current(pager: Paginator[OffContest], status: Option[OffContest.Status], text: String)(implicit ctx: Context) = layout(
    title = "比赛编排",
    active = "current",
    url = (s, q) => routes.OffContest.currentPage(s, q),
    queryUrl = s => routes.OffContest.currentPage(s),
    pager = pager,
    status = status,
    text = text
  )("比赛编排")

  def history(pager: Paginator[OffContest], status: Option[OffContest.Status], text: String)(implicit ctx: Context) = layout(
    title = "历史比赛",
    active = "history",
    url = (s, q) => routes.OffContest.historyPage(s, q),
    queryUrl = s => routes.OffContest.historyPage(s),
    pager = pager,
    status = status,
    text = text
  )("历史比赛")

  private def layout(
    title: String,
    active: String,
    url: PageUrl,
    queryUrl: QueryPageUrl,
    pager: Paginator[OffContest],
    status: Option[OffContest.Status],
    text: String
  )(titleFrag: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = cssTag("offlineContest"),
    wrapClass = "full-screen-force",
    moreJs = infiniteScrollTag
  ) {
      val select = active match {
        case "current" => OffContest.Status.currentChoice
        case "history" => OffContest.Status.historyChoice
      }
      main(cls := "page-menu")(
        menu(active),
        main(cls := "page-menu__content contest-index box")(
          div(cls := "box__top")(
            st.form(cls := "search", action := queryUrl(status.map(_.id)), method := "get")(
              input(name := "q", st.placeholder := "搜索名称", st.value := text),
              submitButton(cls := "button", dataIcon := "y")
            ),
            views.html.base.bits.mselect(
              "status",
              status.fold("所有")(_.name),
              select map { s =>
                a(href := url(s._1, text), cls := status.??(_.id == s._1).option("current"))(s._2)
              }
            ),
            a(cls := "button button-green new-contest", href := routes.OffContest.createForm, dataIcon := "O")
          ),
          paginate(pager, url(status.map(_.id), text))
        )
      )
    }

  private def menu(active: String)(implicit ctx: Context) = {
    def activeCls(c: String) = cls := (c == active).option("active")
    st.aside(cls := "page-menu__menu subnav")(
      a(activeCls("current"), href := routes.OffContest.currentPage(None, "", 1))("比赛编排"),
      a(activeCls("history"), href := routes.OffContest.historyPage(None, "", 1))("历史比赛")
    )
  }

  private def paginate(pager: Paginator[OffContest], url: Call)(implicit ctx: Context) =
    if (pager.currentPageResults.isEmpty) div(cls := "no-contest")(
      iconTag("4"),
      p("没有更多了~")
    )
    else div(cls := "contests list infinitescroll")(
      pager.currentPageResults.map { c =>
        div(cls := "paginated contest")(widget(c))
      },
      pagerNext(pager, np => addQueryParameter(url.url, "page", np))
    )

  def widget(c: OffContest)(implicit ctx: Context) = frag(
    a(cls := "overlay", href := routes.OffContest.show(c.id)),
    table(
      tr(
        td(
          img(cls := "logo", src := c.logo.fold(staticUrl("images/contest.svg")) { l => dbImageUrl(l) })
        ),
        td(
          div(cls := "contest-name")(c.name, nbsp, c.groupName),
          div(cls := "organizer")("主办方：", c.typ match {
            case OffContest.Type.Public | OffContest.Type.TeamInner => teamLinkById(c.organizer, false)
            case OffContest.Type.ClazzInner => clazzLinkById(c.organizer)
          })
        )
      ),
      tr(
        td(c.status.name),
        td(c.typ.name)
      ),
      tr(
        td,
        td(c.rule.name, nbsp, c.rounds, "轮")
      ),
      tr(
        td,
        td(c.nbPlayers, "人")
      )
    )
  )

}
