package controllers

import lila.app._
import lila.api.Context
import lila.security.Permission
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import lila.common.paginator.Paginator
import lila.member.{ JsonView, OrderDirector, MemberCard => MemberCardMode }
import views._

object MemberCard extends LilaController {

  private def env = Env.member

  def page(page: Int, status: Option[String], level: Option[String]) = Auth { implicit ctx => me =>
    Permiss {
      val st = status.map(s => MemberCardMode.CardStatus(s))
      val lv = level.map(l => lila.user.MemberLevel(l))
      env.memberCardApi.minePage(me.id, page, st, lv) map { pager =>
        Ok(html.member.card.mine(pager, st, lv))
      }
    }
  }

  def givingLogPage(page: Int) = AuthBody { implicit ctx => me =>
    Permiss {
      implicit def req = ctx.body
      def searchForm = env.form.cardLogSearch
      searchForm.bindFromRequest.fold(
        err => {
          Ok(html.member.card.givingLogs(Paginator.empty, err)).fuccess
        },
        data => env.memberCardLogApi.minePage(me.id, page, data) map { pager =>
          Ok(html.member.card.givingLogs(pager, searchForm fill data))
        }
      )
    }
  }

  def toBuy = Auth { implicit ctx => me =>
    Permiss {
      Ok(html.member.card.buy(me, env.form.order(me, List.empty))).fuccess
    }
  }

  def calcPrice = AuthBody { implicit ctx => me =>
    Permiss {
      implicit val req = ctx.body
      env.form.calcPrice(me, List.empty).bindFromRequest.fold(
        jsonFormError,
        data => {
          val res = OrderDirector.calcPrice(data, me)
          Ok(JsonView.priceJson(res)) as JSON
        }.fuccess
      )
    }
  }

  def givingForm(id: String) = Auth { implicit ctx => me =>
    Permiss {
      OptionResult(env.memberCardApi.byId(id)) { card =>
        Ok(html.member.card.giving(card))
      }
    }
  }

  def validUsername = AuthBody { implicit ctx => me =>
    Permiss {
      implicit val req = ctx.body
      Form(single(
        "username" -> lila.user.DataForm.historicalUsernameField.verifying("必须是您的学员", canGive _)
      )).bindFromRequest.fold(
        err => jsonFormError(err),
        _ => fuccess(jsonOkResult)
      )
    }
  }

  def giving(id: String) = AuthBody { implicit ctx => me =>
    Permiss {
      OptionFuResult(env.memberCardApi.byId(id)) { card =>
        if (card.userId != me.id || !card.isAvailable) {
          Forbidden(views.html.site.message.authFailed).fuccess
        } else {
          implicit val req = ctx.body
          Form(single(
            "username" -> lila.user.DataForm.historicalUsernameField.verifying("必须是您的学员", canGive _)
          )).bindFromRequest.fold(
            err => fuccess(BadRequest(err.toString)),
            username => env.memberCardApi.give(card, lila.user.User.normalize(username)) inject Redirect(routes.MemberCard.page(1, none, none))
          )
        }
      }
    }
  }

  def use(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(env.memberCardApi.byId(id)) { card =>
      if (card.userId != me.id || !card.isAvailable) {
        Forbidden(views.html.site.message.authFailed).fuccess
      } else {
        env.memberCardApi.use(me, card) inject Redirect(routes.Member.info)
      }
    }
  }

  private def canGive(username: String)(implicit ctx: lila.api.Context): Boolean = {
    for {
      students <- Env.coach.studentApi.mineStudents(ctx.me)
      teamMembers <- Env.team.api.mineMembers(ctx.me)
    } yield {
      val userId = lila.user.User.normalize(username)
      students.contains(userId) || teamMembers.contains(userId)
    }
  } awaitSeconds 3

  private def Permiss(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (isGranted(Permission.Coach) || isGranted(Permission.Team)) f
    else Forbidden(views.html.site.message.authFailed).fuccess
  }

}
