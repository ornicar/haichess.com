package controllers

import akka.pattern.ask
import play.api.data._
import Forms._
import play.api.libs.json._
import play.api.mvc._
import lila.app._
import lila.api.Context
import lila.hub.actorApi.captcha.ValidCaptcha
import makeTimeout.large
import lila.common.{ CellphoneAddress, HTTPRequest, Region, SmsKey, SmsTemplate, UserCellphone }
import lila.hub.actorApi.smsCaptcha.SendSmsCaptcha
import lila.memo.SmsRateLimit
import lila.security.SmsCaptcher
import views._

object Main extends LilaController {

  private lazy val blindForm = Form(tuple(
    "enable" -> nonEmptyText,
    "redirect" -> nonEmptyText
  ))

  def toggleBlindMode = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    fuccess {
      blindForm.bindFromRequest.fold(
        err => BadRequest, {
          case (enable, redirect) =>
            Redirect(redirect) withCookies lila.common.LilaCookie.cookie(
              Env.api.Accessibility.blindCookieName,
              if (enable == "0") "" else Env.api.Accessibility.hash,
              maxAge = Env.api.Accessibility.blindCookieMaxAge.some,
              httpOnly = true.some
            )
        }
      )
    }
  }

  def websocket(apiVersion: Int) = SocketOption { implicit ctx =>
    getSocketSri("sri") ?? { sri =>
      Env.site.socketHandler.human(sri, ctx.userId, apiVersion, get("flag")) map some
    }
  }

  def apiWebsocket = WebSocket.tryAccept { req =>
    Env.site.socketHandler.api(lila.api.Mobile.Api.currentVersion) map Right.apply
  }

  def captchaCheck(id: String) = Open { implicit ctx =>
    Env.hub.captcher ? ValidCaptcha(id, ~get("solution")) map {
      case valid: Boolean => Ok(if (valid) 1 else 0)
    }
  }

  def webmasters = Open { implicit ctx =>
    pageHit
    fuccess {
      html.site.help.webmasters()
    }
  }

  def lag = Open { implicit ctx =>
    pageHit
    fuccess {
      html.site.lag()
    }
  }

  def mobile = Open { implicit ctx =>
    pageHit
    OptionOk(Prismic getBookmark "mobile-apk") {
      case (doc, resolver) => html.mobile(doc, resolver)
    }
  }

  def jslog(id: String) = Open { ctx =>
    Env.round.selfReport(
      userId = ctx.userId,
      ip = HTTPRequest lastRemoteAddress ctx.req,
      fullId = id,
      name = get("n", ctx.req) | "?"
    )
    NoContent.fuccess
  }

  /**
   * Event monitoring endpoint
   */
  def jsmon(event: String) = Action {
    if (event == "socket_gap") lila.mon.jsmon.socketGap()
    else lila.mon.jsmon.unknown()
    NoContent
  }

  private lazy val glyphsResult: Result = {
    import chess.format.pgn.Glyph
    import lila.tree.Node.glyphWriter
    Ok(Json.obj(
      "move" -> Glyph.MoveAssessment.display,
      "position" -> Glyph.PositionAssessment.display,
      "observation" -> Glyph.Observation.display
    )) as JSON
  }
  val glyphs = Action(glyphsResult)

  def image(id: String, hash: String, name: String) = Action.async { req =>
    Env.db.image.fetch(id) map {
      case None => NotFound
      case Some(image) =>
        //lila.log("image").info(s"Serving ${image.path} to ${HTTPRequest printClient req}")
        Ok(image.data).withHeaders(
          CONTENT_TYPE -> image.contentType.getOrElse("image/jpeg"),
          CONTENT_DISPOSITION -> image.name,
          CONTENT_LENGTH -> image.size.toString
        )
    }
  }

  def file(id: String, hash: String, name: String) = Action.async { req =>
    Env.db.file.fetch(id) map {
      case None => NotFound
      case Some(file) =>
        lila.log("file").info(s"Serving ${file.path} to ${HTTPRequest printClient req}")
        Ok(file.data).withHeaders(
          CONTENT_TYPE -> file.contentType.getOrElse("image/jpeg"),
          CONTENT_DISPOSITION -> file.name,
          CONTENT_LENGTH -> file.size.toString
        )
    }
  }

  val robots = Action { req =>
    Ok {
      if (Env.api.Net.Crawlable && req.domain == Env.api.Net.Domain) """User-agent: *
Allow: /
Disallow: /game/export
Disallow: /games/export
"""
      else "User-agent: *\nDisallow: /"
    }
  }

  def renderNotFound(req: RequestHeader): Fu[Result] =
    reqToCtx(req) map renderNotFound

  def renderNotFound(ctx: Context): Result = {
    lila.mon.http.response.code404()
    NotFound(html.base.notFound()(ctx))
  }

  def getFishnet = Open { implicit ctx =>
    Ok(html.site.bits.getFishnet()).fuccess
  }

  def costs = Open { implicit ctx =>
    Redirect("https://docs.google.com/spreadsheets/d/1CGgu-7aNxlZkjLl9l-OlL00fch06xp0Q7eCVDDakYEE/preview").fuccess
  }

  def contact = Open { implicit ctx =>
    Ok(html.site.contact()).fuccess
  }

  def faq = Open { implicit ctx =>
    Ok(html.site.faq()).fuccess
  }

  def legacyQa = Open { implicit ctx =>
    MovedPermanently(routes.Page.faq.url).fuccess
  }

  def legacyQaQuestion(id: Int, slug: String) = Open { implicit ctx =>
    MovedPermanently {
      val faq = routes.Page.faq.url
      id match {
        case 103 => s"$faq#acpl"
        case 258 => s"$faq#marks"
        case 13 => s"$faq#titles"
        case 87 => routes.Stat.ratingDistribution("blitz").url
        case 110 => s"$faq#name"
        case 29 => s"$faq#titles"
        case 4811 => s"$faq#lm"
        case 216 => routes.Main.mobile.url
        case 340 => s"$faq#trophies"
        case 6 => s"$faq#ratings"
        case 207 => s"$faq#hide-ratings"
        case 547 => s"$faq#leaving"
        case 259 => s"$faq#trophies"
        case 342 => s"$faq#provisional"
        case 50 => routes.Page.help.url
        case 46 => s"$faq#name"
        case 122 => s"$faq#marks"
        case _ => faq
      }
    }.fuccess
  }

  def versionedAsset(version: String, file: String) = Assets.at(path = "/public", file)

  def dummyPrismic = Open { implicit ctx =>
    val p = "{\"refs\":[],\"bookmarks\":{},\"types\":{\"do-dont\":\"Dos & Don'ts\",\"variant\":\"Variant\",\"article\":\"Article\",\"doc\":\"Documentation\",\"blog\":\"Blog post\"},\"languages\":[{\"id\":\"en-us\",\"name\":\"English - United States\"}],\"tags\":[],\"forms\":{},\"oauth_initiate\":\"\",\"oauth_token\":\"\",\"version\":\"4c266d1\",\"license\":\"\",\"experiments\":{\"draft\":[],\"running\":[]}}"
    Ok(Json.parse(p)).fuccess
  }

  def citys(province: String) = Auth { implicit ctx => me =>
    implicit val format = Json.format[Region.CaseCity]
    JsonOk(Region.City.caseCitys(province)).fuccess
  }

  def sendSmsCode(repeatValid: Int = 1) = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    Form(tuple(
      if (repeatValid == 1) { SmsCaptcher.form.cellphone(me.id) } else { SmsCaptcher.form.cellphone2 },
      SmsCaptcher.form.template
    )).bindFromRequest.fold(
      jsonFormError,
      data => data match {
        case (cellphone, template) => {
          SmsRateLimit.rateLimit(UserCellphone(me.username, CellphoneAddress(cellphone)), ctx.req) {
            Env.hub.smsCaptcher ! SendSmsCaptcha(SmsKey(me.username, SmsTemplate.byKey(template), CellphoneAddress(cellphone)))
            jsonOkResult.fuccess
          }
        }
      }
    ) map (_ as JSON)
  }

}
