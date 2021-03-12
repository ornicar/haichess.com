package lila.security

import scala.concurrent.duration.{ span => _, _ }
import akka.actor.ActorSystem
import com.aliyuncs.http.MethodType
import com.aliyuncs.{ CommonRequest, DefaultAcsClient }
import lila.common.{ CellphoneAddress, SmsTemplate }
import com.aliyuncs.profile.DefaultProfile

final class Smsgun(
    apiUrl: String,
    apiKey: String,
    apiSecret: String,
    system: ActorSystem
) {

  private val profile = DefaultProfile.getProfile("cn-hangzhou", apiKey, apiSecret)
  private val iacsClient = new DefaultAcsClient(profile)

  def send(msg: Smsgun.Message)(success: => Unit)(fail: => Unit): Unit = {
    val request = new CommonRequest()
    request.setMethod(MethodType.POST)
    request.setDomain("dysmsapi.aliyuncs.com")
    request.setVersion("2017-05-25")
    request.setAction("SendSms")
    request.putQueryParameter("RegionId", "cn-hangzhou")
    request.putQueryParameter("SignName", "嗨棋科技")
    request.putQueryParameter("PhoneNumbers", msg.to.value)
    request.putQueryParameter("TemplateCode", msg.template.code)
    request.putQueryParameter("TemplateParam", msg.param)

    try {
      iacsClient.getCommonResponse(request)
      success
    } catch {
      case e: Exception => {
        if (msg.retriesLeft > 0) {
          system.scheduler.scheduleOnce(10 seconds) {
            send(msg.copy(retriesLeft = msg.retriesLeft - 1))(success)(fail)
          }
        } else {
          logger.error(s"send sms error ${msg.to.value}: ${e.getMessage}")
          fail
        }
      }
    }
  }
}

object Smsgun {

  case class Message(
      to: CellphoneAddress,
      template: SmsTemplate,
      code: String,
      retriesLeft: Int = 3
  ) {
    val param = s"{'code':'$code'}"
  }

}
