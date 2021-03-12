package lila.security

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import lila.common.{ CellphoneAddress, SmsKey, SmsTemplate }
import lila.hub.actorApi.smsCaptcha._
import lila.user.UserRepo
import play.api.data.validation.Constraints
import scala.concurrent.duration._

case class SmsCaptcher(smsgun: Smsgun) extends Actor {

  def receive = {

    case SendSmsCaptcha(key: SmsKey) => sender ! sendCode(key)

    case ValidSmsCaptcha(key: SmsKey, code: String) => sender ! validCode(key, code)
  }

  private val codeCache: Cache[SmsKey, String] =
    Scaffeine().expireAfterWrite(3 minutes).build[SmsKey, String]

  def makeCode = ((Math.random() * 9 + 1) * 100000).toInt.toString

  def putCode(key: SmsKey, code: String) = codeCache.put(key, code)

  def getCode(key: SmsKey): Option[String] = codeCache.getIfPresent(key)

  def cleanCode(key: SmsKey) = codeCache.invalidate(key)

  def validCode(key: SmsKey, code: String) = getCode(key) match {
    case None => false
    case Some(c) => c == code
  }

  def sendCode(key: SmsKey): Unit = {
    val code = makeCode
    smsgun.send(Smsgun.Message(
      key.to,
      key.template,
      code
    ))(success = putCode(key, code))(fail = cleanCode(key))
  }

}

object SmsCaptcher {

  object form {

    import play.api.data.Forms._
    def cellphone(userId: String) = "cellphone" -> cellphoneConstraints(userId)
    def cellphone2 = "cellphone" -> nonEmptyText.verifying(Constraints.pattern(regex = CellphoneAddress.regex, error = "手机号格式错误"))
    def template = "template" -> nonEmptyText.verifying("非法模板", SmsTemplate.keys.contains(_))
    def code = "code" -> nonEmptyText(minLength = 6, maxLength = 6).verifying(Constraints.pattern(regex = """^\d{6}$""".r, error = "验证码格式错误"))
    def cellphoneConstraints(userId: String) = nonEmptyText.verifying(Constraints.pattern(regex = CellphoneAddress.regex, error = "手机号格式错误")).verifying("手机号已被使用", !cellphoneExits(userId, _))
    def cellphoneExits(userId: String, cellphone: String) = UserRepo cellphoneExits (userId, cellphone) awaitSeconds 3
  }
}

