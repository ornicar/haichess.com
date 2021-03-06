package controllers

import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{ Result, Results }
import scala.concurrent.duration._

import chess.format.FEN
import lila.api.{ Context, BodyContext }
import lila.app._
import lila.common.{ HTTPRequest, LilaCookie, IpAddress }
import lila.game.{ GameRepo, Pov, AnonCookie }
import lila.setup.Processor.HookResult
import lila.setup.ValidFen
import lila.socket.Socket.Sri
import lila.user.UserRepo
import views._

object Setup extends LilaController with TheftPrevention {

  private def env = Env.setup

  private[controllers] val PostRateLimit = new lila.memo.RateLimit[IpAddress](5, 1 minute,
    name = "setup post",
    key = "setup_post",
    enforce = Env.api.Net.RateLimit)

  def aiForm = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req) {
      env.forms aiFilled get("fen").map(FEN) map { form =>
        html.setup.forms.ai(
          form,
          Env.fishnet.aiPerfApi.intRatings,
          form("fen").value flatMap ValidFen(getBool("strict"))
        )
      }
    } else fuccess {
      Redirect(routes.Lobby.home + "#ai")
    }
  }

  def ai = process(env.forms.ai) { config => implicit ctx =>
    env.processor ai config
  }

  def friendForm(userId: Option[String]) = Open { implicit ctx =>
    if (HTTPRequest isXhr ctx.req)
      env.forms friendFilled (get("fen").map(FEN), get("limit").map(_.toInt), get("increment").map(_.toInt)) flatMap { form =>
        val validFen = form("fen").value flatMap ValidFen(false)
        val appt = get("appt") match {
          case None => true
          case Some(_) => false
        }
        userId ?? UserRepo.named flatMap {
          case None => Ok(html.setup.forms.friend(form, none, none, validFen, appt)).fuccess
          case Some(user) => Env.challenge.granter(ctx.me, user, none) map {
            case Some(denied) => BadRequest(lila.challenge.ChallengeDenied.translated(denied))
            case None => Ok(html.setup.forms.friend(form, user.some, none, validFen, appt))
          }
        }
      }
    else fuccess {
      Redirect(routes.Lobby.home + "#friend")
    }
  }

  def friend(userId: Option[String]) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
      env.forms.friend(ctx).bindFromRequest.fold(
        err => negotiate(
          html = Lobby.renderHome(Results.BadRequest),
          api = _ => jsonFormError(err)
        ),
        config => userId ?? UserRepo.enabledById flatMap { destUser =>
          destUser ?? { Env.challenge.granter(ctx.me, _, config.perfType) } flatMap {
            case Some(denied) =>
              val message = lila.challenge.ChallengeDenied.translated(denied)
              negotiate(
                html = BadRequest(html.site.message.challengeDenied(message)).fuccess,
                api = _ => BadRequest(jsonError(message)).fuccess
              )
            case None =>
              import lila.challenge.Challenge._
              val challenge = lila.challenge.Challenge.make(
                variant = config.variant,
                initialFen = config.fen,
                timeControl = config.makeClock map { c =>
                  TimeControl.Clock(c)
                } orElse config.makeDaysPerTurn.map {
                  TimeControl.Correspondence.apply
                } getOrElse TimeControl.Unlimited,
                mode = config.mode,
                appt = config.appt,
                apptStartsAt = config.apptStartsAt,
                apptMessage = config.apptMessage,
                color = config.color.name,
                challenger = (ctx.me, HTTPRequest sid req) match {
                  case (Some(user), _) => Right(user)
                  case (_, Some(sid)) => Left(sid)
                  case _ => Left("no_sid")
                },
                destUser = destUser,
                rematchOf = none
              )
              env.processor.saveFriendConfig(config) >>
                (Env.challenge.api create challenge) flatMap {
                  case true => negotiate(
                    html = fuccess(Redirect(routes.Round.watcher(challenge.id, "white"))),
                    api = _ => Challenge showChallenge challenge
                  )
                  case false => negotiate(
                    html = fuccess(Redirect(routes.Lobby.home)),
                    api = _ => fuccess(BadRequest(jsonError("Challenge not created")))
                  )
                }
          }
        }
      )
    }
  }

  def hookForm = Open { implicit ctx =>
    NoBot {
      if (HTTPRequest isXhr ctx.req) NoPlaybanOrCurrent {
        env.forms.hookFilled(timeModeString = get("time")) map { html.setup.forms.hook(_) }
      }
      else fuccess {
        Redirect(routes.Lobby.home + "#hook")
      }
    }
  }

  private def hookResponse(res: HookResult) = res match {
    case HookResult.Created(id) => Ok(Json.obj(
      "ok" -> true,
      "hook" -> Json.obj("id" -> id)
    )) as JSON
    case HookResult.Refused => BadRequest(jsonError("Game was not created"))
  }

  private val hookSaveOnlyResponse = Ok(Json.obj("ok" -> true))

  def hook(sri: String) = OpenBody { implicit ctx =>
    NoBot {
      implicit val req = ctx.body
      PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
        NoPlaybanOrCurrent {
          env.forms.hook(ctx).bindFromRequest.fold(
            jsonFormError,
            userConfig => {
              val config = userConfig withinLimits ctx.me
              if (getBool("pool")) env.processor.saveHookConfig(config) inject hookSaveOnlyResponse
              else (ctx.userId ?? Env.relation.api.fetchBlocking) flatMap {
                blocking =>
                  env.processor.hook(config, Sri(sri), HTTPRequest sid req, blocking) map hookResponse
              }
            }
          )
        }
      }
    }
  }

  def like(sri: String, gameId: String) = Open { implicit ctx =>
    NoBot {
      PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
        NoPlaybanOrCurrent {
          for {
            config <- env.forms.hookConfig
            game <- GameRepo game gameId
            blocking <- ctx.userId ?? Env.relation.api.fetchBlocking
            hookConfig = game.fold(config)(config.updateFrom)
            sameOpponents = game.??(_.userIds)
            hookResult <- env.processor.hook(hookConfig, Sri(sri), HTTPRequest sid ctx.req, blocking ++ sameOpponents)
          } yield hookResponse(hookResult)
        }
      }
    }
  }

  def filterForm = Open { implicit ctx =>
    env.forms.filterFilled map {
      case (form, filter) => html.setup.filter(form, filter)
    }
  }

  def filter = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    env.forms.filter(ctx).bindFromRequest.fold[Fu[Result]](
      f => {
        controllerLogger.branch("setup").warn(f.errors.toString)
        BadRequest(()).fuccess
      },
      config => JsonOk(env.processor filter config inject config.render)
    )
  }

  def validateFen = Open { implicit ctx =>
    get("fen") flatMap ValidFen(getBool("strict")) match {
      case None => BadRequest.fuccess
      case Some(v) => Ok(html.game.bits.miniBoard(v.fen, v.color)).fuccess
    }
  }

  private def process[A](form: Context => Form[A])(op: A => BodyContext[_] => Fu[Pov]) =
    OpenBody { implicit ctx =>
      PostRateLimit(HTTPRequest lastRemoteAddress ctx.req) {
        implicit val req = ctx.body
        form(ctx).bindFromRequest.fold(
          err => negotiate(
            html = Lobby.renderHome(Results.BadRequest),
            api = _ => jsonFormError(err)
          ),
          config => op(config)(ctx) flatMap { pov =>
            negotiate(
              html = fuccess(redirectPov(pov)),
              api = apiVersion => Env.api.roundApi.player(pov, apiVersion) map { data =>
                Created(data) as JSON
              }
            )
          }
        )
      }
    }

  private[controllers] def redirectPov(pov: Pov)(implicit ctx: Context) = {
    val redir = Redirect(routes.Round.watcher(pov.gameId, "white"))
    if (ctx.isAuth) redir
    else redir withCookies LilaCookie.cookie(
      AnonCookie.name,
      pov.playerId,
      maxAge = AnonCookie.maxAge.some,
      httpOnly = false.some
    )
  }
}
