package controllers

import lila.api.Context
import lila.app._
import lila.member.{ JsonView, OrderDirector }
import views._

object Member extends LilaController {

  private def env = Env.member

  def intro = Auth { implicit ctx => me =>
    for {
      hasGoldCard <- env.memberCardApi.existsGoldCard(me.id)
      hasSilverCard <- env.memberCardApi.existsSilverCard(me.id)
    } yield Ok(html.member.intro(hasGoldCard, hasSilverCard))
  }

  def info = Auth { implicit ctx => me =>
    for {
      orders <- env.orderApi.mine(me.id)
      cards <- env.memberCardApi.mine(me.id)
      levelChangeLogs <- env.memberLevelLogApi.mine(me.id)
      levelPointsLogs <- env.memberPointsLogApi.mine(me.id)
    } yield Ok(html.member.info(me, orders, cards, levelChangeLogs, levelPointsLogs))
  }

  def toBuy(level: Option[String]) = Auth { implicit ctx => me =>
    memberDiscounts map { discounts =>
      Ok(html.member.buy(me, env.form.order(me, discounts), discounts, level))
    }
  }

  def calcPrice = AuthBody { implicit ctx => me =>
    memberDiscounts flatMap { discounts =>
      implicit val req = ctx.body
      env.form.calcPrice(me, discounts).bindFromRequest.fold(
        jsonFormError,
        data => {
          val res = OrderDirector.calcPrice(data, me)
          Ok(JsonView.priceJson(res)) as JSON
        }.fuccess
      )
    }
  }

  private[controllers] def memberDiscounts(implicit ctx: Context) = {
    Env.team.api.mineCertifyTeam(ctx.me) flatMap { teams =>
      if (teams.isEmpty) {
        Env.coach.studentApi.mineCertifyCoach(ctx.me) map { users =>
          users.map { u =>
            u.id -> u.username
          }
        }
      } else {
        fuccess {
          teams.map { t =>
            t.createdBy -> t.name
          }
        }
      }
    }
  }

}
