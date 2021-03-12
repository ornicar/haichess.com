package lila.hub

import akka.pattern.ask
import lila.common.{ CellphoneAddress, SmsKey, SmsTemplate }
import lila.hub.actorApi.smsCaptcha.ValidSmsCaptcha

trait SmsCaptchedForm {

  import makeTimeout.large

  type SmsCaptchedData = {
    def cellphone: String
    def template: String
    def code: String
  }

  def smsCaptcha: akka.actor.ActorSelection

  def validSmsCaptcha(username: String, data: SmsCaptchedData): Fu[Boolean] = {
    val key = SmsKey(
      username,
      SmsTemplate.byKey(data.template),
      CellphoneAddress(data.cellphone)
    )
    (smsCaptcha ? ValidSmsCaptcha(key, data.code)).mapTo[Boolean]
  }

  def smsCaptchaFailMessage = "验证码错误"

}
