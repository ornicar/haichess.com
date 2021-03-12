package controllers

import lila.app._
import lila.common.paginator.Paginator
import play.api.mvc.BodyParsers
import views._

object MemberOrder extends LilaController {

  private def env = Env.member

  def page(page: Int) = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    def searchForm = env.form.orderSearch
    searchForm.bindFromRequest.fold(
      err => {
        Ok(html.member.card.orders(Paginator.empty, err)).fuccess
      },
      data => env.orderApi.minePage(me.id, page, data) map { pager =>
        Ok(html.member.card.orders(pager, searchForm fill data))
      }
    )
  }

  def info(orderId: String) = Auth { implicit ctx => me =>
    OptionResult(env.orderApi.byId(orderId)) { o =>
      Ok(html.member.order(o))
    }
  }

  def toPay = AuthBody { implicit ctx => me =>
    Member.memberDiscounts flatMap { discounts =>
      implicit val req = ctx.body
      env.form.order(me, discounts).bindFromRequest.fold(
        err => {
          controllerLogger.error(s"Submit ERROR ${me.id}, order: " + errorsAsJson(err).toString)
          fuccess(BadRequest)
        },
        data => env.orderApi.toPay(data, me) map { body =>
          Ok(html.member.orderPayRedirect(body))
        }
      )
    }
  }

  def alipayReturn(orderId: String) = Open { implicit ctx =>
    OptionResult(env.orderApi.byId(orderId)) { o =>
      Ok(html.member.orderPayReturn(o))
    }
  }

  def alipayNotify = OpenBody(BodyParsers.parse.urlFormEncoded) { implicit ctx =>
    implicit def req = ctx.body
    def searchForm = env.form.orderAlipayNofify
    searchForm.bindFromRequest.fold(
      err => {
        controllerLogger.error(errorsAsJson(err).toString())
        Ok("error").fuccess
      },
      data => env.orderApi.handleAlipayNotify(data, ctx.body.body) map { _ =>
        Ok("success")
      }
    )
  }

}
