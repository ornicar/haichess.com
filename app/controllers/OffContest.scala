package controllers

import lila.app._
import lila.memo.UploadRateLimit
import ornicar.scalalib.Random
import scala.concurrent.duration._
import play.api.mvc.{ BodyParsers, Result }
import play.api.libs.json.Json
import lila.api.Context
import play.api.data.Form
import play.api.data.Forms._
import lila.offlineContest.{ OffBoardRepo, OffForbiddenRepo, OffPlayerRepo, OffRoundRepo, OffScoreSheetRepo, OffContest => ContestModel, OffManualPairingSource }
import lila.team.TeamRatingRepo
import views._

object OffContest extends LilaController {

  private def env = Env.offlineContest
  private def api = env.api
  private def roundApi = env.roundApi
  private def forms = env.forms

  def home = Auth { implicit ctx => me =>
    env.pager.current(me, 1, none, "") flatMap { pager =>
      Ok(html.offlineContest.list.current(pager, none, "")).fuccess
    }
  }

  def currentPage(s: Option[Int], text: String, page: Int) = Auth { implicit ctx => me =>
    val status = s.map(ContestModel.Status(_))
    env.pager.current(me, page, status, text) flatMap { pager =>
      Ok(html.offlineContest.list.current(pager, status, text)).fuccess
    }
  }

  def historyPage(s: Option[Int], text: String, page: Int) = Auth { implicit ctx => me =>
    val status = s.map(ContestModel.Status(_))
    env.pager.history(me, page, status, text) flatMap { pager =>
      Ok(html.offlineContest.list.history(pager, status, text)).fuccess
    }
  }

  def show(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        for {
          rounds <- OffRoundRepo.getByContest(id)
          players <- api.playersWithUsers(contest)
          boards <- OffBoardRepo.getByContest(id)
          forbiddens <- OffForbiddenRepo.getByContest(id)
          teamRating <- contest.teamRated.?? { TeamRatingRepo.findByContest(id) }
          scoreSheets <- contest.isOverStarted.?? {
            val round = rounds.find { r => r.no == contest.currentRound } err s"can not find round $id-${contest.currentRound}"
            val roundNo = if (round.isPublishResult) round.no else round.no - 1
            OffScoreSheetRepo.getByRound(id, Math.max(roundNo, 1))
          }
        } yield Ok(html.offlineContest.show(contest, rounds, players, boards, forbiddens, teamRating, scoreSheets))
      }
    }
  }

  def createForm = Auth { implicit ctx => me =>
    NoLameOrBot {
      for {
        teams <- Contest.teamList(me)
        clazzs <- Contest.clazzList(me)
      } yield Ok(html.offlineContest.form.create(forms.contestDefault(me), teams, clazzs))
    }
  }

  def create = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    for {
      teams <- Contest.teamList(me)
      clazzs <- Contest.clazzList(me)
      res <- {
        forms.contest(me, None).bindFromRequest.fold(
          err => BadRequest(html.offlineContest.form.create(err, teams, clazzs)).fuccess,
          data => CreateLimitPerUser(me.id, cost = 1) {
            val contest = data.toContest(me, teams.map(t => t.id -> t.name), clazzs.map(c => c._1.id -> c._1.name))
            api.create(contest, data.roundList(contest.id)) map { c =>
              Redirect(routes.OffContest.show(c.id))
            }
          }(rateLimited)
        )
      }
    } yield res
  }

  def updateForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Created) {
          for {
            teams <- Contest.teamList(me)
            clazzs <- Contest.clazzList(me)
          } yield Ok(html.offlineContest.form.update(id, forms.contestOf(me, contest), teams, clazzs))
        }
      }
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Created) {
          for {
            teams <- Contest.teamList(me)
            clazzs <- Contest.clazzList(me)
            res <- {
              implicit val req = ctx.body
              forms.contest(me, id.some).bindFromRequest.fold(
                err => BadRequest(html.offlineContest.form.update(id, err, teams, clazzs)).fuccess,
                data => {
                  val newContest = data.toContest(me, teams.map(t => t.id -> t.name), clazzs.map(c => c._1.id -> c._1.name))
                  val rounds = data.roundList(contest.id)
                  api.update(contest, newContest, rounds) inject Redirect(routes.OffContest.show(id))
                }
              )
            }
          } yield res
        }
      }
    }
  }

  def uploadPicture = AuthBody(BodyParsers.parse.multipartFormData) { implicit ctx => implicit me =>
    UploadRateLimit.rateLimit(me.username, ctx.req) {
      val picture = ctx.body.body.file("logo")
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

  def remove(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Created) {
          api.remove(id) inject Redirect(routes.OffContest.home)
        }
      }
    }
  }

  def start(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Created) {
          api.start(id) inject Redirect(routes.OffContest.show(id))
        }
      }
    }
  }

  def cancel(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Started) {
          api.cancel(contest) inject Redirect(routes.OffContest.show(id))
        }
      }
    }
  }

  def playerChooseForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        if (!contest.isCreated && !contest.isStarted) ForbiddenResult
        else {
          for {
            clazzs <- api.teamClazzs(contest)
            teamTags <- api.teamTags(contest)
            allPlayers <- api.allPlayersWithUsers(contest)
            players <- api.playersWithUsers(contest)
            playerUserIds = players.map(_.userId)
          } yield {
            val all = allPlayers.filterNot(p => playerUserIds.contains(p.userId))
            val pls = if (contest.isStarted) Nil else players
            Ok(html.offlineContest.modal.playerChoose(contest, clazzs, teamTags, all, pls))
          }
        }
      }
    }
  }

  def playerChoose(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        if (!contest.isCreated && !contest.isStarted) ForbiddenResult
        else {
          implicit val req = ctx.body
          Form(single("players" -> text)).bindFromRequest.fold(
            err => BadRequest(errorsAsJson(err)).fuccess,
            players => players.trim.nonEmpty.?? {
              api.setPlayers(contest, players.split(",").toList)
            } inject Redirect(routes.OffContest.show(id))
          )
        }
      }
    }
  }

  def externalPlayerForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        if (contest.status == ContestModel.Status.Created || contest.status == ContestModel.Status.Started) {
          Ok(html.offlineContest.modal.externalPlayer(contest)).fuccess
        } else ForbiddenResult
      }
    }
  }

  def externalPlayer(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        if (contest.status == ContestModel.Status.Created || contest.status == ContestModel.Status.Started) {
          implicit val req = ctx.body
          Form(tuple(
            "username" -> lila.user.DataForm.historicalUsernameField,
            "teamRating" -> optional(number(min = 0, max = 2800))
          )).bindFromRequest.fold(
            err => BadRequest(errorsAsJson(err)).fuccess,
            data => data match {
              case (username, teamRating) => {
                OffPlayerRepo.externalExists(contest.id, username) flatMap { exists =>
                  if (exists) BadRequest(jsonError("棋手已存在")).fuccess
                  else {
                    api.externalPlayer(contest, username, teamRating) inject jsonOkResult
                  }
                }
              }
            }
          )
        } else ForbiddenResult
      }
    }
  }

  def reorderPlayer(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Created) {
          implicit val req = ctx.body
          Form(single(
            "playerIds" -> nonEmptyText
          )).bindFromRequest.fold(
            err => BadRequest(errorsAsJson(err)).fuccess,
            playerIds => api.reorderPlayer(playerIds.split(",").toList) inject jsonOkResult
          )
        }
      }
    }
  }

  def removeOrKickPlayer(playerId: String) = AuthBody { implicit ctx => implicit me =>
    OptionFuResult(for {
      playerOption <- OffPlayerRepo.byId(playerId)
      contestOption <- playerOption.??(p => api.byId(p.contestId))
    } yield (contestOption |@| playerOption).tupled) {
      case (contest, player) => {
        Owner(contest) {
          implicit val req = ctx.body
          Form(single("action" -> nonEmptyText)).bindFromRequest.fold(
            _ => Redirect(routes.OffContest.show(contest.id)).fuccess,
            action => action match {
              case "remove" => {
                if (!contest.playerRemoveable) Forbidden(views.html.site.message.authFailed).fuccess
                else api.removePlayer(contest, playerId) inject Redirect(routes.OffContest.show(contest.id))
              }
              case "kick" => {
                if (!contest.playerKickable) Forbidden(views.html.site.message.authFailed).fuccess
                else api.kickPlayer(contest, playerId) inject Redirect(routes.OffContest.show(contest.id))
              }
              case _ => Forbidden(views.html.site.message.authFailed).fuccess
            }
          )
        }
      }
    }
  }

  def forbiddenCreateForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Started) {
          api.playersWithUsers(contest) map { players =>
            Ok(html.offlineContest.modal.playerForbidden(contest, players, none))
          }
        }
      }
    }
  }

  def forbiddenUpdateForm(id: String, fid: String) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      forbiddenOption <- OffForbiddenRepo.byId(fid)
    } yield (contestOption |@| forbiddenOption).tupled) {
      case (contest, forbidden) => {
        Owner(contest) {
          Status(contest, ContestModel.Status.Started) {
            api.playersWithUsers(contest) map { players =>
              Ok(html.offlineContest.modal.playerForbidden(contest, players, forbidden.some))
            }
          }
        }
      }
    }
  }

  def forbiddenApply(id: String, fidOption: Option[String]) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Started) {
          implicit val req = ctx.body
          forms.forbidden.bindFromRequest.fold(
            err => BadRequest(errorsAsJson(err)).fuccess,
            data => fidOption match {
              case None => OffForbiddenRepo.upsert(data.toForbidden(id, none)) inject Redirect(routes.OffContest.show(contest.id) + "#forbidden")
              case Some(fid) => OffForbiddenRepo.byId(fid) flatMap { f =>
                OffForbiddenRepo.upsert(data.toForbidden(id, f)) inject Redirect(routes.OffContest.show(contest.id) + "#forbidden")
              }
            }
          )
        }
      }
    }
  }

  def removeForbidden(id: String, fid: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      forbiddenOption <- OffForbiddenRepo.byId(fid)
    } yield (contestOption |@| forbiddenOption).tupled) {
      case (contest, forbidden) => {
        Owner(contest) {
          Status(contest, ContestModel.Status.Started) {
            OffForbiddenRepo.remove(forbidden) inject Redirect(routes.OffContest.show(contest.id))
          }
        }
      }
    }
  }

  def manualAbsentForm(id: String, rno: Int) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- OffRoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        Owner(contest) {
          if (!contest.isStarted || !round.isCreated) ForbiddenJsonResult
          else api.playersWithUsers(contest) map { players =>
            Ok(html.offlineContest.modal.manualAbsent(contest, round, players))
          }
        }
      }
    }
  }

  def manualAbsent(id: String, rno: Int) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- OffRoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        Owner(contest) {
          if (!contest.isStarted || !round.isCreated) ForbiddenResult
          else {
            implicit val req = ctx.body
            Form(tuple(
              "joins" -> text,
              "absents" -> text
            )).bindFromRequest.fold(
              err => BadRequest(errorsAsJson(err)).fuccess,
              data => data match {
                case (joins, absents) => {
                  roundApi.manualAbsent(round, joins.split(",").toList, absents.split(",").toList) inject Redirect(routes.OffContest.show(contest.id))
                }
              }
            )
          }
        }
      }
    }
  }

  def manualPairingBeyForm(id: String, roundId: String, playerId: String) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      playerOption <- OffPlayerRepo.byId(playerId)
      roundOption <- OffRoundRepo.byId(roundId)
    } yield (contestOption |@| playerOption |@| roundOption).tupled) {
      case (contest, player, round) => {
        OwnerJson(contest) {
          if (!contest.isStarted || !round.isPairing) ForbiddenJsonResult
          else OffBoardRepo.getByRound(round.id) flatMap { boards =>
            api.playersWithUsers(contest) map { players =>
              Ok(html.offlineContest.modal.manualPairing(contest, round, boards, players,
                OffManualPairingSource(none, none, player.some, true)))
            }
          }
        }
      }
    }
  }

  def manualPairingNotBeyForm(id: String, boardId: String, color: Boolean) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      boardOption <- OffBoardRepo.byId(boardId)
      contestOption <- api.byId(id)
      roundOption <- boardOption.?? { b => OffRoundRepo.byId(b.roundId) }
    } yield (boardOption |@| contestOption |@| roundOption).tupled) {
      case (board, contest, round) => {
        OwnerJson(contest) {
          if (!contest.isStarted || !round.isPairing) ForbiddenJsonResult
          else OffBoardRepo.getByRound(round.id) flatMap { boards =>
            api.playersWithUsers(contest) map { players =>
              Ok(html.offlineContest.modal.manualPairing(contest, round, boards, players,
                OffManualPairingSource(board.some, chess.Color(color).some, none, false)))
            }
          }
        }
      }
    }
  }

  def manualPairing(id: String, rno: Int) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- OffRoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        OwnerJson(contest) {
          if (!contest.isStarted || !round.isPairing) ForbiddenJsonResult
          else {
            implicit val req = ctx.body
            forms.manualPairingForm.bindFromRequest.fold(
              err => BadRequest(errorsAsJson(err)).fuccess,
              data => roundApi.manualPairing(data, round) inject jsonOkResult
            )
          }
        }
      }
    }
  }

  def pairing(id: String, rno: Int) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- OffRoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        OwnerJson(contest) {
          if (!contest.isStarted || !round.isCreated) ForbiddenJsonResult
          else {
            roundApi.pairing(contest, round, api.finish).map { succ =>
              if (succ) jsonOkResult
              else BadRequest(jsonError("当前匹配规则下已无法再进行匹配"))
            }
          }
        }
      }
    }
  }

  def publishPairing(id: String, rno: Int) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- OffRoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        Owner(contest) {
          if (!contest.isStarted || !round.isPairing) ForbiddenResult
          else {
            roundApi.publish(contest, round) inject Redirect(routes.OffContest.show(contest.id))
          }
        }
      }
    }
  }

  def manualResultForm(id: String, bid: String) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      boardOption <- OffBoardRepo.byId(bid)
      contestOption <- boardOption.?? { b => api.byId(b.contestId) }
      roundOption <- boardOption.?? { b => OffRoundRepo.byId(b.roundId) }
    } yield (boardOption |@| contestOption |@| roundOption).tupled) {
      case (board, contest, round) => {
        Owner(contest) {
          if (!contest.isStarted || !round.isPublished) ForbiddenResult
          else Ok(html.offlineContest.modal.manualResult(contest, round, board)).fuccess
        }
      }
    }
  }

  def manualResult(id: String, bid: String) = AuthBody { implicit ctx => implicit me =>
    OptionFuResult(for {
      boardOption <- OffBoardRepo.byId(bid)
      contestOption <- boardOption.?? { b => api.byId(b.contestId) }
      roundOption <- boardOption.?? { b => OffRoundRepo.byId(b.roundId) }
    } yield (boardOption |@| contestOption |@| roundOption).tupled) {
      case (board, contest, round) => {
        Owner(contest) {
          if (!contest.isStarted || !round.isPublished) ForbiddenResult
          else {
            implicit val req = ctx.body
            Form(single("result" -> nonEmptyText.verifying(Set("1", "0", "-").contains(_)))).bindFromRequest.fold(
              err => Redirect(routes.OffContest.show(contest.id)).fuccess,
              result => roundApi.manualResult(board, result) inject Redirect(routes.OffContest.show(contest.id))
            )
          }
        }
      }
    }
  }

  def publishResult(id: String, rno: Int) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- OffRoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        OwnerJson(contest) {
          if (!contest.isStarted || !round.isPublished) ForbiddenResult
          else {
            OffBoardRepo.allFinished(round.id, round.boards) flatMap { af =>
              if (!af) {
                BadRequest(jsonError("必须录入了所有对局成绩，才能发布成绩")).fuccess
              } else roundApi.publishResult(contest, round.id, round.no, api.finish, api.playersWithUsers) inject Redirect(routes.OffContest.show(contest.id))
            }
          }
        }
      }
    }
  }

  private def Status(contest: ContestModel, status: ContestModel.Status)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (contest.status == status) f
    else ForbiddenResult

  private def Owner(contest: ContestModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.??(u => contest.isCreator(u) || isGranted(_.ManageContest))) f
    else ForbiddenResult

  private def OwnerJson(contest: ContestModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (ctx.me.??(me => contest.isCreator(me) || isGranted(_.ManageContest))) f
    else ForbiddenJsonResult
  }

  private def ForbiddenJsonResult(implicit ctx: Context) = fuccess(Forbidden(jsonError("Forbidden")) as JSON)

  private def ForbiddenResult(implicit ctx: Context) = Forbidden(views.html.site.message.authFailed).fuccess

  private val CreateLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 5,
    duration = 24 hour,
    name = "offlineContest per user",
    key = "offlineContest.user"
  )

  private val rateLimited = ornicar.scalalib.Zero.instance[Fu[Result]] {
    fuccess(Redirect(routes.OffContest.home))
  }

}
