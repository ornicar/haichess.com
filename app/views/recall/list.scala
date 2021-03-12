package views.html.recall

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.recall.Recall
import controllers.routes

object list {

  def apply(pager: Paginator[Recall])(implicit ctx: Context) =
    views.html.base.layout(
      title = "记谱记录",
      moreCss = cssTag("recall.list"),
      moreJs = infiniteScrollTag
    ) {
        main(cls := "box page-small")(
          h1("记谱记录"),
          table(cls := "slist")(
            thead(
              tr(
                th("名称"),
                th("棋色"),
                th("回合"),
                th("创建日期")
              )
            ),
            if (pager.nbResults > 0) {
              tbody(cls := "infinitescroll")(
                pagerNextTable(pager, np => routes.Recall.page(np).url),
                pager.currentPageResults.map { recall =>
                  tr(cls := "paginated")(
                    td(a(href := routes.Recall.show(recall.id))(recall.name)),
                    td(recall.color.map(_.fold("白方", "黑方")) | "双方"),
                    td(recall.turns.map(_.toString) | "所有"),
                    td(momentFromNow(recall.createAt))
                  )
                }
              )
            } else {
              tbody(
                tr(
                  td(colspan := 4)("暂无记录")
                )
              )
            }
          )
        )
      }

}
