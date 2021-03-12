package lila.common

case class SmsKey(username: String, template: SmsTemplate, to: CellphoneAddress)
case class UserCellphone(username: String, cellphone: CellphoneAddress)

sealed abstract class SmsTemplate(val code: String, val name: String)
object SmsTemplate {

  case object Login extends SmsTemplate("SMS_184990184", "登录确认验证码")
  case object Confirm extends SmsTemplate("SMS_198917160", "身份验证验证码")
  val all = List(Login, Confirm)
  def keys = all.map(_.code).toSet
  def byKey(code: String) = all.find(_.code == code) err s"Bad SmsTemplate $code"
}