package controllers

import lila.app._
import play.api.mvc._
import play.api.libs.json._
import lila.common.IpAddress
import scala.concurrent.duration._
import lila.api.Context
import views._

object Home extends LilaController {

  def home = Open { implicit ctx =>
    negotiate(
      html = renderHome(Results.Ok).map(NoCache),
      api = _ => fuccess {
        val expiration = 60 * 60 * 24 * 7 // set to one hour, one week before changing the pool config
        Ok(Json.obj()).withHeaders(CACHE_CONTROL -> s"max-age=$expiration")
      }
    )
  }

  def handleStatus(req: RequestHeader, status: Results.Status): Fu[Result] =
    reqToCtx(req) flatMap { ctx => renderHome(status)(ctx) }

  def renderHome(status: Results.Status)(implicit ctx: Context): Fu[Result] = {
    Env.current.preloader.home map (views.html.home.apply _).tupled dmap { html =>
      ensureSessionId(ctx.req)(status(html))
    }
  }.mon(_.http.response.home)

  private val MessageLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 40,
    duration = 10 seconds,
    name = "home socket message per IP",
    key = "home_socket.message.ip",
    enforce = Env.api.Net.RateLimit
  )

  def socket(apiVersion: Int) = SocketOptionLimited[JsValue](MessageLimitPerIP, "home") { implicit ctx =>
    getSocketSri("sri") ?? { sri =>
      Env.home.socketHandler(sri, user = ctx.me, mobile = getBool("mobile"), apiVersion) map some
    }
  }

}
