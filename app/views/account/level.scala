package views.html.account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import play.api.data.{ Field, Form }
import lila.user.{ FormSelect, User }
import controllers.routes

object level {

  def apply(me: User, referrer: Option[String], form: Form[_])(implicit ctx: Context) = frag(
    div(cls := "modal-content none")(
      h2("棋协等级"),
      postForm(cls := "form3", action := routes.Account.levelsApply(referrer))(
        table(cls := "level")(
          thead(
            tr(
              th("等级"),
              th("比赛名称"),
              th("参赛日期"),
              th("成绩")
            )
          ),
          tbody(
            tr(
              td(
                form3.groupNoLabel(form("level"))(f => form3.select(f, FormSelect.Level.levelLevelUp(f.value.get))),
                form3.hidden(form("current"), value = "1".some)
              ),
              td(
                form3.groupNoLabel(form("name"))(form3.input(_))
              ),
              td(
                form3.groupNoLabel(form("time"))(form3.input(_, klass = "flatpickr")(dataMaxDate := "today"))
              ),
              td(
                form3.groupNoLabel(form("result"))(form3.input(_))
              )
            )
          )
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交")
        )
      )
    )
  )

}
