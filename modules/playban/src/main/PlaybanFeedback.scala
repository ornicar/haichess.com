package lila.playban

import lila.chat.{ Chat, ChatApi }
import lila.game.Pov

private final class PlaybanFeedback(
    chatApi: ChatApi,
    lightUser: lila.common.LightUser.Getter
) {

  private val tempBan = "会被临时禁赛。"

  def abort(pov: Pov): Unit = tell(pov, s"警告, {user}. 终止太多比赛$tempBan")

  def noStart(pov: Pov): Unit = tell(pov, s"警告, {user}. 没有正常开始比赛$tempBan")

  def rageQuit(pov: Pov): Unit = tell(pov, s"警告, {user}. 没有认输就离开比赛$tempBan")

  def sitting(pov: Pov): Unit = tell(pov, s"警告, {user}. 浪费比赛时间而不认输$tempBan")

  def sandbag(pov: Pov): Unit = tell(pov, s"警告, {user}. 故意输掉多场比赛将被禁赛。")

  private def tell(pov: Pov, template: String): Unit =
    pov.player.userId foreach { userId =>
      lightUser(userId) foreach { light =>
        val message = template.replace("{user}", light.fold(userId)(_.name))
        chatApi.userChat.volatile(Chat.Id(pov.gameId), message)
      }
    }
}
