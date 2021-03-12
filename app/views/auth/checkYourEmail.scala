package views.html.auth

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object checkYourEmail {

  def apply(
    userEmail: Option[lila.security.EmailConfirm.UserEmail],
    form: Option[Form[_]] = None
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "查收邮件",
      moreCss = cssTag("email-confirm")
    ) {
        main(cls := s"page-small box box-pad email-confirm ${if (form.exists(_.hasErrors)) "error" else "anim"}")(
          h1(cls := "is-green text", dataIcon := "E")(trans.checkYourEmail()),
          p(trans.weHaveSentYouAnEmailClickTheLink()),
          h2("没接收到邮件？"),
          ol(
            li(h3(trans.ifYouDoNotSeeTheEmailCheckOtherPlaces())),
            userEmail.map(_.email).map { email =>
              li(
                h3("请确认您的邮箱是正确的："),
                br, br,
                postForm(action := routes.Auth.fixEmail)(
                  input(
                    id := "new-email",
                    tpe := "email",
                    required,
                    name := "email",
                    value := form.flatMap(_("email").value).getOrElse(email.value),
                    pattern := s"^((?!^${email.value}$$).)*$$"
                  ),
                  embedJsUnsafe("""
var email = document.getElementById("new-email");
var currentError = "这是您当前注册的邮箱。";
email.setCustomValidity(currentError);
email.addEventListener("input", function() {
email.setCustomValidity(email.validity.patternMismatch ? currentError : "");
      });"""),
                  submitButton(cls := "button")("修改"),
                  form.map { f =>
                    errMsg(f("email"))
                  }
                )
              )
            },
            li(
              h3("稍等一下"), br,
              "邮件可以需要一段时间到达，这取决于您的邮箱提供商。"
            ),
            li(
              h3("始终没有收到？"), br,
              "确认邮箱地址是正确的吗？", br,
              "您等待超过5分钟了码？", br,
              "如果是这样，",
              a(href := routes.Account.emailConfirmHelp)("请前往这个页面进行处理")
            )
          )
        )
      }
}
