package views.html.errors

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.errors.{ GameErrors, PuzzleErrors }
import play.api.data.Form
import play.mvc.Call
import controllers.routes

object bits {

  private val dataLastMove = attr("data-lastmove")

  private[errors] def layout(
    title: String,
    active: String,
    form: Form[_],
    pager: Paginator[_],
    call: Call,
    showCheckbox: Boolean = false
  )(topFrag: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreJs = frag(
      infiniteScrollTag,
      flatpickrTag,
      delayFlatpickrStart,
      jsTag("errors.js")
    ),
    moreCss = cssTag("errors")
  ) {
      main(cls := "page-menu errors")(
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
    frag(
      a(activeCls("puzzle"), href := routes.Errors.puzzle(1))("战术题"),
      a(activeCls("game"), href := routes.Errors.game(1))("对局")
    )
  }

  private[errors] def paginate(pager: Paginator[_], call: Call, active: String, form: Form[_], showCheckbox: Boolean = false)(implicit ctx: Context) = {
    var params = ""
    form.data.foreach {
      case (key, value) =>
        params = params.concat("&").concat(key).concat("=").concat(value)
    }
    val url =
      if (call.url.contains("?"))
        call.url.concat(params)
      else
        call.url.concat("?a=1").concat(params)

    if (pager.currentPageResults.isEmpty) div(cls := "no-more")(
      iconTag("4"),
      p("没有更多了")
    )
    else div(cls := "list infinitescroll")(
      pager.currentPageResults.map {
        case puzzle: PuzzleErrors => miniBoard(puzzle.id, puzzle.fen, puzzle.color.name, puzzle.lastMove,
          s"${routes.Puzzle.errorPuzzle(puzzle.puzzleId).toString.concat("?rating=").concat(puzzle.rating.toString).concat("&time=").concat(puzzle.createAt.toString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")).concat(params)}") {
            div("难度：", puzzle.rating)
          }
        case game: GameErrors => miniBoard(game.id, game.fen, game.color.name, game.lastMove, s"${routes.Round.watcher(game.gameId, game.color.name).toString}#${game.ply}") {
          div(game.judgement.name)
        }
      },
      pagerNext(pager, np => addQueryParameter(url, "page", np))
    )
  }

  private[errors] def miniBoard(id: String, fen: String, color: String, lastMove: Option[String], call: String)(bottomFrag: Frag) = {
    a(cls := "paginated", target := "_blank", dataId := id, dataHref := call)(
      div(
        cls := "mini-board cg-wrap parse-fen is2d",
        dataColor := color,
        dataFen := fen,
        dataLastMove := lastMove
      )(cgWrapContent),
      div(cls := "btm")(bottomFrag)
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
      as.contains("delete") option option(value := "delete")("删除")
    )
  )

}
