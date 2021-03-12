package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object menu {

  def apply(active: String)(implicit ctx: Context) = st.nav(cls := "page-menu__menu subnav")(
    isGranted(_.SeeReport) option
      a(cls := active.active("report"), href := routes.Report.list)("举报"),
    isGranted(_.ChangePermission) option
      a(cls := active.active("coach"), href := routes.Coach.modList(1, "applying"))("教练认证"),
    isGranted(_.ManageTeam) option
      a(cls := active.active("team"), href := routes.TeamCertification.modList(1, "applying"))("俱乐部认证"),
    isGranted(_.Cli) option
      a(cls := active.active("fishnet"), href := routes.Fishnet.clients)("Fishnet"),
    isGranted(_.ChatTimeout) option
      a(cls := active.active("public-chat"), href := routes.Mod.publicChat)("公共聊天"),
    isGranted(_.SeeReport) option
      a(cls := active.active("gamify"), href := routes.Mod.gamify)("名人堂"),
    isGranted(_.UserSearch) option
      a(cls := active.active("search"), href := routes.Mod.search)("用户查询"),
    isGranted(_.SetEmail) option
      a(cls := active.active("email"), href := routes.Mod.emailConfirm)("邮箱确认"),
    isGranted(_.PracticeConfig) option
      a(cls := active.active("practice"), href := routes.Practice.config)("练习编辑"),
    isGranted(_.ManageTournament) option
      a(cls := active.active("tour"), href := routes.TournamentCrud.index(1))("锦标赛"),
    isGranted(_.ManageEvent) option
      a(cls := active.active("event"), href := routes.Event.manager)("事件"),
    isGranted(_.SeeReport) option
      a(cls := active.active("log"), href := routes.Mod.log)("管理员日志"),
    isGranted(_.SeeReport) option
      a(cls := active.active("irwin"), href := routes.Irwin.dashboard)("欧文仪表盘"),
    isGranted(_.Shadowban) option
      a(cls := active.active("panic"), href := routes.Mod.chatPanic)(
        "Chat Panic: ",
        strong(if (isChatPanicEnabled) "ON" else "OFF")
      ),
    isGranted(_.Settings) option
      a(cls := active.active("setting"), href := routes.Dev.settings)("设置"),
    isGranted(_.Cli) option
      a(cls := active.active("cli"), href := routes.Dev.cli)("CLI")
  )
}
