package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.{ CellphoneAddress }
import controllers.routes

object cellphoneConfirm {

  def apply(u: lila.user.User, cellphoneAddress: Option[CellphoneAddress], form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = s"${u.username} - ${trans.editProfile.txt()}",
    active = "cellphoneConfirm",
    evenMoreJs = frag(
      smsCaptchaTag
    )
  ) {
      div(cls := "account box box-pad")(
        h1(cls := "text")("手机绑定"),
        cellphoneAddress.fold {
          postForm(cls := "form3", dataSmsrv := 1, action := routes.Account.cellphoneConfirmApply)(
            views.html.base.smsCaptcha(form),
            form3.action(form3.submit(trans.apply()))
          )
        } { cpa =>
          div(
            span(style := "font-size: 2em;")(cpa.toString),
            nbsp,
            a(href := routes.Account.cellphoneModify)("修改")
          )
        }
      )
    }
}
