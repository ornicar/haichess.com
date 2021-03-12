package lila.coach

import akka.actor.ActorSelection
import lila.common.{ Region, SmsTemplate }
import lila.security.SmsCaptcher
import lila.user.User
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

final class CoachCertifyForm(val smsCaptcherActorSelection: akka.actor.ActorSelection) extends lila.hub.SmsCaptchedForm {

  override def smsCaptcha: ActorSelection = smsCaptcherActorSelection

  def certifyOf(coachWithUser: Option[Coach.WithUser], user: User) = coachWithUser.fold {
    certify(user) fill CertifyData(
      realName = user.realNameOrUsername,
      certNo = "",
      province = user.profileOrDefault.province | "",
      city = user.profileOrDefault.city | "",
      cellphone = user.cellphone.map(_.value) | "",
      template = SmsTemplate.Confirm.code,
      code = ""
    )
  } { c =>
    certify(user) fill CertifyData(
      realName = user.realNameOrUsername,
      certNo = c.certify.certNo,
      province = user.profileOrDefault.province | "",
      city = user.profileOrDefault.city | "",
      cellphone = user.cellphone.map(_.value) | "",
      template = SmsTemplate.Confirm.code,
      code = ""
    )
  }

  def certify(user: User) = Form(mapping(
    "realName" -> nonEmptyText(minLength = 2, maxLength = 20),
    "certNo" -> certNo,
    "province" -> nonEmptyText.verifying(Region.Province.keySet contains _),
    "city" -> nonEmptyText.verifying(Region.City.keySet contains _),
    SmsCaptcher.form.cellphone(user.id),
    SmsCaptcher.form.template,
    SmsCaptcher.form.code
  )(CertifyData.apply)(CertifyData.unapply)
    .verifying(smsCaptchaFailMessage, d => if (user.cellphone.isEmpty) { validSmsCaptcha(user.username, d) awaitSeconds 5 } else true))

  lazy val certNo = nonEmptyText.verifying(
    Constraints.pattern(
      regex = """[1-9]\d{7}((0\d)|(1[0-2]))(([0|1|2]\d)|3[0-1])\d{3}$|^[1-9]\d{5}[1-9]\d{3}((0\d)|(1[0-2]))(([0|1|2]\d)|3[0-1])\d{3}([0-9]|X)""".r,
      error = "身份证号格式错误"
    )
  )

}

case class CertifyData(
    realName: String,
    certNo: String,
    province: String,
    city: String,
    cellphone: String,
    template: String,
    code: String
)