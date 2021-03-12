package views.html.user.show

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object newPlayer {

  def apply(u: User)(implicit ctx: Context) =
    div(cls := "new-player")(
      h2("欢迎来到haichess.com!"),
      p(
        "这是您的个人资料页.",
        u.profile.isEmpty option frag(
          br,
          "您想要 ",
          a(href := routes.Account.profile(None))("完善"), "它?"
        )
      ),
      /*      p(
        if (u.kid) "儿童模式已经被启用."
        else frag(
          "儿童会使用这个帐户吗？您可能需要启用 ", a(href := routes.Account.kid)("儿童模式"), "."
        )
      ),*/
      p(
        "现在怎么办？以下是一些建议:"
      ),
      ul(
        u.cellphone.isEmpty option li(a(href := routes.Account.cellphoneConfirm)("绑定手机")),
        li(a(href := routes.Account.profile(None))("完善个人资料")),
        !isGranted(_.Coach) option li(a(href := routes.Coach.certify)("注册为教练")),
        li(a(href := routes.Learn.index)("学习国际象棋规则")),
        li(a(href := routes.Puzzle.home)("战术题练习")),
        li(a(href := routes.Study.allDefault(1))("研习学习")),
        li(a(href := s"${routes.Lobby.home}#ai")("电脑对战")),
        li(a(href := s"${routes.Lobby.home}#hook")("与人对战")),
        li(a(href := routes.Pref.form("privacy"))("隐私设置")),
        li(a(href := routes.Pref.form("game-display"))("个人设置"))
      )
    )
}
