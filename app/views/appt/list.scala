package views.html
package appt

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.appt.{ Appt, ApptRecord }
import controllers.routes

object list {

  val separator = " • "
  def apply(pager: Paginator[Appt])(implicit ctx: Context) =
    views.html.base.layout(
      title = "约棋记录",
      moreCss = cssTag("appt"),
      moreJs = frag(
        infiniteScrollTag,
        jsTag("appt.list.js")
      )
    ) {
        main(cls := "box page-small apptlist")(
          div(cls := "box__top")(
            h1("约棋记录")
          ),
          table(cls := "slist")(
            thead(
              tr(
                th("对局"),
                th("约棋人"),
                th("比赛时间"),
                th("约棋消息"),
                th("对手状态"),
                th("操作")
              )
            ),
            if (pager.nbResults > 0) {
              tbody(cls := "infinitescroll")(
                pagerNextTable(pager, np => routes.Appt.page(np).url),
                pager.currentPageResults.map { appt =>
                  val record = appt.currentRecord
                  tr(cls := "paginated")(
                    td(
                      div(
                        appt.showClock,
                        separator,
                        (if (appt.rated) trans.rated else trans.casual).txt(),
                        separator,
                        appt.variant.name
                      ),
                      appt.contest.map { c =>
                        div(
                          contestLink(c.id, c.name),
                          nbsp,
                          span(s"第${c.roundNo}轮"),
                          span(s"#${c.boardNo}")
                        )
                      }
                    ),
                    td(record.applyBy | "系统"),
                    td(record.time.toString("yyyy-MM-dd HH:mm")),
                    td(record.message | "-"),
                    td(destStatus(appt, record)),
                    td(style := "display:flex")(
                      a(cls := "button button-empty text", href := routes.Appt.form(appt.id))("查看"),
                      ctx.me.??(u => appt.contains(u.id)) option {
                        val color = appt.userColor((ctx.me err "user not login").id) err "can not find user"
                        if (!record.isConfirmed(color)) {
                          if (appt.currentTime.isAfterNow) {
                            a(cls := "button button-empty button-green text accept", href := routes.Appt.acceptXhr(appt.id))("接受")
                          } else if (appt.maxDateTime.isAfterNow) {
                            a(cls := "button button-empty text", href := routes.Appt.form(appt.id))("改时间")
                          } else a(cls := "button button-empty button-red text disabled")("已过期")
                        } else a(cls := "button button-empty button-green text disabled")("已接受")
                      },
                      !appt.isConfirmed && ctx.me.??(u => appt.contains(u.id) && appt.createBy.??(u.id == _)) option a(cls := "button button-empty button-red text cancel", href := routes.Challenge.cancel(appt.id))("取消"),
                      !appt.isConfirmed && ctx.me.??(u => appt.contains(u.id) && appt.createBy.??(u.id != _)) option a(cls := "button button-empty button-red text decline", href := routes.Challenge.decline(appt.id))("拒绝")
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

  private def destStatus(appt: Appt, record: ApptRecord)(implicit ctx: Context) = {
    val userId = appt.opponent((ctx.me err "user not login").id) err "can not find user"
    val color = appt.userColor(userId) err "can not find user"
    if (record.isConfirmed(color)) "已接受" else "-"
  }

}
