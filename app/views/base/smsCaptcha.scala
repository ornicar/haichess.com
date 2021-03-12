package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object smsCaptcha {

  def apply(form: play.api.data.Form[_], label: String = "手机号", showCode: Boolean = true)(implicit ctx: Context) = div(cls := "smsCaptcha")(
    form3.hidden(form("template")),
    form3.group(form("cellphone"), label, half = true)(form3.input(_)(required)),
    showCode option form3.group(form("code"), "验证码")(f => div(cls := "code-send")(
      form3.input(f, typ = "number")(required),
      button(tpe := "button", cls := "button send disabled", disabled)(
        span(cls := "clock none")(
          span(cls := "time"),
          nbsp
        ),
        span(cls := "resend none")("后重新发送"),
        span(cls := "dosend")("发送验证码")
      )
    )),
    form3.globalError(form)
  )
}
