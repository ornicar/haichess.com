package lila.security

import scalatags.Text.all._

import lila.common.{ Lang, EmailAddress }
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }

final class AutomaticEmail(
    mailgun: Mailgun,
    baseUrl: String
) {

  import Mailgun.html._

  def welcome(user: User, email: EmailAddress)(implicit lang: Lang): Funit = {
    val profileUrl = s"$baseUrl/@/${user.username}"
    val editUrl = s"$baseUrl/account/profile"
    mailgun send Mailgun.Message(
      to = email,
      subject = trans.welcome_subject.literalTxtTo(lang, List(user.username)),
      text = s"""
${trans.welcome_text.literalTxtTo(lang, List(profileUrl, editUrl))}

${Mailgun.txt.serviceNote}
""",
      htmlBody = standardEmail(
        trans.welcome_text.literalTxtTo(lang, List(profileUrl, editUrl))
      ).some
    )
  }

  def onTitleSet(username: String)(implicit lang: Lang): Funit = for {
    user <- UserRepo named username flatten s"No such user $username"
    emailOption <- UserRepo email user.id
  } yield for {
    title <- user.title
    email <- emailOption
  } yield {

    val profileUrl = s"$baseUrl/@/${user.username}"
    val body = s"""您好,

感谢您在haichess上确认您的 $title 称号。
现在可以在您的个人资料页上看到它： ${baseUrl}/@/${user.username}.

祝好，

Haichess团队
"""

    mailgun send Mailgun.Message(
      to = email,
      subject = s"Haichess $title 称号确认",
      text = s"""
$body

${Mailgun.txt.serviceNote}
""",
      htmlBody = standardEmail(body).some
    )
  }

  def onBecomeCoach(user: User)(implicit lang: Lang): Funit =
    UserRepo email user.id flatMap {
      _ ?? { email =>
        val body = s"""您好,

我们很高兴欢迎您成为Haichess认证教练。
完善您的教练资料 ${baseUrl}/coach/edit.

祝好，

Haichess团队
"""

        mailgun send Mailgun.Message(
          to = email,
          subject = "Haichess 教练认证通过",
          text = s"""
$body

${Mailgun.txt.serviceNote}
""",
          htmlBody = standardEmail(body).some
        )
      }
    }

  def onFishnetKey(userId: User.ID, key: String)(implicit lang: Lang): Funit = for {
    user <- UserRepo named userId flatten s"No such user $userId"
    emailOption <- UserRepo email user.id
  } yield emailOption ?? { email =>

    val body = s"""您好,

这是您的fishnet密钥

$key


请把它当作密码。您可以在多台计算机上使用同一密钥（即使在同一时间），但你不应该与任何人分享。

非常感谢你的帮助！感谢你，全世界的象棋爱好者将享受快速和强大的分析为他们的游戏。


祝好，

Haichess团队
"""

    mailgun send Mailgun.Message(
      to = email,
      subject = "请接收您的fishnet密钥",
      text = s"""
$body

${Mailgun.txt.serviceNote}
""",
      htmlBody = standardEmail(body).some
    )
  }
}
