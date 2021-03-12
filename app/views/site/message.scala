package views
package html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import controllers.routes

object message {

  def apply(
    title: String,
    back: Boolean = true,
    icon: Option[String] = None,
    moreCss: Option[Frag] = None
  )(message: Modifier*)(implicit ctx: Context) =
    views.html.base.layout(title = title, moreCss = ~moreCss) {
      main(cls := "box box-pad")(
        h1(cls := List("text" -> icon.isDefined), dataIcon := icon)(title),
        p(message),
        br,
        back option embedJsUnsafe {
          """if (document.referrer) document.write('<a class="button text" data-icon="I" href="' + document.referrer + '">返回</a>');"""
        }
      )
    }

  def noBot(implicit ctx: Context) = apply("禁止机器人") {
    frag("对不起，机器人账号在这里是不允许的。")
  }

  def noEngine(implicit ctx: Context) = apply("禁止引擎辅助") {
    "对不起，引擎辅助在这里是不允许的。"
  }

  def noBooster(implicit ctx: Context) = apply("禁止超速") {
    "对不起，超速和堆沙袋是不允许的。"
  }

  def privateStudy(ownerId: User.ID)(implicit ctx: Context) = apply(
    title = s"${usernameOrId(ownerId)}的研习",
    icon = "4".some
  )("对不起！这个研习是私有的，您无法访问。")

  def streamingMod(implicit ctx: Context) = apply("Disabled while streaming") {
    frag(
      "This moderation feature is disabled while streaming,", br, "to avoid leaking sensible information."
    )
  }

  def challengeDenied(msg: String)(implicit ctx: Context) = apply(
    title = trans.challengeToPlay.txt(),
    icon = "j".some
  )(msg)

  def insightNoGames(u: User)(implicit ctx: Context) = apply(
    title = s"${u.username}还没有进行过有积分的比赛",
    icon = "7".some
  )(frag("在使用数据洞察前，", u.username, " 需要至少参加一场有积分的比赛。"))

  def teamCreateLimit(implicit ctx: Context) = apply("不能创建俱乐部") {
    "本周您已经创建一个俱乐部。"
  }

  def teamNotAvailable(implicit ctx: Context) = apply("俱乐部权限不足") {
    "您的操作权限不足，或者俱乐部已经关闭。"
  }

  def authFailed(implicit ctx: Context) = apply("403 - 访问被拒绝！") {
    "您正在尝试访问一个没有权限的资源。"
  }

  def teamOwnerCannotChange(userId: String)(implicit ctx: Context) = apply("操作失败") {
    s"成员 $userId 已经拥有俱乐部"
  }

}
