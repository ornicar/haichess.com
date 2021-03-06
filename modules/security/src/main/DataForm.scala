package lila.security

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints
import lila.common.{ CellphoneAddress, EmailAddress, LameName, SmsTemplate }
import lila.user.{ FormSelect, TotpSecret, User, UserRepo }
import User.{ ClearPassword, TotpToken }
import akka.actor.ActorSelection

final class DataForm(
    val captcherActor: akka.actor.ActorSelection,
    val smsCaptcherActor: akka.actor.ActorSelection,
    authenticator: lila.user.Authenticator,
    emailValidator: EmailAddressValidator
) extends lila.hub.CaptchedForm with lila.hub.SmsCaptchedForm {

  import DataForm._

  override def captcher: ActorSelection = captcherActor
  override def smsCaptcha: ActorSelection = smsCaptcherActor

  case class Empty(gameId: String, move: String)

  val empty = Form(mapping(
    "gameId" -> text,
    "move" -> text
  )(Empty.apply)(_ => None)
    .verifying(captchaFailMessage, validateCaptcha _))

  def emptyWithCaptcha = withCaptcha(empty)

  private val anyEmail = trimField(text).verifying(Constraints.emailAddress)
  private val acceptableEmail = anyEmail.verifying(emailValidator.acceptableConstraint)
  private def acceptableUniqueEmail(forUser: Option[User]) =
    acceptableEmail.verifying(emailValidator uniqueConstraint forUser)

  private def withAcceptableDns(m: Mapping[String]) = m verifying emailValidator.withAcceptableDns

  private def trimField(m: Mapping[String]) = m.transform[String](_.trim, identity)

  private val preloadEmailDnsForm = Form(single("email" -> acceptableEmail))

  def preloadEmailDns(implicit req: play.api.mvc.Request[_]): Funit =
    preloadEmailDnsForm.bindFromRequest.fold(
      _ => funit,
      email => emailValidator.preloadDns(EmailAddress(email))
    )

  object signup {

    val username = trimField(nonEmptyText).verifying(
      Constraints minLength 2,
      Constraints maxLength 20,
      Constraints.pattern(
        regex = User.newUsernamePrefix,
        error = "usernamePrefixInvalid"
      ),
      Constraints.pattern(
        regex = User.newUsernameSuffix,
        error = "usernameSuffixInvalid"
      ),
      Constraints.pattern(
        regex = User.newUsernameChars,
        error = "usernameCharsInvalid"
      )
    ).verifying("usernameUnacceptable", u => !LameName.sensitive(u))
      .verifying("?????????????????????????????????????????????????????????????????????", u => !LameName.reserved(u))
      .verifying("usernameAlreadyUsed", u => !UserRepo.nameExists(u).awaitSeconds(4))

    val password = trimField(nonEmptyText).verifying(
      Constraints minLength 6,
      Constraints maxLength 20,
      Constraints.pattern(
        regex = User.passwordRegex,
        error = "?????????????????????, 6-20????????????????????????"
      )
    )

    val website = Form(mapping(
      "username" -> trimField(username),
      "password" -> trimField(password),
      "email" -> withAcceptableDns(acceptableUniqueEmail(none)),
      "level" -> text.verifying(FormSelect.Level.keySet.contains _),
      "fp" -> optional(nonEmptyText),
      "g-recaptcha-response" -> optional(nonEmptyText),
      "gameId" -> text,
      "move" -> text
    )(SignupData.apply)(_ => None)
      .verifying(captchaFailMessage, validateCaptcha _))

    def websiteWithCaptcha = withCaptcha(website)

    val mobile = Form(mapping(
      "username" -> trimField(username),
      "password" -> trimField(password),
      "email" -> withAcceptableDns(acceptableUniqueEmail(none)),
      "level" -> text.verifying(FormSelect.Level.keySet.contains _)
    )(MobileSignupData.apply)(_ => None))
  }

  val passwordReset = Form(mapping(
    "email" -> anyEmail, // allow unacceptable emails for BC
    "gameId" -> text,
    "move" -> text
  )(PasswordReset.apply)(_ => None)
    .verifying(captchaFailMessage, validateCaptcha _))

  def passwordResetWithCaptcha = withCaptcha(passwordReset)

  val newPassword = Form(single(
    "password" -> trimField(signup.password)
  ))

  case class PasswordResetConfirm(newPasswd1: String, newPasswd2: String) {
    def samePasswords = newPasswd1 == newPasswd2
  }

  val passwdReset = Form(mapping(
    "newPasswd1" -> trimField(signup.password),
    "newPasswd2" -> trimField(signup.password)
  )(PasswordResetConfirm.apply)(PasswordResetConfirm.unapply).verifying(
      "the new passwords don't match",
      _.samePasswords
    ))

  def changeEmail(u: User, old: Option[EmailAddress]) = authenticator loginCandidate u map { candidate =>
    Form(mapping(
      "passwd" -> passwordMapping(candidate),
      "email" -> withAcceptableDns {
        acceptableUniqueEmail(candidate.user.some).verifying(emailValidator differentConstraint old)
      }
    )(ChangeEmail.apply)(ChangeEmail.unapply)).fill(ChangeEmail(
      passwd = "",
      email = old.??(_.value)
    ))
  }

  def cellphoneConfirm(user: User) = Form(mapping(
    SmsCaptcher.form.cellphone(user.id),
    SmsCaptcher.form.template,
    SmsCaptcher.form.code
  )(CellphoneData.apply)(CellphoneData.unapply)
    .verifying(smsCaptchaFailMessage, validSmsCaptcha(user.username, _) awaitSeconds 3)) fill (CellphoneData(
    cellphone = "",
    template = SmsTemplate.Confirm.code,
    code = ""
  ))

  def setupTwoFactor(u: User) = authenticator loginCandidate u map { candidate =>
    Form(mapping(
      "secret" -> nonEmptyText,
      "passwd" -> passwordMapping(candidate),
      "token" -> nonEmptyText
    )(TwoFactor.apply)(TwoFactor.unapply).verifying(
        "invalidAuthenticationCode",
        _.tokenValid
      )).fill(TwoFactor(
      secret = TotpSecret.random.base32,
      passwd = "",
      token = ""
    ))
  }

  def disableTwoFactor(u: User) = authenticator loginCandidate u map { candidate =>
    Form(tuple(
      "passwd" -> passwordMapping(candidate),
      "token" -> text.verifying("invalidAuthenticationCode", t => u.totpSecret.??(_.verify(TotpToken(t))))
    ))
  }

  def fixEmail(old: EmailAddress) = Form(
    single(
      "email" -> withAcceptableDns {
        acceptableUniqueEmail(none).verifying(emailValidator differentConstraint old.some)
      }
    )
  ).fill(old.value)

  def modEmail(user: User) = Form(single("email" -> acceptableUniqueEmail(user.some)))

  def modPassword = Form(single("password" -> trimField(signup.password)))

  def closeAccount(u: User) = authenticator loginCandidate u map { candidate =>
    Form(single("passwd" -> passwordMapping(candidate)))
  }

  private def passwordMapping(candidate: User.LoginCandidate) =
    text.verifying("incorrectPassword", p => candidate.check(ClearPassword(p)))

}

object DataForm {

  case class SignupData(
      username: String,
      password: String,
      email: String,
      level: String,
      fp: Option[String],
      `g-recaptcha-response`: Option[String],
      gameId: String,
      move: String
  ) {
    def recaptchaResponse = `g-recaptcha-response`

    def realEmail = EmailAddress(email)

    def fingerPrint = fp.filter(_.nonEmpty) map FingerPrint.apply
  }

  case class MobileSignupData(
      username: String,
      password: String,
      email: String,
      level: String
  ) {
    def realEmail = EmailAddress(email)
  }

  case class PasswordReset(
      email: String,
      gameId: String,
      move: String
  ) {
    def realEmail = EmailAddress(email)
  }

  case class ChangeEmail(passwd: String, email: String) {
    def realEmail = EmailAddress(email)
  }

  case class CellphoneData(cellphone: String, template: String, code: String) {
    def realCellphone = CellphoneAddress(cellphone)
  }

  case class TwoFactor(secret: String, passwd: String, token: String) {
    def tokenValid = TotpSecret(secret).verify(User.TotpToken(token))
  }

}

