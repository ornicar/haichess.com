package controllers

import lila.api.Context
import lila.app._
import lila.common.paginator.Paginator
import lila.common.{ HTTPRequest, MaxPerSecond }
import lila.memo.UploadRateLimit
import lila.security.Granter
import lila.team.{ Invite, InviteRepo, Joined, MemberRepo, MemberSearch, Motivate, RequestRepo, TagRepo, TeamRepo, Team => TeamModel, TeamRatingRepo, MemberWithUser }
import lila.user.{ UserRepo, User => UserModel }
import ornicar.scalalib.Random
import play.api.libs.json.Json
import play.api.data._
import play.api.data.Forms._
import views._
import play.api.mvc._

object Team extends LilaController {

  private def forms = Env.team.forms
  private def api = Env.team.api
  private def paginator = Env.team.paginator
  private lazy val teamInfo = Env.current.teamInfo

  def all(page: Int) = Open { implicit ctx =>
    NotForKids {
      Ok(html.team.list.all(Paginator.empty)).fuccess
    }
  }

  def search(text: String, page: Int) = OpenBody { implicit ctx =>
    NotForKids {
      if (text.trim.isEmpty) paginator popularTeams page map { html.team.list.all(_) }
      else paginator.popularTeams(page, text.some) map { teams => html.team.list.search(text, teams) }
    }
  }

  def home(page: Int) = Open { implicit ctx =>
    NotForKids {
      ctx.me.??(api.hasTeams) map {
        case true => Redirect(routes.Team.mine)
        case false => Redirect(routes.Team.all(page))
      }
    }
  }

  def show(id: String, page: Int) = Open { implicit ctx =>
    NotForKids {
      OptionFuOk(api team id) { team =>
        renderTeam(team, page)
      }
    }
  }

  private def renderTeam(team: TeamModel, page: Int = 1, error: Option[String] = None)(implicit ctx: Context) = for {
    info <- teamInfo(team, ctx.me)
    member <- ctx.userId.??(userId => MemberRepo.byId(team.id, userId))
    members <- paginator.teamMembers(team, page, MemberSearch(role = lila.team.Member.Role.Trainee.id.some))
    _ <- Env.user.lightUserApi preloadMany info.userIds
  } yield html.team.show(team, member, members, info, error)

  def users(teamId: String) = Action.async { req =>
    import Api.limitedDefault
    Env.team.api.team(teamId) flatMap {
      _ ?? { team =>
        Api.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
          import play.api.libs.iteratee._
          Api.jsonStream {
            Env.team.memberStream(team, MaxPerSecond(20)) &>
              Enumeratee.map(Env.api.userApi.one)
          } |> fuccess
        }
      }
    }
  }

  def setting(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        TagRepo.findByTeam(id) map { tags =>
          html.team.forms.setting(team, tags, forms setting team)
        }
      }
    }
  }

  def settingApply(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        TagRepo.findByTeam(id) flatMap { tags =>
          implicit val req = ctx.body
          forms.setting(team).bindFromRequest.fold(
            err => BadRequest(html.team.forms.setting(team, tags, err)).fuccess,
            data => api.setting(team, data, me) inject Redirect(routes.Team.show(team.id))
          )
        }
      }
    }
  }

  def addTagModal(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        Ok(html.team.forms.addTag(id, forms.tagAdd)).fuccess
      }
    }
  }

  def addTagApply(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        implicit val req = ctx.body
        forms.tagAdd.bindFromRequest.fold(
          err => BadRequest(html.team.forms.addTag(id, err)).fuccess,
          data => api.addTag(team, me, data) inject Redirect(routes.Team.setting(id))
        )
      }
    }
  }

  def editTagModal(tagId: String) = Auth { implicit ctx => me =>
    OptionFuResult(TagRepo.byId(tagId)) { tag =>
      OptionFuResult(api team tag.team) { team =>
        OwnerAndEnable(team) {
          Ok(html.team.forms.editTag(tag, forms.tagEditOf(tag))).fuccess
        }
      }
    }
  }

  def editTagApply(tagId: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(TagRepo.byId(tagId)) { tag =>
      OptionFuResult(api team tag.team) { team =>
        OwnerAndEnable(team) {
          implicit val req = ctx.body
          forms.tagEdit.bindFromRequest.fold(
            err => BadRequest(html.team.forms.editTag(tag, err)).fuccess,
            data => api.updateTag(tagId, data) inject Redirect(routes.Team.setting(tag.team))
          )
        }
      }
    }
  }

  def removeTag(id: String, tagId: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(TagRepo.byId(tagId)) { tag =>
      OptionFuResult(api team id) { team =>
        OwnerAndEnable(team) {
          api.removeTag(id, tag) inject Redirect(routes.Team.setting(id))
        }
      }
    }
  }

  def edit(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) { fuccess(html.team.forms.edit(team, forms edit team)) }
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

  def update(id: String) = AuthBody { implicit ctx => implicit me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        implicit val req = ctx.body
        forms.edit(team).bindFromRequest.fold(
          err => BadRequest(html.team.forms.edit(team, err)).fuccess,
          data => api.update(team, data, me) inject Redirect(routes.Team.show(team.id))
        )
      }
    }
  }

  def kickForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        MemberRepo userIdsByTeam team.id map { userIds =>
          html.team.admin.kick(team, userIds - me.id)
        }
      }
    }
  }

  def kick(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        implicit val req = ctx.body
        forms.kickForm.bindFromRequest.fold(
          _ => fuccess(BadRequest(routes.Team.show(team.id).toString)), {
            case (u, url) =>
              if (me.id == u) {
                fuccess(BadRequest(routes.Team.show(team.id).toString))
              } else {
                api.kick(team, u, me) inject Redirect(url)
              }
          }
        )
      }
    }
  }

  def kickUser(teamId: String, userId: String) = Scoped(_.Team.Write) { req => me =>
    api team teamId flatMap {
      _ ?? { team =>
        if (team.isCreator(me.id) && team.enabled) api.kick(team, userId, me) inject jsonOkResult
        else Forbidden(jsonError("Not your team or team disable")).fuccess
      }
    }
  }

  def changeOwnerForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        MemberRepo userIdsByTeam team.id map { userIds =>
          html.team.admin.changeOwner(team, userIds - team.createdBy)
        }
      }
    }
  }

  def changeOwner(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        if (team.certified) {
          Forbidden(views.html.site.message.teamNotAvailable).fuccess
        } else {
          implicit val req = ctx.body
          forms.selectMember.bindFromRequest.value ?? { userId =>
            UserRepo.byId(userId).flatMap {
              case None => Redirect(routes.Team.show(team.id)).fuccess
              case Some(u) => {
                if (u.teamId.isEmpty) {
                  api.changeOwner(team, userId, me) inject Redirect(routes.Team.show(team.id))
                } else Forbidden(views.html.site.message.teamOwnerCannotChange(userId)).fuccess
              }
            }
          }
        }
      }
    }
  }

  def close(id: String) = Secure(_.ManageTeam) { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      (api delete team) >>
        Env.mod.logApi.deleteTeam(me.id, team.name, team.description) inject
        Redirect(routes.Team all 1)
    }
  }

  def disable(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        (api disable team) >>
          Env.mod.logApi.deleteTeam(me.id, team.name, team.description) inject
          Redirect(routes.Team all 1)
      }
    }
  }

  def form = Auth { implicit ctx => me =>
    NotForKids {
      //OnePerWeek(me) {
      if (me.hasTeam) {
        Forbidden("每个用户仅可创建一个俱乐部").fuccess
      } else {
        forms.anyCaptcha map { captcha =>
          Ok(html.team.forms.create(forms.create, captcha))
        }
      }
      //}
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    //OnePerWeek(me) {
    if (me.hasTeam) {
      Forbidden("每个用户仅可创建一个俱乐部").fuccess
    } else {
      if (me.cellphone.isEmpty) Forbidden("请先绑定手机").fuccess
      else {
        implicit val req = ctx.body
        forms.create.bindFromRequest.fold(
          err => forms.anyCaptcha map { captcha =>
            BadRequest(html.team.forms.create(err, captcha))
          },
          data => api.create(data, me) ?? {
            _ map { team => Redirect(routes.Team.show(team.id)): Result }
          }
        )
      }
    }
    //}
  }

  def mine = Auth { implicit ctx => me =>
    api mine me map { html.team.list.mine(_) }
  }

  def join(id: String) = AuthOrScoped(_.Team.Write)(
    auth = ctx => me => Env.clazz.api.myTeamClazz(me.id, id) flatMap { clazzIds =>
      api.join(id, me, clazzIds) flatMap {
        case Some(Joined(team)) => Redirect(routes.Team.show(team.id)).fuccess
        case Some(Motivate(team)) => Redirect(routes.Team.requestForm(team.id)).fuccess
        case _ => notFound(ctx)
      }
    },
    scoped = req => me => Env.oAuth.server.fetchAppAuthor(req) flatMap {
      _ ?? { xx =>
        Env.clazz.api.myTeamClazz(me.id, id) flatMap { clazzIds =>
          api.joinApi(id, me, xx, clazzIds)
        }
      }
    } map {
      case Some(Joined(_)) => jsonOkResult
      case Some(Motivate(_)) => Forbidden(jsonError("This team requires confirmation, and is not owned by the oAuth app owner."))
      case _ => NotFound(jsonError("Team not found"))
    }
  )

  def requests = Auth { implicit ctx => me =>
    Env.team.cached.nbRequests invalidate me.id
    api requestsWithUsers me map { html.team.request.all(_) }
  }

  def acceptMemberModal(requestId: String, referrer: String) = Auth { implicit ctx => me =>
    OptionFuResult(api request requestId) { request =>
      OptionFuResult(api team request.team) { team =>
        OwnerAndEnable(team) {
          TagRepo.findByTeam(request.team) flatMap { tags =>
            UserRepo.byId(request.user) map { requestUser =>
              Ok(html.team.request.accept(team, requestId, requestUser, referrer, tags, forms.memberAdd))
            }
          }
        }
      }
    }
  }

  def acceptMemberApply(requestId: String, referrer: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      requestOption ← api request requestId
      teamOption ← requestOption.??(req => TeamRepo.owned(req.team, me.id))
    } yield (teamOption |@| requestOption).tupled) {
      case (team, request) => {
        TagRepo.findByTeam(team.id) flatMap { tags =>
          UserRepo.byId(request.user) flatMap { requestUser =>
            implicit val req = ctx.body
            forms.memberAdd.bindFromRequest.fold(
              err => fuccess(BadRequest(html.team.request.accept(team, requestId, requestUser, referrer, tags, err))),
              data => Env.clazz.api.myTeamClazz(request.user, team.id) flatMap { clazzIds =>
                api.acceptRequest(team, request, lila.team.MemberTags.byTagList(data.fields), data.mark, clazzIds) inject Redirect(referrer)
              }
            )
          }
        }
      }
    }
  }

  def requestForm(id: String) = Auth { implicit ctx => me =>
    OptionFuOk(api.requestable(id, me)) { team =>
      forms.anyCaptcha map { html.team.request.requestForm(team, forms.request, _) }
    }
  }

  def requestCreate(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.requestable(id, me)) { team =>
      implicit val req = ctx.body
      forms.request.bindFromRequest.fold(
        err => forms.anyCaptcha map { captcha =>
          BadRequest(html.team.request.requestForm(team, err, captcha))
        },
        setup => api.createRequest(team, setup, me) inject Redirect(routes.Team.show(team.id))
      )
    }
  }
  //clazzIds ← teamOption.??(team => Env.clazz.api.myTeamClazz(me.id, team.id))
  def requestProcess(requestId: String) = AuthBody { implicit ctx => me =>
    OptionFuRedirectUrl(for {
      requestOption ← api request requestId
      teamOption ← requestOption.??(req => TeamRepo.owned(req.team, me.id))
    } yield (teamOption |@| requestOption).tupled) {
      case (team, request) => {
        implicit val req = ctx.body
        forms.processRequest.bindFromRequest.fold(
          _ => fuccess(routes.Team.show(team.id).toString), {
            case (decision, url) =>
              if (team.disabled) fuccess(url)
              else {
                Env.clazz.api.myTeamClazz(request.user, team.id) flatMap { clazzIds =>
                  api.processRequest(team, request, decision === "accept", clazzIds) inject url
                }
              }
          }
        )
      }
    }
  }

  def invite(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      if (team.disabled) {
        renderInviteMessage(team, Invite.InviteMessage.Disabled)
      } else {
        if (team.certified) {
          implicit val req = ctx.body
          Form(single(
            "username" -> lila.user.DataForm.historicalUsernameField
          )).bindFromRequest.fold(
            _ => renderInviteMessage(team, Invite.InviteMessage.CoachNotFound),
            username => UserRepo.named(username) flatMap {
              case None => renderInviteMessage(team, Invite.InviteMessage.CoachNotFound)
              case Some(u) => if (Granter(_.Coach)(u)) {
                api.belongsTo(team.id, u.id) flatMap {
                  case true => renderInviteMessage(team, Invite.InviteMessage.Joined)
                  case false => InviteRepo.exists(team.id, u.id) flatMap {
                    case true => renderInviteMessage(team, Invite.InviteMessage.Inviting)
                    case false => RequestRepo.exists(team.id, u.id) flatMap {
                      case true => renderInviteMessage(team, Invite.InviteMessage.Requested)
                      case false => api.createInvite(team, u) inject Redirect(routes.Team.show(team.id))
                    }
                  }
                }
              } else {
                renderInviteMessage(team, Invite.InviteMessage.MustCoach)
              }
            }
          )
        } else {
          Forbidden("team is not certified").fuccess
        }
      }
    }
  }

  private def renderInviteMessage(team: TeamModel, im: Invite.InviteMessage)(implicit ctx: Context) =
    renderTeam(team = team, page = 1, error = im.message.some) map { Ok(_) }

  def inviteProcess(inviteId: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(for {
      inviteOption ← api invite inviteId
      teamOption ← inviteOption.??(inv => api team inv.team)
    } yield (teamOption |@| inviteOption).tupled) {
      case (team, invite) => {
        if (team.certified) {
          implicit val req = ctx.body
          Form(single(
            "process" -> nonEmptyText
          )).bindFromRequest.fold(
            _ => renderTeam(team) map { BadRequest(_) },
            process => {
              if (team.disabled) Redirect(routes.Team.show(team.id)).fuccess
              else Env.clazz.api.myTeamClazz(me.id, team.id) flatMap { clazzIds =>
                api.processInvite(team, invite, process === "accept", clazzIds) inject Redirect(routes.Team.show(team.id))
              }
            }
          )
        } else {
          Forbidden("team is not certified").fuccess
        }
      }
    }
  }

  def quit(id: String) = AuthOrScoped(_.Team.Write)(
    auth = ctx => me => OptionResult(api.quit(id, me)) { team =>
      Redirect(routes.Team.show(team.id))
    }(ctx),
    scoped = req => me => api.quit(id, me) flatMap {
      _.fold(notFoundJson())(_ => jsonOkResult.fuccess)
    }
  )

  def members(id: String, p: Int = 1) = AuthBody { implicit ctx => me =>
    OptionFuOk(api team id) { team =>
      TagRepo.findByTeam(id) flatMap { tags =>
        implicit val req = ctx.body
        val form = forms.memberSearch.bindFromRequest
        form.fold(
          fail => {
            fuccess(html.team.member(fail, team, Paginator.empty, tags, MemberSearch.empty))
          },
          data => {
            paginator.teamMembers(team, p, data) map { pager =>
              html.team.member(form, team, pager, tags, data)
            }
          }
        )
      }
    }
  }

  def editMemberModal(memberId: String) = Auth { implicit ctx => me =>
    OptionFuResult(MemberRepo.memberWithUser(memberId)) { mwu =>
      OptionFuResult(api team mwu.team) { team =>
        TagRepo.findByTeam(mwu.team) flatMap { tags =>
          OwnerAndEnable(team) {
            Ok(html.team.member.edit(mwu, tags, forms.memberEditOf(team, mwu))).fuccess
          }
        }
      }
    }
  }

  def editMemberApply(memberId: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(MemberRepo.memberWithUser(memberId)) { mwu =>
      OptionFuResult(api team mwu.team) { team =>
        TagRepo.findByTeam(mwu.team) flatMap { tags =>
          OwnerAndEnable(team) {
            implicit val req = ctx.body
            forms.memberEdit(mwu.user).bindFromRequest.fold(
              err => BadRequest(
                html.team.member.edit(mwu, tags, err)
              ).fuccess,
              data => MemberRepo.updateMember(
                mwu.member.copy(
                  role = lila.team.Member.Role(data.role),
                  mark = data.mark,
                  tags = lila.team.MemberTags.byTagList(data.fields).some
                )
              ) inject Redirect(routes.Team.members(mwu.team, 1))
            )
          }
        }
      }
    }
  }

  def ratingRule() = Auth { implicit ctx => me =>
    Ok(views.html.team.ratingRulePage()).fuccess
  }

  def ratingDistributionChart(id: String, clazzId: String) = Auth { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        MemberRepo.ratingDistribution(team.id, if (clazzId.isEmpty) none else clazzId.some).map { distributionData =>
          Ok(distributionData.mkString("[", ",", "]")) as JSON
        }
      }
    }
  }

  def ratingDistribution(id: String, p: Int = 1) = AuthBody { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      OwnerAndEnable(team) {
        implicit val req = ctx.body
        val form = Form(tuple(
          "q" -> optional(lila.user.DataForm.historicalUsernameField),
          "clazzId" -> optional(nonEmptyText)
        )).bindFromRequest
        form.fold(
          fail => {
            fuccess(Ok(html.team.ratingDistribution(fail, team, Nil, Paginator.empty[MemberWithUser], Nil)))
          },
          data => data match {
            case (q, clazzId) => {
              for {
                clazzs <- team.clazzIds.??(Env.clazz.api.byIds)
                pager <- MemberRepo.ratingPage(team.id, p, q, clazzId)
                distributionData <- MemberRepo.ratingDistribution(team.id, clazzId)
              } yield Ok(html.team.ratingDistribution(form, team, clazzs.map(c => c.id -> c.name), pager, distributionData))
            }
          }
        )
      }
    }
  }

  def memberRatingModal(memberId: String) = Auth { implicit ctx => me =>
    OptionFuResult(MemberRepo.memberWithUser(memberId)) { mwu =>
      OptionFuResult(api team mwu.team) { team =>
        OwnerAndEnable(team) {
          Ok(html.team.ratingDistribution.ratingEdit(mwu, forms.ratingEditOf(team, mwu))).fuccess
        }
      }
    }
  }

  def memberRatingApply(memberId: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(MemberRepo.memberWithUser(memberId)) { mwu =>
      OptionFuResult(api team mwu.team) { team =>
        OwnerAndEnable(team) {
          implicit val req = ctx.body
          forms.ratingEdit.bindFromRequest.fold(
            err => BadRequest(
              html.team.ratingDistribution.ratingEdit(mwu, err)
            ).fuccess,
            data => api.setMemberRating(team, mwu.member, data.k, data.rating.doubleValue(), data.note) inject Redirect(routes.Team.ratingDistribution(team.id, 1))
          )
        }
      }
    }
  }

  def memberRatingDistribution(memberId: String, p: Int = 1) = Auth { implicit ctx => me =>
    OptionFuResult(MemberRepo.memberWithUser(memberId)) { mwu =>
      OptionFuResult(api team mwu.team) { team =>
        for {
          member <- MemberRepo.byId(team.id, me.id)
          distributionData <- MemberRepo.ratingDistribution(team.id, none)
          historyData <- TeamRatingRepo.historyData(mwu.user.id)
          pager <- TeamRatingRepo.page(p, mwu.user.id)
        } yield Ok(html.team.memberRatingDistribution(team, mwu, distributionData, historyData, pager))
      }
    }
  }

  private def OnePerWeek[A <: Result](me: UserModel)(f: => Fu[A])(implicit ctx: Context): Fu[Result] =
    api.hasCreatedRecently(me) flatMap { did =>
      if (did && !Granter(_.ManageTeam)(me)) Forbidden(views.html.site.message.teamCreateLimit).fuccess
      else f
    }

  private def OwnerAndEnable(team: TeamModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.??(me => (team.isCreator(me.id) || isGranted(_.ManageTeam)) && team.enabled)) f
    else Forbidden(views.html.site.message.teamNotAvailable).fuccess

}
