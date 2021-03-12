package lila.message

case class ModPreset(subject: String, text: String)

/* From https://github.com/ornicar/lila/wiki/Canned-responses-for-moderators */
object ModPreset {

  /* First line is the message subject;
   * Other lines are the message body.
   * The message body can contain several lines.
   */
  val all = List("""

警告：攻击性言语

您 *必须* 友善的与其他棋手沟通，不管在什么情况下。

Haichess.com 对所有人来说是一个友好的环境。请注意多次在聊天中攻击其他人将被禁止聊天，情况严重的将封禁账号，不论是否VIP用户。

""", /* ---------------------------------------------------------------*/ """

警告：堆沙袋

在您的对局记录中，您为了刻意提升自己或其他人的等级分，故意输掉了一些比赛。这在 Haichess.com 是严格禁止的，如果持续发生类似情况，您的账号将被封禁，不论是否VIP用户。

""", /* ---------------------------------------------------------------*/ """

警告：超速

在您的对局记录中，为了刻意提升自己或其他人的等级分，您的对手故意输掉了一些比赛。这在 Haichess.com 是严格禁止的，如果持续发生类似情况，您的账号将被封禁，不论是否VIP用户。

""", /* ---------------------------------------------------------------*/ """

警告：频繁求和

频繁发出求和申请，干扰对手在 Haichess.com 是被禁止的。如果持续发生类似情况，您的账号将被封禁，不论是否VIP用户。

""", /* ---------------------------------------------------------------*/ """

吃过路兵

这个走法成为“吃过路兵”，是国际象棋的规则之一。如果您不了解，请查看 https://haichess.com/learn#/15 进行学习。

""", /* ---------------------------------------------------------------*/ """

请使用 /report

如果您想举报其他用户的异常行为，请访问 https://haichess.com/report

""", /* ---------------------------------------------------------------*/ """

警告：指责

在 Haichess.com ，直接指责对手使用计算机辅助或存在其他作弊行为是不允许的。如果您确定对手确实作弊，请使用举报功能说明情况，并提交给系统管理人员处理。

""", /* ---------------------------------------------------------------*/ """

警告：灌水

您可以提交一次您的链接，而不是在多个比赛、讨论区或者是每天都提交。如果持续发生类似情况，您的账号将被封禁，不论是否VIP用户。

""", /* ---------------------------------------------------------------*/ """

关于等级分返还

为了减轻等级分的膨胀效应，等级分返还必须满足特定的条件。目前的情况下并不满足返还的条件。请您需要理解的是，长期来看，等级分整体上会与棋手的真实水平匹配。

""", /* ---------------------------------------------------------------*/ """

警告：用户名或昵称暗示您拥有棋手称号

用户协议 2.5 关于用户名和昵称的管理（https://haichess.com/terms-of-service）中规定，您不能注册和使用暗示您拥有FIDE称号或嗨棋大师称号的名字或昵称。已经获得称号的棋手可通过系统提交相关信息，有管理员进行验证。如果两周内，您没有修改您的用户名，我们将关闭您的账号。

""", /* ---------------------------------------------------------------*/ """

用户名标识为计算机辅助

|我们的检测算法标识您的账号使用计算机进行辅助。如果您对此有异议，请联系客服。或者如果您是拥有称号的棋手，请在系统中提交相关信息，由管理员进行验证。

""") flatMap toPreset

  private def toPreset(txt: String) =
    txt.lines.toList.map(_.trim).filter(_.nonEmpty) match {
      case subject :: body => ModPreset(subject, body mkString "\n").some
      case _ =>
        logger.warn(s"Invalid mod message preset $txt")
        none
    }

  lazy val sandbagAuto = ModPreset(
    subject = "警告: 堆沙袋嫌疑",
    text =
      """您在很少的步数内就输掉了几盘棋。请注意您必须在有积分的比赛中全力而为。
故意输掉有几分的比赛成为“堆沙袋”，这在Haichess.com是严格禁止的。
谢谢您的理解。
      """.stripMargin
  )

  def maxFollow(username: String, max: Int) = ModPreset(
    subject = "关注用户数超过限制",
    text = s"""对不起，在Haichess.com，您不能关注超过 $max 个棋手。
在关注其他棋手前，您必须要先去掉一些已关注的棋手。请访问： https://haichess.com/@/$username/following
谢谢您的理解。"""
  )

  lazy val asJson = play.api.libs.json.Json.toJson {
    all.map { p =>
      List(p.subject, p.text)
    }
  }

  def bySubject(s: String) = all.find(_.subject == s)
}
