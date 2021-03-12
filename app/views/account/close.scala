package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object close {

  def apply(u: lila.user.User, form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = s"${u.username} - ${trans.closeAccount.txt()}",
    active = "close"
  ) {
    div(cls := "account box box-pad")(
      h1(dataIcon := "j", cls := "text")(trans.closeAccount()),
      postForm(cls := "form3", action := routes.Account.closeConfirm)(
        div(cls := "form-group")(trans.closeAccountExplanation()),
        div(cls := "form-group")("即使情况不同，也不允许您以相同的名称开立新帐户。"),
        form3.passwordModified(form("passwd"), trans.password())(autocomplete := "off"),
        form3.actions(frag(
          a(href := routes.User.show(u.username))(trans.changedMindDoNotCloseAccount()),
          form3.submit(
            trans.closeAccount(),
            icon = "j".some,
            confirm = "关闭账号后不可恢复。是否确认？".some,
            klass = "button-red"
          )
        ))
      )
    )
  }
}
