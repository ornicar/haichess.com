package lila.coach

import java.net.URLEncoder
import com.alipay.easysdk.factory.Factory.Member
import com.alipay.easysdk.kernel.Config
import com.alipay.easysdk.kernel.util.ResponseChecker
import com.alipay.easysdk.member.identification.models.{ IdentityParam, MerchantConfig }
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import lila.notify.{ CoachApply, CoachApproved, Notification, NotifyApi }
import lila.db.dsl._
import lila.hub.actorApi.socket.SendTo
import lila.socket.Socket.makeMessage
import lila.user.{ Profile, User, UserRepo }
import org.joda.time.DateTime
import play.api.libs.json.Json
import scala.concurrent.duration._

final class CoachCertifyApi(
    bus: lila.common.Bus,
    coachColl: Coll,
    notifyApi: NotifyApi,
    adminUid: String
) {

  import BsonHandlers._

  case class IdWithUrl(certifyId: String, url: String)

  /**
   * 1、certifyId未认证时，或认证失败，有效期是23小时；
   * 2、认证成功后，用certifyId查询认证结果的有效期是3个月。
   */
  private val certifyCache: Cache[User.ID, IdWithUrl] = Scaffeine()
    .expireAfterWrite(23 hours)
    .build[User.ID, IdWithUrl]

  def certifyPerson(user: User, data: CertifyData): Funit = {
    UserRepo.setProfile(user.id, Profile(
      realName = data.realName.some,
      province = data.province.some,
      city = data.city.some
    )) >> {
      if (user.cellphone.isEmpty) {
        UserRepo.setCellphone(user.id, data.cellphone)
      } else funit
    } >> alipayCertify(user, data)
  }

  def alipayCertify(user: User, data: CertifyData): Funit = {
    val response = Member.Identification()
      .init(
        generateOrderNo,
        "CERT_PHOTO",
        new IdentityParam()
          .setIdentityType("CERT_INFO")
          .setCertType("IDENTITY_CARD")
          .setCertName(data.realName)
          .setCertNo(data.certNo),
        new MerchantConfig()
          .setReturnUrl(s"""alipays://platformapi/startapp?appId=20000067&url=${URLEncoder.encode(s"https://haichess.com/coach/certify/callback?id=${user.id}", "UTF-8")}""")
      )

    if (ResponseChecker.success(response)) {
      val certifyId = response.getCertifyId
      val response2 = Member.Identification().certify(certifyId)
      if (ResponseChecker.success(response2)) {
        coachColl.update(
          $id(user.id),
          Coach.make(
            user = user,
            certify = Certify(
              certNo = data.certNo
            )
          ),
          upsert = true
        ) map { _ =>
            certifyCache.put(user.id, IdWithUrl(certifyId, response2.body))
          }
      } else fufail(s"certify failed $certifyId")
    } else {
      fufail(s"${response.msg} ${response.subMsg}")
    }
  }

  def certifyPersonUrl(id: User.ID): Option[String] = {
    certifyCache.getIfPresent(id).map(_.url)
  }

  def certifyPersonCallback(id: User.ID): Fu[Boolean] = {
    val certify = certifyCache.getIfPresent(id)
    certify.fold(fuccess(false)) { c =>
      val response = Member.Identification().query(c.certifyId)
      if (ResponseChecker.success(response)) {
        val passed = response.getPassed
        if (passed == "T") {
          coachColl.update(
            $id(id),
            $set("certify.status" -> Certify.Status.Passed.id)
          ).inject(true)
        } else fuccess(false)
      } else {
        fufail(s"${response.msg} ${response.subMsg}")
      }
    }
  } addEffect { p =>
    certifyCache.invalidate(id)
    callbackPush(id, p)
  }

  def callbackPush(userId: User.ID, passed: Boolean) =
    bus.publish(SendTo(userId, makeMessage(
      "coachCertifyPassed",
      Json.obj("passed" -> passed)
    )), 'socketUsers)

  def certifyQualify(user: User): Funit = {
    coachColl.update(
      $id(user.id),
      $set(
        "certify.status" -> Certify.Status.Applying.id,
        "certify.applyAt" -> DateTime.now
      )
    ) map { result =>
        if (result.n > 0) {
          notifyApi.addNotification(
            Notification.make(Notification.Notifies(adminUid), CoachApply(user.id))
          )
        }
      }
  }

  def toggleQualifyApproved(username: String, value: Boolean): Fu[String] = {
    val status = if (value) Certify.Status.Approved else Certify.Status.Rejected
    val userId = User.normalize(username)
    coachColl.update(
      $id(userId),
      $set(
        "certify.status" -> status.id
      ) ++ value ?? $set(
          "certify.approvedAt" -> DateTime.now
        )
    ) map { result =>
        if (result.n > 0) {
          notifyApi.addNotification(
            Notification.make(Notification.Notifies(userId), CoachApproved(userId, status.id))
          ) >>- bus.publish(lila.hub.actorApi.coach.Certify(userId, value), 'coachCertify)
          "Done!"
        } else "No such coach"
      }
  }

  private def generateOrderNo = s"HC${DateTime.now.toString("yyyyMMddHHmmssSSS")}"

}
