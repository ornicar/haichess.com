package lila.user

import play.api.data._
import play.api.data.validation.Constraints
import play.api.data.Forms._
import User.ClearPassword
import lila.common.Form.ISODate
import lila.common.Region
import org.joda.time.DateTime

final class DataForm(authenticator: Authenticator) {

  val note = Form(mapping(
    "text" -> text(minLength = 3, maxLength = 2000),
    "mod" -> boolean
  )(NoteData.apply)(NoteData.unapply))

  case class NoteData(text: String, mod: Boolean)

  def username(user: User): Form[String] = Form(single(
    "username" -> text.verifying("changeUsernameNotSame", name =>
      name.toLowerCase == user.username.toLowerCase && name != user.username)
  )).fill(user.username)

  def usernameOf(user: User) = username(user) fill user.username

  def profile(user: User) = Form(mapping(
    "head" -> optional(text(minLength = 5, maxLength = 150)),
    "country" -> optional(text.verifying(Countries.codeSet contains _)),
    "bio" -> optional(nonEmptyText(maxLength = 600)),
    "firstName" -> nameField,
    "lastName" -> nameField,
    "fideRating" -> optional(number(min = 600, max = 3000)),
    "uscfRating" -> optional(number(min = 100, max = 3000)),
    "ecfRating" -> optional(number(min = 0, max = 300)),
    "links" -> optional(nonEmptyText(maxLength = 3000)),
    "province" -> optional(text.verifying(Region.Province.keySet contains _)),
    "city" -> optional(text.verifying(Region.City.keySet contains _)),
    "realName" -> nameField,
    "sex" -> optional(text.verifying(FormSelect.Sex.keySet contains _)),
    "levels" -> optional(mapping(
      "lvs" -> list(mapping(
        "level" -> text.verifying(FormSelect.Level.keySet contains _),
        "current" -> booleanNumber,
        "name" -> optional(nonEmptyText(maxLength = 30)),
        "time" -> optional(ISODate.isoDate),
        "result" -> optional(nonEmptyText(maxLength = 300))
      )(Level.apply)(Level.unapply))
    )(Levels.apply)(Levels.unapply)),
    "birthyear" -> optional(number(min = 1900, max = DateTime.now.getYear)),
    /*"mobile" -> optional(text.verifying(Constraints.pattern(regex = User.cellphoneRegex, error = "手机号格式错误")).verifying("手机号已存在", !cellphoneExits(user, _))),*/
    "wechat" -> optional(text.verifying(Constraints.pattern(regex = User.weiChatRegex, error = "微信号格式错误")).verifying("微信号已存在", !wechatExits(user, _)))
  )(Profile.apply)(Profile.unapply))

  def levels = Form(mapping(
    "lvs" -> list(mapping(
      "level" -> text.verifying(FormSelect.Level.keySet contains _),
      "current" -> booleanNumber,
      "name" -> optional(nonEmptyText(maxLength = 30)),
      "time" -> optional(ISODate.isoDate),
      "result" -> optional(nonEmptyText(maxLength = 300))
    )(Level.apply)(Level.unapply))
  )(Levels.apply)(Levels.unapply))

  def level = Form((mapping(
    "level" -> text.verifying(FormSelect.Level.keySet contains _),
    "current" -> booleanNumber,
    "name" -> optional(nonEmptyText(maxLength = 30)),
    "time" -> optional(ISODate.isoDate),
    "result" -> optional(nonEmptyText(maxLength = 300))
  )(Level.apply)(Level.unapply)))

  def levelsOf(user: User) = level.fill(user.profileOrDefault.currentLevel)

  private lazy val booleanNumber = number.verifying((v: Int) => v == 0 || v == 1)

  def profileOf(user: User) = profile(user) fill user.profileOrDefault

  private def nameField = optional(text(minLength = 2, maxLength = 20))

  private def wechatExits(user: User, wechat: String) = UserRepo wechatExits (user, wechat) awaitSeconds 3

  case class Passwd(
      oldPasswd: String,
      newPasswd1: String,
      newPasswd2: String
  ) {
    def samePasswords = newPasswd1 == newPasswd2
  }

  def passwd(u: User) = authenticator loginCandidate u map { candidate =>
    Form(mapping(
      "oldPasswd" -> nonEmptyText.verifying("incorrectPassword", p => candidate.check(ClearPassword(p))),
      "newPasswd1" -> password,
      "newPasswd2" -> password
    )(Passwd.apply)(Passwd.unapply).verifying("the new passwords don't match", _.samePasswords))
  }

  private def trimField(m: Mapping[String]) = m.transform[String](_.trim, identity)
  val password = trimField(nonEmptyText).verifying(
    Constraints minLength 6,
    Constraints maxLength 20,
    Constraints.pattern(
      regex = User.passwordRegex,
      error = "密码格式不正确, 6-20位英文数字下划线"
    )
  )
}

object DataForm {

  val title = Form(single("title" -> optional(nonEmptyText)))

  lazy val historicalUsernameConstraints = Seq(
    Constraints minLength 2,
    Constraints maxLength 20,
    Constraints.pattern(regex = User.historicalUsernameRegex, error = "用户名格式错误")
  )
  lazy val historicalUsernameField = text.verifying(historicalUsernameConstraints: _*)
}
