package controllers

import lila.app._
import lila.contest.{ BoardRepo, ForbiddenRepo, Invite, InviteRepo, ManualPairingSource, PlayerRepo, Request, RequestRepo, RoundRepo, ScoreSheetRepo, Contest => ContestModel }
import lila.hub.lightClazz._
import lila.hub.lightTeam._
import lila.memo.UploadRateLimit
import ornicar.scalalib.Random
import play.api.mvc.{ BodyParsers, Result }
import play.api.data.Forms._
import play.api.data.Form

import scala.concurrent.duration._
import play.api.libs.json.Json
import lila.api.Context
import lila.common.Form.numberIn
import lila.contest.DataForm.booleanChoices
import lila.user.UserRepo
import lila.security.Permission
import lila.team.TeamRepo
import org.joda.time.DateTime
import views._

object Contest extends LilaController {

  private def env = Env.contest
  private def api = env.contestApi
  private def roundApi = env.roundApi
  private def forms = env.forms

  def testPairing(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      roundApi.isContestFinished(contest).map(_ =>
        Ok("ok"))
    }
  }

  def home = Auth { implicit ctx => me =>
    env.pager.all(1, none, "") flatMap { pag =>
      Ok(html.contest.list.all(pag, none, "")).fuccess
    }
  }

  def allPage(s: Option[Int], text: String, page: Int) = Auth { implicit ctx => me =>
    val status = s.map(ContestModel.Status(_))
    env.pager.all(page, status, text) flatMap { pag =>
      Ok(html.contest.list.all(pag, status, text)).fuccess
    }
  }

  def belongPage(s: Option[Int], text: String, page: Int) = Auth { implicit ctx => me =>
    val status = s.map(ContestModel.Status(_))
    env.pager.belong(page, status, me, text) flatMap { pag =>
      Ok(html.contest.list.belong(pag, status, text)).fuccess
    }
  }

  def ownerPage(s: Option[Int], text: String, page: Int) = Auth { implicit ctx => me =>
    val status = s.map(ContestModel.Status(_))
    env.pager.owner(page, status, me, text) flatMap { pag =>
      Ok(html.contest.list.owner(pag, status, text)).fuccess
    }
  }

  def finishPage(s: Option[Int], text: String, page: Int) = Auth { implicit ctx => me =>
    val status = s.map(ContestModel.Status(_))
    env.pager.finish(page, status, text) flatMap { pag =>
      Ok(html.contest.list.finish(pag, status, text)).fuccess
    }
  }

  def createForm = Auth { implicit ctx => me =>
    Permiss {
      NoLameOrBot {
        for {
          teams <- teamList(me)
          clazzs <- clazzList(me)
        } yield Ok(html.contest.form.create(forms.contestDefault(me), teams, clazzs))
      }
    }
  }

  /*  def startsPosition(fen: Option[String]) = Auth { implicit ctx => me =>
    Ok(html.contest.form.startingPositionModal(fen)).fuccess
  }*/

  def create = AuthBody { implicit ctx => me =>
    Permiss {
      implicit val req = ctx.body
      for {
        teams <- teamList(me)
        clazzs <- clazzList(me)
        res <- {
          forms.contest(me, None).bindFromRequest.fold(
            err => BadRequest(html.contest.form.create(err, teams, clazzs)).fuccess,
            data => CreateLimitPerUser(me.id, cost = 1) {
              val contest = data.toContest(me, teams.map(t => t.id -> t.name), clazzs.map(c => c._1.id -> c._1.name))
              api.create(contest, data.roundList(contest.id)) map { c =>
                Redirect(routes.Contest.show(c.id))
              }
            }(rateLimited)
          )
        }
      } yield res
    }
  }

  def updateForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        for {
          teams <- teamList(me)
          clazzs <- clazzList(me)
          rounds <- RoundRepo.getByContest(id)
        } yield Ok(html.contest.form.update(id, forms.contestOf(me, contest, rounds), teams, clazzs))
      }
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        for {
          teams <- teamList(me)
          clazzs <- clazzList(me)
          rounds <- RoundRepo.getByContest(id)
          res <- {
            implicit val req = ctx.body
            forms.contest(me, id.some).bindFromRequest.fold(
              err => BadRequest(html.contest.form.update(id, err, teams, clazzs)).fuccess,
              data => {
                val newContest = data.toContest(me, teams.map(t => t.id -> t.name), clazzs.map(c => c._1.id -> c._1.name))
                val rounds = data.roundList(contest.id)
                api.update(contest, newContest, rounds) inject Redirect(routes.Contest.show(id))
              }
            )
          }
        } yield res
      }
    }
  }

  def show(id: String) = Auth { implicit ctx => me =>
    OptionFuOk(api.byId(id)) { contest =>
      for {
        rounds <- RoundRepo.getByContest(id)
        players <- api.playersWithUsers(contest)
        boards <- BoardRepo.getByContest(id)
        requests <- contest.isCreator(me) ?? api.requestsWithUsers(contest)
        invites <- api.inviteWithUsers(contest)
        myRequest <- RequestRepo.find(id, me.id)
        myInvite <- InviteRepo.find(id, me.id)
        forbiddens <- ForbiddenRepo.getByContest(id)
        scoreSheets <- contest.isOverStarted.?? {
          val round = rounds.find { r => r.no == contest.currentRound } err s"can not find round $id-${contest.currentRound}"
          val roundNo = if (round.isPublishResult) round.no else round.no - 1
          ScoreSheetRepo.getByRound(id, Math.max(roundNo, 1))
        }
      } yield html.contest.show(contest, rounds, players, boards, requests, invites, forbiddens, scoreSheets, myRequest, myInvite)
    }
  }

  def scoreDetail(id: String, rno: Int, uid: String) = Auth { implicit ctx => me =>
    OptionFuResult(PlayerRepo.find(id, uid)) { player =>
      BoardRepo.getByUser(id, rno, uid).map { boards =>
        Ok(html.contest.modal.scoreDetail(rno, player, boards))
      }
    }
  }

  def remove(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Created) {
          api.remove(id) inject Redirect(routes.Contest.home)
        }
      }
    }
  }

  def publish(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        Status(contest, ContestModel.Status.Created) {
          if (contest.shouldEnterStop) ForbiddenResult
          else api.publish(id) inject Redirect(routes.Contest.show(id))
        }
      }
    }
  }

  def cancel(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        api.cancel(contest) inject Redirect(routes.Contest.show(id))
      }
    }
  }

  def clone(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        for {
          teams <- teamList(me)
          clazzs <- clazzList(me)
          rounds <- RoundRepo.getByContest(id)
        } yield Ok(html.contest.form.clone(forms.contestOf(me, contest, rounds), teams, clazzs))
      }
    }
  }

  def joinForm(id: String) = AuthBody { implicit ctx => implicit me =>
    NoLameOrBot {
      NoPlayban {
        OptionFuResult(api.byId(id)) { contest =>
          Status(contest, ContestModel.Status.Published) {
            forms.anyCaptcha.map {
              html.contest.join(contest, forms.joinForm, _)
            }
          }
        }
      }
    }
  }

  def join(id: String) = AuthBody { implicit ctx => implicit me =>
    NoLameOrBot {
      NoPlayban {
        OptionFuResult(api.byId(id)) { contest =>
          Status(contest, ContestModel.Status.Published) {
            implicit val req = ctx.body
            forms.joinForm.bindFromRequest.fold(
              err => forms.anyCaptcha map { captcha =>
                BadRequest(html.contest.join(contest, err, captcha))
              },
              setup => {
                forms.anyCaptcha flatMap { captcha =>
                  RequestRepo.find(id, me.id) flatMap {
                    case Some(r) => {
                      r.status match {
                        case Request.RequestStatus.Invited => BadRequest(html.contest.join(contest, forms.joinForm, captcha, "邀请已经发出，请勿重新加入".some)).fuccess
                        case Request.RequestStatus.Joined => BadRequest(html.contest.join(contest, forms.joinForm, captcha, "您已经加入比赛".some)).fuccess
                        case Request.RequestStatus.Refused => BadRequest(html.contest.join(contest, forms.joinForm, captcha, "您已经被拒绝加入比赛".some)).fuccess
                      }
                    }
                    case None => {
                      InviteRepo.find(id, me.id) flatMap {
                        case Some(iv) => {
                          iv.status match {
                            case Invite.InviteStatus.Invited => BadRequest(html.contest.join(contest, forms.joinForm, captcha, "您已被邀请加入比赛，请通过申请".some)).fuccess
                            case Invite.InviteStatus.Joined => BadRequest(html.contest.join(contest, forms.joinForm, captcha, "您已经加入比赛".some)).fuccess
                            case Invite.InviteStatus.Refused => BadRequest(html.contest.join(contest, forms.joinForm, captcha, "您已拒绝加入比赛".some)).fuccess
                          }
                        }
                        case None => {
                          if (contest.isPlayerFull) {
                            BadRequest(html.contest.join(contest, forms.joinForm, captcha, "参赛名额已满".some)).fuccess
                          } else {
                            api.verdicts(contest, me, teamIdList, clazzIdList) flatMap { v =>
                              if (v.accepted) {
                                api.joinRequest(contest, setup.toRequest(contest, me), me) inject Redirect(routes.Contest.show(contest.id))
                              } else {
                                BadRequest(html.contest.join(contest, forms.joinForm, captcha, "请核对个人资料是否满足参赛条件".some)).fuccess
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            )
          }
        }
      }
    }
  }

  def joinProcess(requestId: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      requestOption <- RequestRepo.byId(requestId)
      userOption <- requestOption.??(req => UserRepo.byId(req.userId))
      contestOption <- requestOption.??(req => api.byId(req.contestId))
    } yield (contestOption |@| requestOption |@| userOption).tupled) {
      case (contest, request, user) => {
        Owner(contest) {
          if (!contest.isPublished && !contest.isEnterStopped && !contest.isStarted) ForbiddenResult
          else {
            implicit val req = ctx.body
            Form(single("process" -> nonEmptyText)).bindFromRequest.fold(
              _ => Redirect(routes.Contest.show(contest.id)).fuccess,
              decision => {
                val accept = decision === "accept"
                if (accept && contest.isPlayerFull) {
                  Redirect(routes.Contest.show(contest.id)).fuccess
                } else {
                  api.processRequest(contest, request, accept, user) inject Redirect(routes.Contest.show(contest.id))
                }
              }
            )
          }
        }
      }
    }
  }

  def quit(id: String) = Auth { implicit ctx => implicit me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- contestOption.??(c => RoundRepo.byId(lila.contest.Round.makeId(c.id, c.currentRound)))
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        if (!contest.quitable) Forbidden(views.html.site.message.authFailed).fuccess
        else {
          PlayerRepo.quit(id, me.id, round.isOverPairing) inject Redirect(routes.Contest.show(id))
        }
      }
    }
  }

  def removeOrKickPlayer(playerId: String) = AuthBody { implicit ctx => implicit me =>
    OptionFuResult(for {
      playerOption <- PlayerRepo.byId(playerId)
      contestOption <- playerOption.??(p => api.byId(p.contestId))
      roundOption <- contestOption.??(c => RoundRepo.byId(lila.contest.Round.makeId(c.id, c.currentRound)))
    } yield (contestOption |@| roundOption |@| playerOption).tupled) {
      case (contest, round, player) => {
        Owner(contest) {
          implicit val req = ctx.body
          Form(single("action" -> nonEmptyText)).bindFromRequest.fold(
            _ => Redirect(routes.Contest.show(contest.id)).fuccess,
            action => action match {
              case "remove" => {
                if (!contest.playerRemoveable) Forbidden(views.html.site.message.authFailed).fuccess
                else api.removePlayer(contest.id, playerId) inject Redirect(routes.Contest.show(contest.id))
              }
              case "kick" => {
                if (!contest.playerKickable) Forbidden(views.html.site.message.authFailed).fuccess
                else PlayerRepo.kick(playerId, round.isOverPairing) inject Redirect(routes.Contest.show(contest.id))
              }
              case _ => Forbidden(views.html.site.message.authFailed).fuccess
            }
          )
        }
      }
    }
  }

  def inviteForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      OwnerJson(contest) {
        if (!contest.inviteable) ForbiddenJsonResult
        else Ok(html.contest.modal.invite(contest)).fuccess
      }
    }
  }

  def invite(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      OwnerJson(contest) {
        if (!contest.inviteable) ForbiddenJsonResult
        else {
          api.inviteWithUsers(contest) flatMap { invites =>
            implicit def req = ctx.body
            Form(single(
              "username" -> lila.user.DataForm.historicalUsernameField
            )).bindFromRequest.fold(
              err => BadRequest(errorsAsJson(err)).fuccess,
              username => UserRepo.named(username) flatMap {
                case None => BadRequest(jsonError("棋手不存在")).fuccess
                case Some(user) => {
                  InviteRepo.find(id, user.id) flatMap {
                    case Some(iv) => {
                      iv.status match {
                        case Invite.InviteStatus.Invited => BadRequest(jsonError("邀请已经发出，请勿重新邀请")).fuccess
                        case Invite.InviteStatus.Joined => BadRequest(jsonError("棋手已经加入比赛")).fuccess
                        case Invite.InviteStatus.Refused => BadRequest(jsonError("棋手已经被拒绝加入比赛")).fuccess
                      }
                    }
                    case None => {
                      RequestRepo.find(id, user.id) flatMap {
                        case Some(r) => {
                          r.status match {
                            case Request.RequestStatus.Invited => BadRequest(jsonError("棋手已经请求加入比赛，请通过申请")).fuccess
                            case Request.RequestStatus.Joined => BadRequest(jsonError("棋手已经加入比赛")).fuccess
                            case Request.RequestStatus.Refused => doInvite(contest, user)
                          }
                        }
                        case None => doInvite(contest, user)
                      }
                    }
                  }
                }
              }
            )
          }
        }
      }
    }
  }

  private def doInvite(contest: ContestModel, user: lila.user.User) = {
    if (contest.isPlayerFull) {
      BadRequest(jsonError("参赛名额已满")).fuccess
    } else {
      doValidTyp(contest, user) flatMap {
        case true => {
          api.invite(
            contest,
            Invite.make(
              contest.id,
              contest.name,
              user.id
            )
          ) inject jsonOkResult
        }
        case false => BadRequest(jsonError("棋手参赛资格不足")).fuccess
      }
    }
  }

  private def doValidTyp(contest: ContestModel, user: lila.user.User): Fu[Boolean] = {
    contest.typ match {
      case ContestModel.Type.Public => teamIdList(user) map (_.contains(contest.organizer))
      case ContestModel.Type.TeamInner => teamIdList(user) map (_.contains(contest.organizer))
      case ContestModel.Type.ClazzInner => clazzIdList(user) map (_.contains(contest.organizer))
    }
  }

  def inviteProcess(inviteId: String) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      inviteOption <- InviteRepo.byId(inviteId)
      userOption <- inviteOption.??(inv => UserRepo.byId(inv.userId))
      contestOption <- inviteOption.??(inv => api.byId(inv.contestId))
    } yield (contestOption |@| inviteOption |@| userOption).tupled) {
      case (contest, invite, user) => {
        if (!contest.inviteable) Forbidden(views.html.site.message.authFailed).fuccess
        else {
          api.processInvite(contest, invite, true, user) inject Redirect(routes.Contest.show(contest.id))
        }
      }
    }
  }

  def inviteRemove(inviteId: String) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      inviteOption <- InviteRepo.byId(inviteId)
      contestOption <- inviteOption.??(inv => api.byId(inv.contestId))
    } yield (contestOption |@| inviteOption).tupled) {
      case (contest, invite) => {
        if (!contest.inviteable || invite.status == Invite.InviteStatus.Joined) Forbidden(views.html.site.message.authFailed).fuccess
        else {
          InviteRepo.remove(inviteId) inject Redirect(routes.Contest.show(contest.id))
        }
      }
    }
  }

  def forbiddenCreateForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        if (contest.status == ContestModel.Status.Published || contest.status == ContestModel.Status.EnterStopped || contest.status == ContestModel.Status.Started) {
          api.playersWithUsers(contest) map { players =>
            Ok(html.contest.modal.playerForbidden(contest, players, none))
          }
        } else ForbiddenResult
      }
    }
  }

  def forbiddenUpdateForm(id: String, fid: String) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      forbiddenOption <- ForbiddenRepo.byId(fid)
    } yield (contestOption |@| forbiddenOption).tupled) {
      case (contest, forbidden) => {
        Owner(contest) {
          if (contest.status == ContestModel.Status.Published || contest.status == ContestModel.Status.EnterStopped || contest.status == ContestModel.Status.Started) {
            api.playersWithUsers(contest) map { players =>
              Ok(html.contest.modal.playerForbidden(contest, players, forbidden.some))
            }
          } else ForbiddenResult
        }
      }
    }
  }

  def forbiddenApply(id: String, fidOption: Option[String]) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        if (contest.status == ContestModel.Status.Published || contest.status == ContestModel.Status.EnterStopped || contest.status == ContestModel.Status.Started) {
          implicit val req = ctx.body
          forms.forbidden.bindFromRequest.fold(
            err => BadRequest(errorsAsJson(err)).fuccess,
            data => fidOption match {
              case None => ForbiddenRepo.upsert(data.toForbidden(id, none)) inject Redirect(routes.Contest.show(contest.id) + "#forbidden")
              case Some(fid) => ForbiddenRepo.byId(fid) flatMap { f =>
                ForbiddenRepo.upsert(data.toForbidden(id, f)) inject Redirect(routes.Contest.show(contest.id) + "#forbidden")
              }
            }
          )
        } else ForbiddenResult
      }
    }
  }

  def removeForbidden(id: String, fid: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      forbiddenOption <- ForbiddenRepo.byId(fid)
    } yield (contestOption |@| forbiddenOption).tupled) {
      case (contest, forbidden) => {
        Owner(contest) {
          if (contest.status == ContestModel.Status.Published || contest.status == ContestModel.Status.EnterStopped || contest.status == ContestModel.Status.Started) {
            ForbiddenRepo.remove(forbidden) inject Redirect(routes.Contest.show(contest.id))
          } else ForbiddenResult
        }
      }
    }
  }

  def reorderPlayer(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        RoundRepo.byId(lila.contest.Round.makeId(contest.id, contest.currentRound)).map(r => r.isEmpty || r.??(_.isCreated)) flatMap { roundAccept =>
          if (contest.status == ContestModel.Status.Published || (contest.status == ContestModel.Status.EnterStopped && roundAccept)) {
            implicit val req = ctx.body
            Form(single(
              "playerIds" -> nonEmptyText
            )).bindFromRequest.fold(
              err => BadRequest(errorsAsJson(err)).fuccess,
              playerIds => api.reorderPlayerByPlayerIds(playerIds.split(",").toList) inject jsonOkResult
            )
          } else ForbiddenJsonResult
        }
      }
    }
  }

  def autoPairing(id: String) = AuthBody { implicit ctx => implicit me =>
    OptionFuResult(api.byId(id)) { contest =>
      OwnerJson(contest) {
        if (!contest.isPublished && !contest.isEnterStopped && !contest.isStarted) ForbiddenJsonResult
        else {
          implicit val req = ctx.body
          Form(single("autoPairing" -> numberIn(booleanChoices))).bindFromRequest.fold(
            err => BadRequest(errorsAsJson(err)).fuccess,
            auto => api.setAutoPairing(contest, auto == 1) inject jsonOkResult
          )
        }
      }
    }
  }

  def pairing(id: String, rno: Int) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- RoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        OwnerJson(contest) {
          if (contest.autoPairing || !(contest.isEnterStopped || contest.isStarted) || !round.isCreated) ForbiddenJsonResult
          else {
            roundApi.pairing(contest, round, api.publishScoreAndFinish).map { succ =>
              if (succ) jsonOkResult
              else BadRequest(jsonError("当前匹配规则下已无法再进行匹配，您可以在成绩册中发布成绩结束比赛"))
            }
          }
        }
      }
    }
  }

  def manualAbsentForm(id: String, rno: Int) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- RoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        OwnerJson(contest) {
          if (contest.autoPairing || !(contest.isEnterStopped || contest.isStarted) || !round.isCreated) ForbiddenJsonResult
          else PlayerRepo.getByContest(id) map { players =>
            Ok(html.contest.modal.manualAbsent(contest, round, players))
          }
        }
      }
    }
  }

  def manualAbsent(id: String, rno: Int) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- RoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        OwnerJson(contest) {
          if (contest.autoPairing || !(contest.isEnterStopped || contest.isStarted) || !round.isCreated) ForbiddenJsonResult
          else {
            implicit val req = ctx.body
            Form(tuple(
              "joins" -> list(nonEmptyText),
              "absents" -> list(nonEmptyText)
            )).bindFromRequest.fold(
              err => BadRequest(errorsAsJson(err)).fuccess,
              data => data match {
                case (joins, absents) => {
                  roundApi.manualAbsent(round, joins, absents) inject jsonOkResult
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
      playerOption <- PlayerRepo.byId(playerId)
      roundOption <- RoundRepo.byId(roundId)
    } yield (contestOption |@| playerOption |@| roundOption).tupled) {
      case (contest, player, round) => {
        OwnerJson(contest) {
          if (contest.autoPairing || !(contest.isEnterStopped || contest.isStarted) || !round.isPairing) ForbiddenJsonResult
          else BoardRepo.getByRound(round.id) flatMap { boards =>
            PlayerRepo.getByContest(contest.id) map { players =>
              Ok(html.contest.modal.manualPairing(contest, round, boards, players,
                ManualPairingSource(none, none, player.some, true)))
            }
          }
        }
      }
    }
  }

  def manualPairingNotBeyForm(id: String, boardId: String, color: Boolean) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      boardOption <- BoardRepo.byId(boardId)
      contestOption <- api.byId(id)
      roundOption <- boardOption.?? { b => RoundRepo.byId(b.roundId) }
    } yield (boardOption |@| contestOption |@| roundOption).tupled) {
      case (board, contest, round) => {
        OwnerJson(contest) {
          if (contest.autoPairing || !(contest.isEnterStopped || contest.isStarted) || !round.isPairing) ForbiddenJsonResult
          else BoardRepo.getByRound(round.id) flatMap { boards =>
            PlayerRepo.getByContest(contest.id) map { players =>
              Ok(html.contest.modal.manualPairing(contest, round, boards, players,
                ManualPairingSource(board.some, chess.Color(color).some, none, false)))
            }
          }
        }
      }
    }
  }

  def manualPairing(id: String, rno: Int) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- RoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        OwnerJson(contest) {
          if (contest.autoPairing || !(contest.isEnterStopped || contest.isStarted) || !round.isPairing) ForbiddenJsonResult
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

  def manualResultForm(id: String, bid: String) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      boardOption <- BoardRepo.byId(bid)
      contestOption <- boardOption.?? { b => api.byId(b.contestId) }
      roundOption <- boardOption.?? { b => RoundRepo.byId(b.roundId) }
    } yield (boardOption |@| contestOption |@| roundOption).tupled) {
      case (board, contest, round) => {
        OwnerJson(contest) {
          if (contest.autoPairing || !(contest.isStarted || contest.isFinished) || !round.isFinished) ForbiddenJsonResult
          else Ok(html.contest.modal.manualResult(contest, round, board)).fuccess
        }
      }
    }
  }

  def manualResult(id: String, bid: String) = AuthBody { implicit ctx => implicit me =>
    OptionFuResult(for {
      boardOption <- BoardRepo.byId(bid)
      contestOption <- boardOption.?? { b => api.byId(b.contestId) }
      roundOption <- boardOption.?? { b => RoundRepo.byId(b.roundId) }
    } yield (boardOption |@| contestOption |@| roundOption).tupled) {
      case (board, contest, round) => {
        Owner(contest) {
          if (contest.autoPairing || !(contest.isStarted || contest.isFinished) || !round.isFinished) ForbiddenJsonResult
          else {
            implicit val req = ctx.body
            Form(single("result" -> nonEmptyText.verifying(Set("1", "0", "-").contains(_)))).bindFromRequest.fold(
              err => Redirect(routes.Contest.show(contest.id)).fuccess,
              result => roundApi.manualResult(board, result) inject Redirect(routes.Contest.show(contest.id))
            )
          }
        }
      }
    }
  }

  def publishPairing(id: String, rno: Int) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- RoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        OwnerJson(contest) {
          if (contest.autoPairing || !(contest.isEnterStopped || contest.isStarted) || !round.isPairing) ForbiddenResult
          else {
            if (round.actualStartsAt.getMillis < DateTime.now.withSecondOfMinute(0).withMillisOfSecond(0).plusMinutes(lila.contest.Round.beforeStartMinutes).getMillis) {
              BadRequest(jsonError(s"轮次开始时间必须大于当前时间（+${lila.contest.Round.beforeStartMinutes}）分钟")).fuccess
            } else roundApi.publish(contest, round) inject jsonOkResult
          }
        }
      }
    }
  }

  def publishResult(id: String, rno: Int) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- RoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        Owner(contest) {
          if (contest.autoPairing || !contest.isStarted || !round.isFinished) ForbiddenResult
          else {
            roundApi.publishResult(contest, round.id, round.no) inject Redirect(routes.Contest.show(contest.id))
          }
        }
      }
    }
  }

  def cancelScore(id: String, sid: String) = Auth { implicit ctx => implicit me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      scoreSheetOption <- ScoreSheetRepo.byId(sid)
    } yield (contestOption |@| scoreSheetOption).tupled) {
      case (contest, scoreSheet) => {
        Owner(contest) {
          if (!contest.isStarted || contest.autoPairing || !contest.allRoundFinished) Forbidden(views.html.site.message.authFailed).fuccess
          else api.cancelScore(scoreSheet) inject Redirect(routes.Contest.show(id))
        }
      }
    }
  }

  def setRoundStartsTime(id: String, rno: Int) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      contestOption <- api.byId(id)
      roundOption <- RoundRepo.find(id, rno)
    } yield (contestOption |@| roundOption).tupled) {
      case (contest, round) => {
        OwnerJson(contest) {
          if (contest.autoPairing || !(contest.isEnterStopped || contest.isStarted) || !(round.isCreated || round.isPairing)) ForbiddenJsonResult
          else {
            implicit val req = ctx.body
            Form(single("startsAt" -> lila.common.Form.futureDateTime)).bindFromRequest.fold(
              _ => BadRequest(jsonError(s"轮次开始时间必须大于当前时间（+${lila.contest.Round.beforeStartMinutes}）分钟")).fuccess,
              st => {
                if (st.getMillis < DateTime.now.withSecondOfMinute(0).withMillisOfSecond(0).plusMinutes(lila.contest.Round.beforeStartMinutes).getMillis) {
                  BadRequest(jsonError(s"轮次开始时间必须大于当前时间（+${lila.contest.Round.beforeStartMinutes}）分钟")).fuccess
                } else roundApi.setStartsTime(contest, round.id, st) inject jsonOkResult
              }
            )
          }
        }
      }
    }
  }

  def publishScoreAndFinish(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { contest =>
      Owner(contest) {
        if (contest.autoPairing || !contest.isStarted || !contest.allRoundFinished) ForbiddenResult
        else {
          api.publishScoreAndFinish(contest) inject Redirect(routes.Contest.show(contest.id))
        }
      }
    }
  }

  def setBoardTimeForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      boardOption <- BoardRepo.byId(id)
      contestOption <- boardOption.?? { b => api.byId(b.contestId) }
      roundOption <- boardOption.?? { b => RoundRepo.byId(b.roundId) }
    } yield (boardOption |@| contestOption |@| roundOption).tupled) {
      case (board, contest, round) => {
        OwnerJson(contest) {
          if (!(contest.isEnterStopped || contest.isStarted) || !(round.isPublished || round.isStarted) || !board.isCreated) ForbiddenJsonResult
          else Ok(html.contest.modal.setBoardTime(contest, round, board)).fuccess
        }
      }
    }
  }

  def setBoardTime(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      boardOption <- BoardRepo.byId(id)
      contestOption <- boardOption.?? { b => api.byId(b.contestId) }
      roundOption <- boardOption.?? { b => RoundRepo.byId(b.roundId) }
    } yield (boardOption |@| contestOption |@| roundOption).tupled) {
      case (board, contest, round) => {
        OwnerJson(contest) {
          if (!(contest.isEnterStopped || contest.isStarted) || !(round.isPublished || round.isStarted) || !board.isCreated) ForbiddenJsonResult
          else {
            implicit def req = ctx.body
            import lila.common.Form.futureDateTime
            Form(single("startsAt" -> futureDateTime.verifying(
              s"日期必须大于当前时间（+${lila.contest.Round.beforeStartMinutes}分钟）",
              DateTime.now.plusMinutes(lila.contest.Round.beforeStartMinutes - 1).isBefore(_)
            ))).bindFromRequest.fold(
              err => Redirect(routes.Contest.show(contest.id)).fuccess,
              startsAt => roundApi.setBoardTime(contest, round, board, startsAt) inject Redirect(routes.Contest.show(contest.id))
            )
          }
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

  def uploadFile = AuthBody(BodyParsers.parse.multipartFormData) { implicit ctx => implicit me =>
    UploadRateLimit.rateLimit(me.username, ctx.req) {
      val file = ctx.body.body.file("attachments")
      file match {
        case Some(f) => api.uploadFile(Random nextString 16, f) map { dbFile =>
          Ok(Json.obj("ok" -> true, "path" -> dbFile.path))
        } recover {
          case e: lila.base.LilaException => Ok(Json.obj("ok" -> false, "message" -> e.message))
        }
        case _ => fuccess(Ok(Json.obj("ok" -> true)))
      }
    }
  }

  def ForbiddenResult(implicit ctx: Context) = Forbidden(views.html.site.message.authFailed).fuccess

  private def Status(contest: ContestModel, status: ContestModel.Status)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (contest.status == status) f
    else ForbiddenResult

  private def Owner(contest: ContestModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.??(me => contest.isCreator(me) || isGranted(_.ManageContest))) f
    else ForbiddenResult

  private def Permiss(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (isGranted(Permission.Coach) || ctx.me.??(_.hasTeam)) f
    else ForbiddenResult
  }

  def ForbiddenJsonResult(implicit ctx: Context) = fuccess(Forbidden(jsonError("Forbidden")) as JSON)

  private def StatusJson(contest: ContestModel, status: ContestModel.Status)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (contest.status == status) f
    else ForbiddenJsonResult
  }

  private def OwnerJson(contest: ContestModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (ctx.me.??(me => contest.isCreator(me) || isGranted(_.ManageContest))) f
    else ForbiddenJsonResult
  }

  private def PermissJson(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (isGranted(Permission.Coach) || ctx.me.??(_.hasTeam)) f
    else ForbiddenJsonResult
  }

  private val CreateLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 5,
    duration = 24 hour,
    name = "contest per user",
    key = "contest.user"
  )

  private val rateLimited = ornicar.scalalib.Zero.instance[Fu[Result]] {
    fuccess(Redirect(routes.Contest.home))
  }

  private[controllers] def teamIdList(me: lila.user.User): Fu[TeamIdList] =
    Env.team.api.mine(me).map(_.filter(_.team.enabled).map(_.team.id))

  private[controllers] def clazzIdList(me: lila.user.User): Fu[ClazzIdList] =
    Env.clazz.api.mine(me.id).map(_.map(_._id))

  private[controllers] def teamList(me: lila.user.User): Fu[List[lila.team.Team]] =
    Env.team.api.mine(me).map(_.filter(twm => twm.team.enabled && twm.member.isOwner).map(t => t.team))

  private[controllers] def clazzList(me: lila.user.User): Fu[List[(lila.clazz.Clazz, Boolean)]] =
    for {
      clazzs <- Env.clazz.api.mine(me.id).map(_.filter(c => c.isCreator(me.id)))
      teams <- TeamRepo.byOrderedIds(clazzs.map(_.teamOrDefault))
    } yield {
      val ratedList = teams.map(t => t.id -> (t.ratingSettingOrDefault.open && t.ratingSettingOrDefault.coachSupport)).toMap
      clazzs.map { c =>
        c -> c.team.?? { teamId =>
          ratedList.get(teamId) | false
        }
      }
    }

}
