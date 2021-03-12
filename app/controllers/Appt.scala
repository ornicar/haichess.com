package controllers

import lila.app.Env
import lila.app._
import lila.appt.DataForm
import lila.contest.{ BoardRepo, Contest => ContestModel }
import org.joda.time.DateTime
import play.api.mvc.Result
import lila.api.Context
import lila.common.Form.futureDateTime
import play.api.data.Forms._
import play.api.data.Form
import views._

object Appt extends LilaController {

  def api = Env.appt.api
  def jsonView = Env.appt.jsonView

  def mine = Auth { implicit ctx => me =>
    api.home(me.id) map { appts =>
      Ok(jsonView.apptsJson(appts))
    } map (_ as JSON)
  }

  def page(page: Int) = Auth { implicit ctx => me =>
    api.page(me.id, page) map { pager =>
      Ok(views.html.appt.list(pager))
    }
  }

  def acceptXhr(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { appt =>
      val color = appt.userColor(me.id) err "can not find user"
      if (appt.currentTime.isBeforeNow) {
        BadRequest(jsonError("预约时间需大于当前时间")).fuccess
      } else {
        if (appt.maxDateTime.isBeforeNow) {
          BadRequest(jsonError("预约已经过期")).fuccess
        } else {
          if (appt.isContest && appt.currentTime.isBefore(DateTime.now.plusMinutes(lila.contest.Round.beforeStartMinutes - 1))) {
            BadRequest(jsonError(s"预约时间需大于当前时间（+${lila.contest.Round.beforeStartMinutes}分钟）")).fuccess
          } else api.update(appt.withRecordConfirmed(color), appt.currentRecord, me.id) inject jsonOkResult
        }
      }
    } map (_ as JSON)
  }

  def accept(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { appt =>
      appt.userColor(me.id).fold(notFound) { color =>
        if (appt.currentTime.isBeforeNow) {
          ForbiddenResult
        } else {
          if (appt.maxDateTime.isBeforeNow) {
            ForbiddenResult
          } else {
            if (appt.isContest && appt.currentTime.isBefore(DateTime.now.plusMinutes(lila.contest.Round.beforeStartMinutes - 1))) {
              ForbiddenResult
            } else api.update(appt.withRecordConfirmed(color), appt.currentRecord, me.id) inject Redirect(routes.Appt.page(1))
          }
        }
      }
    }
  }

  def form(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { appt =>
      Ok(views.html.appt.form(DataForm.add(appt), appt)).fuccess
    }
  }

  def create(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { appt =>
      implicit def req = ctx.body
      DataForm.add(appt).bindFromRequest.fold(
        fail => Ok(views.html.appt.form(fail, appt)).fuccess,
        data => {
          val color = appt.userColor(me.id) err "can not find user"
          val newAppt = appt.withAddRecord(color, data.time, data.message, me.id)
          api.update(newAppt, appt.currentRecord, me.id) inject Redirect(routes.Appt.form(id))
        }
      )
    }
  }

  private def OwnerJson(contest: ContestModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (ctx.me.??(me => contest.isCreator(me) || isGranted(_.ManageContest))) f
    else ForbiddenJsonResult
  }

  def ForbiddenJsonResult(implicit ctx: Context) = fuccess(Forbidden(jsonError("Forbidden")) as JSON)

  def ForbiddenResult(implicit ctx: Context) = Forbidden(views.html.site.message.authFailed).fuccess

}
