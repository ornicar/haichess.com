package views.html.resource

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.puzzle.Puzzle
import lila.resource.{ Capsule, Sorting, ThemeQuery }
import play.api.data.Form
import play.mvc.Call
import controllers.routes

object puzzle {

  def liked(form: Form[_], pager: Paginator[Puzzle], tags: Set[String])(implicit ctx: Context) = layout(
    title = "收藏的战术题",
    active = "liked",
    form = form,
    pager = pager,
    call = routes.Resource.puzzleLiked()
  ) {
      st.form(
        cls := "search_form",
        action := s"${routes.Resource.puzzleLiked()}#results",
        method := "GET"
      )(
          table(
            tr(
              td(
                form3.tags(form, "tags", tags)
              ),
              td(cls := "action")(
                pager.nbResults > 0 option frag(
                  form3.select(form("order"), Sorting.orders),
                  actions(List("unlike", "toCapsule"))
                )
              )
            )
          )
        )
    }

  def imported(form: Form[_], pager: Paginator[Puzzle], tags: Set[String])(implicit ctx: Context) = layout(
    title = "导入的战术题",
    active = "imported",
    form = form,
    pager = pager,
    call = routes.Resource.puzzleImported(),
    showCheckbox = true
  ) {
      st.form(
        cls := "search_form",
        action := s"${routes.Resource.puzzleImported()}#results",
        method := "GET"
      )(
          table(
            tr(
              td(colspan := 2)(
                form3.tags(form, "tags", tags)
              ),
              td(cls := "action")(
                pager.nbResults > 0 option actions(List("delete", "toCapsule"))
              )
            )
          )
        )
    }

  def theme(form: Form[_], pager: Paginator[Puzzle], tags: Set[String])(implicit ctx: Context) = layout(
    title = "主题搜索",
    active = "theme",
    form = form,
    pager = pager,
    call = routes.Resource.puzzleTheme()
  ) {
      st.form(
        cls := "search_form",
        action := s"${routes.Resource.puzzleTheme()}#results",
        method := "GET"
      )(
          table(
            tr(
              th(label("题号范围")),
              td(
                div(cls := "half")("从 ", form3.input3(form("idMin"), vl = "100000".some, "number")),
                div(cls := "half")("到 ", form3.input3(form("idMax"), vl = "306216".some, "number"))
              )
            ),
            tr(
              th(label("难度范围")),
              td(
                div(cls := "half")("从 ", form3.input(form("ratingMin"), "number")),
                div(cls := "half")("到 ", form3.input(form("ratingMax"), "number"))
              )
            ),
            tr(
              th(label("答案步数")),
              td(
                div(cls := "half")("从 ", form3.input(form("stepsMin"), "number")),
                div(cls := "half")("到 ", form3.input(form("stepsMax"), "number"))
              )
            ),
            tr(
              th(label("黑白")),
              td(
                form3.tagsWithKv(form, "pieceColor", ThemeQuery.pieceColor)
              )
            ),
            tr(
              th(label("阶段")),
              td(
                form3.tagsWithKv(form, "phase", ThemeQuery.phase)
              )
            ),
            tr(
              th(label("目的")),
              td(
                form3.tagsWithKv(form, "moveFor", ThemeQuery.moveFor)
              )
            ),
            tr(
              th(label("子力")),
              td(
                form3.tagsWithKv(form, "strength", ThemeQuery.strength)
              )
            ),
            tr(
              th(label("局面")),
              td(
                form3.tagsWithKv(form, "chessGame", ThemeQuery.chessGame)
              )
            ),
            tr(
              th(label("技战术")),
              td(
                form3.tagsWithKv(form, "subject", ThemeQuery.subject)
              )
            ),
            tr(
              th(label("综合")),
              td(
                form3.tagsWithKv(form, "comprehensive", ThemeQuery.comprehensive)
              )
            ),
            /*            tr(
              th(label("标签")),
              td(
                form3.tags(form, "tags", tags)
              )
            ),*/
            tr(
              th(label("排序")),
              td(
                form3.select(form("order"), Sorting.orders)
              )
            ),
            tr(
              th,
              td(cls := "action")(
                submitButton(cls := "button")("搜索")
              )
            )
          ),
          table(
            tr(
              td(cls := "action")(
                pager.nbResults > 0 option actions(List("toCapsule"))
              )
            )
          )
        )
    }

  def capsule(pager: Paginator[Puzzle], capsuleId: Capsule.ID)(implicit ctx: Context) = layout(
    title = "战术题列表",
    active = "capsule",
    form = lila.resource.DataForm.capsule.puzzleOrder,
    pager = pager,
    call = routes.Resource.puzzleCapsule(capsuleId)
  ) {
      st.form(
        cls := "search_form",
        action := s"${routes.Resource.puzzleCapsule(capsuleId)}#results",
        method := "GET"
      )(
          table(
            tr(
              td(cls := "action capsule")(
                pager.nbResults > 0 option actions(List("delete"))
              )
            )
          )
        )
    }

  /*  def targetUrl(active: String, puzzleId: Int, url: String) = {
    s"${routes.Puzzle.themePuzzle(puzzleId)}?${url.substring(url.indexOf("\\?") + 1)}"
  }*/

  private val dataLastMove = attr("data-lastmove")

  private[resource] def miniPuzzle(p: lila.puzzle.Puzzle, active: String, url: String, showCheckbox: Boolean = false) = {
    // targetUrl(active, p.id, url)
    a(cls := "paginated", target := "_blank", dataId := p.id, dataHref := routes.Puzzle.show(p.id, false, false))(
      div(
        cls := "mini-board cg-wrap parse-fen is2d",
        dataColor := p.color.name,
        dataFen := p.fenAfterInitialMove,
        dataLastMove := p.initialUci
      )(cgWrapContent),
      div(cls := "btm")(
        span((active != "imported") option label("难度：", if (p.isImport) "NA" else p.perf.glicko.rating.toInt))
      /*showCheckbox option input(tpe := "checkbox", id := s"cbx-${p.id}", name := "cbx-puzzle", value := p.id)*/
      )
    )
  }

  private[resource] def paginate(pager: Paginator[Puzzle], call: Call, active: String, form: Form[_], showCheckbox: Boolean = false)(implicit ctx: Context) = {
    var url = if (call.url.contains("?")) call.url else call.url.concat("?a=1")
    form.data.foreach {
      case (key, value) =>
        url = url.concat("&").concat(key).concat("=").concat(value)
    }

    if (pager.currentPageResults.isEmpty) div(cls := "no-more")(
      iconTag("4"),
      p("没有更多了")
    )
    else div(cls := "now-playing list infinitescroll")(
      pager.currentPageResults.map { p =>
        miniPuzzle(p, active, url, showCheckbox)
      },
      pagerNext(pager, np => addQueryParameter(url, "page", np))
    )
  }

  private[resource] def layout(
    title: String,
    active: String,
    form: Form[_],
    pager: Paginator[Puzzle],
    call: Call,
    showCheckbox: Boolean = false
  )(topFrag: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreJs = frag(
      infiniteScrollTag,
      jsTag("resource.js")
    ),
    moreCss = cssTag("resource")
  ) {
      main(cls := "page-menu resource")(
        st.aside(cls := "page-menu__menu subnav")(
          menuLinks(active)
        ),
        div(cls := "box")(
          topFrag,
          paginate(pager, call, active, form, showCheckbox)
        )
      )
    }

  def menuLinks(active: String)(implicit ctx: Context) = {
    def activeCls(c: String) = cls := (c == active).option("active")
    val accept = ctx.me.??(_.hasResource)
    frag(
      a(activeCls("imported"), href := routes.Resource.puzzleImported())("导入的战术题"),
      accept option frag(
        a(activeCls("liked"), href := routes.Resource.puzzleLiked())("收藏的战术题"),
        a(activeCls("theme"), href := routes.Resource.puzzleTheme())("主题搜索"),
        a(activeCls("capsule"), href := routes.Capsule.list())("战术题列表")
      ),
      !accept option frag(
        a(cls := "liked disabled")(span("收藏的战术题"), br, span(cls := "onlyMember")("会员可用")),
        a(cls := "theme disabled")(span("主题搜索"), br, span(cls := "onlyMember")("会员可用")),
        a(cls := "capsule disabled")(span("战术题列表"), br, span(cls := "onlyMember")("会员可用"))
      )
    )
  }

  def actions(as: List[String]) = frag(
    select(cls := "select")(
      option(value := "")("选中的（单击）"),
      option(value := "all")("选中所有"),
      option(value := "none")("取消选择")
    ),
    select(cls := "action")(
      option(value := "")("操作"),
      as.contains("delete") option option(value := "delete")("删除"),
      as.contains("unlike") option option(value := "unlike")("取消收藏"),
      as.contains("toCapsule") option option(value := "toCapsule")("添加到列表")
    )
  )

  def capsuleModal(capsules: List[Capsule])(implicit ctx: Context) = frag(
    div(cls := "modal-content modal-capsule none")(
      h2("选择战术题列表"),
      postForm(cls := "form3")(
        div(cls := "capsule-scroll")(
          table(cls := "capsule-list")(
            capsules.map { capsule =>
              tr(
                td(
                  input(tpe := "radio", id := capsule.id, name := "capsule", value := capsule.id),
                  nbsp,
                  label(`for` := capsule.id)(capsule.name)
                ),
                td(capsule.total)
              )
            }
          )
        ),
        p(cls := "is-gold", dataIcon := "")("每个列表最多添加15道战术题"),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交")
        )
      )
    )
  )

}
