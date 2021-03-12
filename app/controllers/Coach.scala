package controllers

import lila.app._
import lila.app.Env
import lila.api.Context
import lila.coach.{ Certify, CoachProfileForm, Coach => CoachModel }
import lila.common.{ HTTPRequest, IpAddress }
import lila.memo.{ RateLimit, UploadRateLimit }
import ornicar.scalalib.{ Random, Zero }
import play.api.mvc.{ BodyParsers, RequestHeader, Result, Results }
import scala.concurrent.duration._
import play.api.data._
import play.api.libs.json.Json
import views._

object Coach extends LilaController {

  private def api = Env.coach.api
  private def certifyApi = Env.coach.certifyApi
  private def certifyForm = Env.coach.certifyForm
  private def studentApi = Env.coach.studentApi

  def showById(coachId: String) = Open { implicit ctx =>
    OptionFuResult(api byId2 CoachModel.Id(coachId)) { c =>
      ctx.me.??(u => studentApi.byIds(c.coach.id.value, u.id)) map { student =>
        Ok(html.coach.show(c, student))
      }
    }
  }

  def show(username: String) = Open { implicit ctx =>
    OptionFuResult(api find username) { c =>
      ctx.me.??(u => studentApi.byIds(c.coach.id.value, u.id)) map { student =>
        Ok(html.coach.show(c, student))
      }
    }
  }

  def certify = Auth { implicit ctx => me =>
    api.findNoGranter(me) map { c =>
      Ok(
        html.coach.certify(
          certifyForm.certifyOf(c, me),
          c,
          certifyApi.certifyPersonUrl(me.id)
        )
      )
    }
  }

  def certifyPerson = AuthBody { implicit ctx => me =>
    rateLimit(me.username, ctx.req) {
      implicit val req = ctx.body
      val form = certifyForm.certify(me).bindFromRequest
      api.findNoGranter(me) flatMap {
        case Some(c) => c.certify.status match {
          case None => sendCertifyPerson(c.some, form, me)
          case Some(_) => Forbidden("Can not apply certify status").fuccess
        }
        case _ => sendCertifyPerson(none, form, me)
      }
    }
  }

  private def sendCertifyPerson(c: Option[CoachModel.WithUser], form: Form[lila.coach.CertifyData], u: lila.user.User)(implicit ctx: Context): Fu[Result] = {
    form.fold(
      err => BadRequest(
        html.coach.certify(
          err,
          c,
          certifyApi.certifyPersonUrl(u.id)
        )
      ).fuccess,
      data => certifyApi.certifyPerson(u, data) inject Redirect(routes.Coach.certify)
    )
  }

  def certifyQualify = AuthBody { implicit ctx => me =>
    api.findNoGranter(me) flatMap {
      case Some(c) => {
        c.certify.status match {
          case None => Forbidden("Can not apply certify status").fuccess
          case Some(status) => status match {
            case Certify.Status.Passed | Certify.Status.Rejected => certifyApi.certifyQualify(me) inject Redirect(routes.Coach.certify)
            case Certify.Status.Applying | Certify.Status.Approved => Forbidden("Can not apply certify status").fuccess
          }
        }
      }
      case _ => certifyApi.certifyQualify(me) inject Redirect(routes.Coach.certify)
    }
  }

  def certifyPersonCallback(id: String) = Open { implicit ctx =>
    certifyApi.certifyPersonCallback(id) map { passed =>
      Ok(html.coach.certify.certifyPersonCallback(passed))
    }
  }

  def edit = Secure(_.Coach) { implicit ctx => me =>
    OptionResult(api findNoGranter me) { c =>
      NoCache {
        Ok(html.coach.edit(c, CoachProfileForm edit c.coach))
      }
    }
  }

  def editApply = SecureBody(_.Coach) { implicit ctx => me =>
    OptionFuResult(api findNoGranter me) { c =>
      implicit val req = ctx.body
      CoachProfileForm.edit(c.coach).bindFromRequest.fold(
        _ => fuccess(BadRequest),
        data => api.update(c, data) inject Redirect(routes.Coach.show(me.username))
      )
    }
  }

  def uploadPicture = AuthBody(BodyParsers.parse.multipartFormData) { implicit ctx => implicit me =>
    UploadRateLimit.rateLimit(me.username, ctx.req) {
      val picture = ctx.body.body.file("file")
      picture match {
        case Some(pic) => api.uploadPicture(Random nextString 16, pic) map { image =>
          Ok(Json.obj("ok" -> true, "path" -> image.path))
        } recover {
          case e: lila.base.LilaException => Ok(Json.obj("ok" -> false, "message" -> e.message))
        }
        case _ => fuccess(Ok(Json.obj("ok" -> true)))
      }
    }
  }

  def applyingStuList = Secure(_.Coach) { implicit ctx => me =>
    studentApi.applyingList(me.id) map { list =>
      Ok(html.coach.student.applying(list))
    }
  }

  def approvedStuList(q: String = "") = Secure(_.Coach) { implicit ctx => me =>
    studentApi.approvedList(me.id, q) map { list =>
      Ok(html.coach.student.approved(list, q.trim))
    }
  }

  def studentApply(coachId: String) = Auth { implicit ctx => me =>
    OptionFuResult(api byId2 CoachModel.Id(coachId)) { _ =>
      studentApi.addOrReAdd(coachId, me.id) inject jsonOkResult
    }
  }

  def studentApprove(id: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(for {
      studentOption ← studentApi.byId(id)
      coachOption ← studentOption.??(_ => api byId CoachModel.Id(me.id))
    } yield (coachOption |@| studentOption).tupled) {
      case (coach, student) => {
        if (student.coachId == coach.id.value) {
          import lila.coach.Student.Status._
          student.status match {
            case Applying => studentApi.approve(id, student.coachId, student.studentId) inject Redirect(routes.Coach.applyingStuList)
            case Approved | Decline => Redirect(routes.Coach.applyingStuList).fuccess
          }
        } else Redirect(routes.Coach.applyingStuList).fuccess
      }
    }
  }

  def studentDecline(id: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(for {
      studentOption ← studentApi.byId(id)
      coachOption ← studentOption.??(_ => api byId CoachModel.Id(me.id))
    } yield (coachOption |@| studentOption).tupled) {
      case (coach, student) => {
        if (student.coachId == coach.id.value) {
          import lila.coach.Student.Status._
          student.status match {
            case Applying => studentApi.decline(id) inject Redirect(routes.Coach.applyingStuList)
            case Approved | Decline => Redirect(routes.Coach.applyingStuList).fuccess
          }
        } else Redirect(routes.Coach.applyingStuList).fuccess
      }
    }
  }

  def studentRemove(id: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(for {
      studentOption ← studentApi.byId(id)
      coachOption ← studentOption.??(_ => api byId CoachModel.Id(me.id))
    } yield (coachOption |@| studentOption).tupled) {
      case (coach, student) => {
        if (student.coachId == coach.id.value) {
          import lila.coach.Student.Status._
          student.status match {
            case Approved => studentApi.remove(id) inject Redirect(routes.Coach.approvedStuList())
            case Applying | Decline => Redirect(routes.Coach.approvedStuList()).fuccess
          }
        } else Redirect(routes.Coach.approvedStuList()).fuccess
      }
    }
  }

  def modList(page: Int, s: String) = Secure(_.ChangePermission) { implicit ctx => me =>
    pageHit
    val status = Certify.Status(s)
    Env.coach.pager(page, status) map { pager =>
      Ok(html.coach.mod(pager, status))
    }
  }

  def modDetail(id: String) = Secure(_.ChangePermission) { implicit ctx => me =>
    OptionResult(api byId2 CoachModel.Id(id)) { c =>
      Ok(html.coach.mod.detail(c))
    }
  }

  def modRejected(id: String) = Secure(_.ChangePermission) { implicit ctx => me =>
    OptionFuResult(api byId2 CoachModel.Id(id)) { c =>
      if (!c.certify.applying) {
        Ok(html.coach.mod.detail(c, "申请状态异常".some)).fuccess
      } else {
        certifyApi.toggleQualifyApproved(c.user.username, false) inject Redirect(routes.Coach.modList(1, "applying"))
      }
    }
  }

  private[controllers] val rateLimitGlobal = new lila.memo.RateLimit[String](
    credits = 100,
    duration = 1 day,
    name = "coach.certify global",
    key = "coach.certify.global"
  )

  private[controllers] lazy val rateLimitPerIP = new RateLimit[IpAddress](
    credits = 3,
    duration = 1 day,
    name = "coach.certify per IP",
    key = "coach.certify.ip"
  )

  private[controllers] lazy val rateLimitPerUser = new RateLimit[String](
    credits = 3,
    duration = 1 day,
    name = "coach.certify per user",
    key = "coach.certify.user"
  )

  private[controllers] def rateLimit(username: String, req: RequestHeader)(run: => Fu[Result]): Fu[Result] = {
    implicit val limitedDefault = Zero.instance[Fu[Result]](fuccess(Results.TooManyRequest("请求过于频繁，请明日再试")))
    rateLimitGlobal("-", cost = 1) {
      rateLimitPerUser(username, cost = 1) {
        rateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = 1) {
          run
        }
      }
    }
  }
}
