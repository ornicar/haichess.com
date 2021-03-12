package views.html.contest

import play.api.data.Form
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.contest.{ Contest, PlayerWithUser, RequestWithUser }
import controllers.routes

object join {

  def apply(c: Contest, form: Form[_], captcha: lila.common.Captcha, error: Option[String] = None)(implicit ctx: Context) = {
    views.html.base.layout(
      title = c.fullName,
      moreCss = cssTag("contest.form"),
      moreJs = captchaTag
    ) {
        main(cls := "page-small")(
          div(cls := "box box-pad")(
            h1(c.fullName),
            postForm(cls := "form3", action := routes.Contest.join(c.id))(
              form3.group(form("message"), raw("消息"))(form3.textarea(_)()),
              p("您的加入请求将由管理员审核。"),
              views.html.base.captcha(form, captcha),
              error.map { badTag(_) },
              form3.actions(
                a(href := routes.Contest.show(c.id))(trans.cancel()),
                form3.submit("报名")
              )
            )
          )
        )
      }
  }

}
