package views.html
package auth

import play.api.data.Form
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object signup {

  def apply(form: Form[_], captcha: lila.common.Captcha)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.signUp.txt(),
      moreJs = frag(
        captchaTag,
        jsTag("signup.js"),
        fingerprintTag
      ),
      moreCss = frag(
        cssTag("form3-captcha"),
        cssTag("auth")
      ),
      csp = defaultCsp.withRecaptcha.some
    ) {
        main(cls := "auth auth-signup box box-pad")(
          h1(trans.signUp()),
          postForm(id := "signup_form", cls := "form3", action := routes.Auth.signupPost)(
            form3.globalError(form),
            form3.group(form("username"), "用户名", help = raw("中文或字母开头，只包含中文、字母、数字、下划线和连字符").some) { f =>
              frag(
                form3.input(f)(autofocus, required),
                p(cls := "error exists none")(trans.usernameAlreadyUsed())
              )
            },
            form3.group(form("password"), "密码", help = raw("6-20位英文数字下划线").some) { f =>
              form3.input(f, typ = "password")(required)
            },
            form3.group(form("email"), "电子邮箱")(form3.input(_, typ = "email")(required)),
            form3.group(form("level"), "棋协级别", help = frag("注册后，级别只能向高级别修改，请准确填写").some) { f =>
              form3.select(f, lila.user.FormSelect.Level.levelWithRating, default = "".some)
            },
            views.html.base.captcha(form, captcha),
            input(id := "signup-fp-input", name := "fp", tpe := "hidden"),
            div(cls := "agreement")(
              input(name := "agree", tpe := "checkbox"),
              span("我已阅读"),
              a(href := routes.Page.tos)("《用户协议》"),
              span("和"),
              a(href := routes.Page.privacy)("《隐私政策》"),
              span(cls := "is-gold", dataIcon := "", title := "电脑与电脑辅助棋手不允许参加对弈。在下棋时，请不要从国际象棋引擎、数据库或其他棋手那里得到帮助。另外，强烈建议您不要创建多个账号。过度使用多个帐号会导致帐户被封禁。")
            ),
            form3.submit("同意条款并注册", icon = none, klass = "big disabled", isDisable = true)
          )
        )
      }
}
