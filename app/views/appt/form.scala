package views.html
package appt

import play.api.data.Form
import lila.api.Context
import lila.appt.{ Appt, ApptRecord }
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object form {

  def apply(form: Form[_], appt: Appt)(implicit ctx: Context) = {
    val separator = " • "
    views.html.base.layout(
      title = "比赛约棋",
      moreJs = frag(
        flatpickrTag,
        delayFlatpickrStart,
        jsTag("appt.form.js")
      ),
      moreCss = cssTag("appt")
    ) {
        main(cls := "page-small appt-form")(
          div(cls := "box box-pad record")(
            h1("约棋记录"),
            table(cls := "slist")(
              thead(
                tr(
                  th("约棋人"),
                  th("比赛时间"),
                  th("约棋消息"),
                  th("对手状态"),
                  th("操作")
                )
              ),
              tbody(
                appt.records.reverse.map { record =>
                  tr /*(cls := List("current" -> record.current))*/ (
                    td(record.applyBy | "系统"),
                    td(record.time.toString("yyyy-MM-dd HH:mm")),
                    td(record.message | "-"),
                    td(destStatus(appt, record)),
                    td(
                      if (record.current) {
                        if (ctx.me.??(u => appt.contains(u.id))) {
                          val color = appt.userColor((ctx.me err "user not login").id) err "can not find user"
                          if (!record.isConfirmed(color)) {
                            if (appt.currentTime.isAfterNow) {
                              a(cls := "button button-empty button-green text accept", href := routes.Appt.acceptXhr(appt.id))("接受")
                            } else "已过期"
                          } else "已接受"
                        } else "-"
                      } else "-"
                    )
                  )
                }
              )
            )
          ),
          ctx.me.??(u => appt.contains(u.id)) option div(cls := "box box-pad create")(
            h1("比赛约棋"),
            div(cls := "top")(
              div(cls := "info")(
                div(cls := "titles")(
                  appt.contest.map { c =>
                    div(cls := "contest-name")(
                      contestLink(c.id, c.name),
                      nbsp,
                      span(s"第${c.roundNo}轮"),
                      span(s"#${c.boardNo}")
                    )
                  },
                  div(cls := "setup")(
                    appt.showClock,
                    separator,
                    (if (appt.rated) trans.rated else trans.casual).txt(),
                    separator,
                    appt.variant.name
                  )
                ),
                div(cls := "players")(
                  div(cls := "player text")(
                    appt.isChallenge option strong("棋手："),
                    appt.isContest option strong("白方："),
                    userIdLink(appt.whitePlayerUid, none)
                  ),
                  div(cls := "player text")(
                    appt.isChallenge option strong("棋手："),
                    appt.isContest option strong("黑方："),
                    userIdLink(appt.blackPlayerUid, none)
                  )
                ),
                div(cls := "times")(
                  div(
                    strong("开始时间："),
                    appt.minDateTime.toString("yyyy年MM月dd日 HH:mm")
                  ),
                  div(
                    strong("结束时间："),
                    appt.maxDateTime.toString("yyyy年MM月dd日 HH:mm")
                  )
                )
              ),
              div(cls := "board")(
                (chess.StartingPosition.initial.fen != appt.position) option div(cls := "board-warp")(
                  (chess.format.Forsyth << appt.position).map { situation =>
                    span(
                      cls := s"mini-board cg-wrap parse-fen is2d ${appt.variant.key}",
                      dataColor := situation.color.name,
                      dataFen := appt.position
                    )(cgWrapContent)
                  }
                )
              )
            ),
            postForm(cls := "form3", action := routes.Appt.create(appt.id))(
              form3.group(form("time"), raw("比赛时间"), help = raw("建议选择一个您能接受的最早时间，这样双方可以按从早到晚的顺序来约，更快约定时间").some)(f =>
                form3.input(f, klass = "flatpickr")(
                  dataEnableTime := true,
                  datatime24h := true
                )(dataMinDate := appt.minDateTime.toString("yyyy-MM-dd HH:mm"), dataMaxDate := appt.maxDateTime.toString("yyyy-MM-dd HH:mm"))),
              form3.group(form("message"), raw("留言"))(form3.textarea(_)(rows := 6)),
              globalError(form),
              form3.action(
                if (appt.isConfirmed) {
                  form3.submit(content = "已完成", klass = "disabled", isDisable = true)
                } else if (appt.maxDateTime.isBeforeNow) {
                  form3.submit(content = "已过期", klass = "disabled", isDisable = true)
                } else form3.submit("提交")
              )
            )
          )
        )
      }
  }

  private def destStatus(appt: Appt, record: ApptRecord)(implicit ctx: Context) = {
    val userId = appt.opponent((ctx.me err "user not login").id) err "can not find user"
    val color = appt.userColor(userId) err "can not find user"
    if (record.isConfirmed(color)) "已接受" else "-"
  }

}

