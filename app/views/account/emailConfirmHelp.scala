package views.html
package account

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.security.EmailConfirm.Help._

import controllers.routes

object emailConfirmHelp {

  private val title = "协助确认邮箱"

  def apply(form: Form[_], status: Option[Status])(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = cssTag("email-confirm"),
    moreJs = jsTag("emailConfirmHelp.js")
  )(frag(
      main(cls := "page-small box box-pad email-confirm-help")(
        h1(title),
        p("您注册了，但是没有收到确认的邮件？"),
        st.form(cls := "form3", action := routes.Account.emailConfirmHelp, method := "get")(
          form3.split(
            form3.group(
              form("username"),
              trans.username(),
              help = raw("您注册的用户名是？").some
            ) { f =>
                form3.input(f)(pattern := lila.user.User.newUsernameRegex.regex)
              },
            div(cls := "form-group")(
              form3.submit(trans.apply())
            )
          )
        ),
        div(cls := "replies")(
          status map {
            case NoSuchUser(name) => frag(
              p("我们没有找到同名的用户：", strong(name), "。"),
              p(
                "您可以使用它来",
                a(href := routes.Auth.signup)("创建一个账号"), "。"
              )
            )
            case EmailSent(name, email) => frag(
              p("我们已经发送了一封邮件给 ", email.conceal, "。"),
              p(
                "它可能需要一点时间才能到达", br,
                strong("请等待10分钟再刷新您的收件箱。")
              ),
              p("并且请检查一下垃圾邮件目录，它可能被错误的分发。如果是这样，请将它移动到正常的收件箱中。"),
              p("如果您都尝试过了还是无法收到，请发邮件给我们："),
              hr,
              p(i(s"您好，请帮我确认账号：$name")),
              hr,
              p("拷贝粘贴以上内容发送到：", contactEmail),
              p("我们将协助您完成注册。")
            )
            case Confirmed(name) => frag(
              p("账号 ", strong(name), " 已经成功确认！"),
              p("您可以", a(href := routes.Auth.login)("使用 ", name, " 进行登录了"), "。"),
              p("您不需要再进行邮件确认。")
            )
            case Closed(name) => frag(
              p("账号 ", strong(name), " 已经被关闭。")
            )
            case NoEmail(name) => frag(
              p("账号 ", strong(name), " 未填写邮箱。"),
              p("请访问 ", a(href := routes.Page.contact)("联系我们"), "。")
            )
          }
        )
      )
    ))
}
