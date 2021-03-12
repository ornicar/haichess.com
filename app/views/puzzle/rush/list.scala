package views.html.puzzle.rush

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.puzzle.PuzzleRush
import controllers.routes

object list {

  def apply(pager: Paginator[PuzzleRush], mode: PuzzleRush.Mode, order: PuzzleRush.Order)(implicit ctx: Context) =
    views.html.base.layout(
      title = "冲刺记录",
      moreCss = cssTag("puzzleRush.list"),
      moreJs = infiniteScrollTag
    ) {
        main(cls := "box page-small")(
          div(cls := "box__top")(
            h1("冲刺记录"),
            div(cls := "box__top__actions")(
              views.html.base.bits.mselect(
                "rush-mode",
                mode.name,
                lila.puzzle.PuzzleRush.Mode.all map { m =>
                  a(href := routes.PuzzleRush.page(pager.currentPage, m.id, order.key), cls := (mode == m).option("current"))(m.name)
                }
              ),
              views.html.base.bits.mselect(
                "rush-order",
                order.name,
                lila.puzzle.PuzzleRush.Order.all map { o =>
                  a(href := routes.PuzzleRush.page(pager.currentPage, mode.id, o.key), cls := (order == o).option("current"))(o.name)
                }
              )
            )
          ),
          table(cls := "slist")(
            thead(
              tr(
                th("分数"),
                th("时间"),
                th("平均时间"),
                th("尝试数"),
                th("失败"),
                th("连胜"),
                th("日期")
              )
            ),
            if (pager.nbResults > 0) {
              tbody(cls := "infinitescroll")(
                pagerNextTable(pager, np => routes.PuzzleRush.page(np, mode.id, order.key).url),
                pager.currentPageResults.map { rush =>
                  tr(cls := "paginated")(
                    td(
                      a(href := routes.PuzzleRush.showById(rush.id))(
                        rush.result.map(_.win)
                      )
                    ),
                    td(rush.result.map(r => showTime(r.seconds))),
                    td(rush.result.map(r => showTime(r.avgTime))),
                    td(rush.result.map(_.nb)),
                    td(rush.result.map(_.loss)),
                    td(rush.result.map(_.winStreaks)),
                    td(
                      rush.endTime.map(momentFromNow(_))
                    )
                  )
                }
              )
            } else {
              tbody(
                tr(
                  td(colspan := 7)("暂无记录")
                )
              )
            }
          )
        )
      }

}
