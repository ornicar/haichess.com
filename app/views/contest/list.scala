package views.html.contest

import play.api.mvc.Call
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.contest.Contest
import lila.user.User
import controllers.routes

object list {

  type PageUrl = (Option[Int], String) => Call
  type QueryPageUrl = Option[Int] => Call

  def all(pag: Paginator[Contest], status: Option[Contest.Status], text: String)(implicit ctx: Context) = layout(
    title = "所有比赛",
    active = "all",
    url = (s, q) => routes.Contest.allPage(s, q),
    queryUrl = s => routes.Contest.allPage(s),
    pag = pag,
    status = status,
    text = text
  )("所有比赛")

  def belong(pag: Paginator[Contest], status: Option[Contest.Status], text: String)(implicit ctx: Context) = layout(
    title = "我参加的比赛",
    active = "belong",
    url = (s, q) => routes.Contest.belongPage(s, q),
    queryUrl = s => routes.Contest.belongPage(s),
    pag = pag,
    status = status,
    text = text
  )("我参加的比赛")

  def owner(pag: Paginator[Contest], status: Option[Contest.Status], text: String)(implicit ctx: Context) = layout(
    title = "我创建的比赛",
    active = "owner",
    url = (s, q) => routes.Contest.ownerPage(s, q),
    queryUrl = s => routes.Contest.ownerPage(s),
    pag = pag,
    status = status,
    text = text
  )("我创建的比赛")

  def finish(pag: Paginator[Contest], status: Option[Contest.Status], text: String)(implicit ctx: Context) = layout(
    title = "历史比赛",
    active = "finish",
    url = (s, q) => routes.Contest.finishPage(s, q),
    queryUrl = s => routes.Contest.finishPage(s),
    pag = pag,
    status = status,
    text = text
  )("历史比赛")

  private def layout(
    title: String,
    active: String,
    url: PageUrl,
    queryUrl: QueryPageUrl,
    pag: Paginator[Contest],
    status: Option[Contest.Status],
    text: String
  )(titleFrag: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = cssTag("contest.list"),
    wrapClass = "full-screen-force",
    moreJs = infiniteScrollTag
  ) {
      val select = active match {
        case "all" => Contest.Status.allSelect
        case "belong" => Contest.Status.belongSelect
        case "owner" => Contest.Status.ownerSelect
        case "finish" => Contest.Status.finishSelect
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
                a(href := url(s._1, text), cls := (status == s).option("current"))(s._2)
              }
            ),
            ctx.me.?? { user =>
              (user.hasTeam || isGranted(_.Coach, user) || isGranted(_.ManageContest, user))
            } option a(cls := "button button-green new-contest", href := routes.Contest.createForm, dataIcon := "O")
          ),
          paginate(pag, url(status.map(_.id), text))
        )
      )
    }

  private def menu(active: String)(implicit ctx: Context) = {
    def activeCls(c: String) = cls := (c == active).option("active")
    st.aside(cls := "page-menu__menu subnav")(
      a(activeCls("all"), href := routes.Contest.allPage(None, "", 1))("所有比赛"),
      a(activeCls("belong"), href := routes.Contest.belongPage(None, "", 1))("我参加的比赛"),
      a(activeCls("owner"), href := routes.Contest.ownerPage(None, "", 1))("我创建的比赛"),
      a(activeCls("finish"), href := routes.Contest.finishPage(None, "", 1))("历史比赛")
    )
  }

  private def paginate(pager: Paginator[Contest], url: Call)(implicit ctx: Context) =
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

  def widget(c: Contest)(implicit ctx: Context) = frag(
    a(cls := "overlay", href := routes.Contest.show(c.id)),
    table(
      tr(
        td(
          img(cls := "logo", src := c.logo.fold(staticUrl("images/contest.svg")) { l => dbImageUrl(l) })
        ),
        td(
          div(cls := "contest-name")(c.name, nbsp, c.groupName),
          div(cls := "organizer")("主办方：", c.typ match {
            case Contest.Type.Public | Contest.Type.TeamInner => teamLinkById(c.organizer, false)
            case Contest.Type.ClazzInner => clazzLinkById(c.organizer)
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
        td(c.variant.name, nbsp, c.clock.toString)
      ),
      (chess.StartingPosition.initial.fen != c.position.fen) option tr(
        td,
        td("指定初始位置：", "是")
      ),
      tr(
        td,
        td("比赛时间：", c.startsAt.toString("yyyy-MM-dd HH:mm"))
      ),
      c.appt option tr(
        td,
        td("自由约棋：", "是")
      )
    )
  )

}
