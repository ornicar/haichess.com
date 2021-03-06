package lila.team

import actorApi._
import akka.actor.ActorSelection
import lila.db.dsl._
import lila.hub.actorApi.team._
import lila.hub.actorApi.timeline.{ Propagate, TeamCreate, TeamJoin }
import lila.mod.ModlogApi
import lila.user.{ User, UserRepo }
import org.joda.time.Period
import reactivemongo.api.{ Cursor, ReadPreference }
import lila.db.{ DbImage, Photographer }
import lila.game.Game
import lila.team.Team.TeamWithMember
import lila.user.User.ID
import akka.pattern.ask
import makeTimeout.large
import lila.hub.actorApi.contest.{ ContestBoard, GetContestBoard }
import lila.hub.actorApi.offContest.{ OffContestBoard, OffContestRoundResult, OffContestUser }

import scala.math.BigDecimal.RoundingMode

final class TeamApi(
    coll: Colls,
    cached: Cached,
    notifier: Notifier,
    bus: lila.common.Bus,
    indexer: ActorSelection,
    timeline: ActorSelection,
    modLog: ModlogApi,
    photographer: Photographer,
    contestActor: akka.actor.ActorSelection,
    adminUid: String
) {

  import BSONHandlers._

  val creationPeriod = Period weeks 1

  def team(id: Team.ID) = coll.team.byId[Team](id)

  def teamOptionFromSecondary(ids: Seq[String]): Fu[List[Option[Team]]] =
    coll.team.optionsByOrderedIds[Team, String](
      ids,
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def teamFromSecondary(ids: Seq[String]): Fu[List[Team]] =
    coll.team.byOrderedIds[Team, String](
      ids,
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def request(id: Team.ID) = coll.request.byId[Request](id)

  def invite(id: Team.ID) = coll.invite.byId[Invite](id)

  def create(setup: TeamSetup, me: User): Option[Fu[Team]] = me.canTeam option {
    val s = setup.trim
    findNextId flatMap { id =>
      val team = Team.make(
        id = id.toString,
        name = s.name,
        province = s.province,
        city = s.city,
        description = s.description,
        open = s.isOpen,
        createdBy = me
      )
      coll.team.insert(team) >>
        MemberRepo.add(team.id, me.id, Member.Role.Owner) /*>> addDefaultTag(team, me)*/ >>- {
          cached invalidateTeamIds me.id
          indexer ! InsertTeam(team)
          timeline ! Propagate(
            TeamCreate(me.id, team.id)
          ).toFollowersOf(me.id)
          bus.publish(CreateTeam(id = team.id, name = team.name, userId = me.id), 'team)
        } inject team
    }
  }

  def findNextId = {
    coll.team.find($empty, $id(true))
      .sort($sort desc "_id")
      .uno[Bdoc] map {
        _ flatMap { doc => doc.getAs[String]("_id") map (1 + _.toInt) } getOrElse 100000
      }
  }

  def setting(team: Team, s: TeamSetting, me: User): Funit =
    team.copy(
      open = s.open == 1,
      tagTip = s.tagTip == 1,
      ratingSetting = s.ratingSetting.some
    ) |> { team =>
      coll.team.update($id(team.id), team).void >> s.ratingSetting.open.?? {
        MemberRepo.initMembersRating(team.id, s.ratingSetting.defaultRating)
      } >> {
        !team.isCreator(me.id) ?? {
          modLog.teamEdit(me.id, team.createdBy, team.name)
        } >>- (indexer ! InsertTeam(team))
      }
    }

  def update(team: Team, edit: TeamEdit, me: User): Funit = edit.trim |> { e =>
    team.copy(
      name = if (team.certified) team.name else edit.name,
      province = e.province,
      city = e.city,
      description = e.description,
      logo = e.logo,
      envPicture = e.envPicture
    ) |> { team =>
      coll.team.update($id(team.id), team).void >>
        !team.isCreator(me.id) ?? {
          modLog.teamEdit(me.id, team.createdBy, team.name)
        } >>-
        (indexer ! InsertTeam(team))
    }
  }

  def mine(me: User): Fu[List[TeamWithMember]] = {
    for {
      teamIds <- MemberRepo.teamIdsByUser(me.id)
      teams <- coll.team.byIds[Team](teamIds)
      members <- MemberRepo.memberFromSecondary(teams.map(_.id), me.id)
    } yield (teams zip members) map {
      case (team, member) => TeamWithMember(team, member)
    }
  }

  def mineTeamOwner(user: Option[User]): Fu[Set[String]] = user.?? { u =>
    for {
      teamIds <- MemberRepo.teamIdsByUser(u.id)
      teamsOwners <- MemberRepo.teamOwners(teamIds)
    } yield teamsOwners
  }

  def mineCertifyTeam(user: Option[User]): Fu[List[Team]] = user.?? { u =>
    for {
      teamIds <- MemberRepo.teamIdsByMember(u.id)
      teams <- TeamRepo.byOrderedIds(teamIds.toSeq)
    } yield teams.filter(_.certified)
  }

  def mineMembers(user: Option[User]): Fu[Set[String]] = user.?? { u =>
    for {
      teamIds <- MemberRepo.ownerTeamIdsByUser(u.id)
      memberIds <- MemberRepo.teamMembers(teamIds)
    } yield memberIds
  }

  def hasTeams(me: User): Fu[Boolean] = cached.teamIds(me.id).map(_.value.nonEmpty)

  def hasCreatedRecently(me: User): Fu[Boolean] =
    TeamRepo.userHasCreatedSince(me.id, creationPeriod)

  def requestsWithUsers(team: Team): Fu[List[RequestWithUser]] = for {
    requests ??? RequestRepo findByTeam team.id
    users ??? UserRepo usersFromSecondary requests.map(_.user)
    teams <- teamFromSecondary(requests.map(_.team))
  } yield requests zip users zip teams map {
    case ((request, user), team) => RequestWithUser(request, user, team)
  }

  def requestsWithUsers(user: User): Fu[List[RequestWithUser]] = for {
    teamIds ??? TeamRepo teamIdsByCreator user.id
    requests ??? RequestRepo findByTeams teamIds
    users ??? UserRepo usersFromSecondary requests.map(_.user)
    teams <- teamFromSecondary(requests.map(_.team))
  } yield requests zip users zip teams map {
    case ((request, user), team) => RequestWithUser(request, user, team)
  }

  def doJoin(
    team: Team,
    user: User,
    role: Member.Role = Member.Role.Trainee,
    tags: Option[MemberTags] = None,
    mark: Option[String] = None,
    rating: Option[Int] = None,
    clazzIds: List[String]
  ): Funit =
    !belongsTo(team.id, user.id) flatMap {
      _ ?? {
        val mk = if (team.tagTip) mark else mark.fold(user.profileOrDefault.realName) { m => m.some }
        val rt = if (team.tagTip) rating else if (team.ratingSettingOrDefault.open) team.ratingSettingOrDefault.defaultRating.some else none
        MemberRepo.add(team.id, user.id, role, tags, mk, rt, clazzIds.some) >>
          TeamRepo.incMembers(team.id, +1) >>- {
            cached invalidateTeamIds user.id
            timeline ! Propagate(TeamJoin(user.id, team.id)).toFollowersOf(user.id)
            bus.publish(JoinTeam(id = team.id, userId = user.id), 'team)
          }
      } recover lila.db.recoverDuplicateKey(_ => ())
    }

  def join(teamId: Team.ID, me: User, clazzIds: List[String]): Fu[Option[Requesting]] =
    coll.team.byId[Team](teamId) flatMap {
      _ ?? { team =>
        if (team.open) doJoin(team, me, clazzIds = clazzIds) inject Joined(team).some
        else fuccess(Motivate(team).some)
      }
    }

  def joinApi(teamId: Team.ID, me: User, oAuthAppOwner: User.ID, clazzIds: List[String]): Fu[Option[Requesting]] =
    coll.team.byId[Team](teamId) flatMap {
      _ ?? { team =>
        if (team.open || team.createdBy == oAuthAppOwner) doJoin(team, me, clazzIds = clazzIds) inject Joined(team).some
        else fuccess(Motivate(team).some)
      }
    }

  def requestable(teamId: Team.ID, user: User): Fu[Option[Team]] = for {
    teamOption ??? coll.team.byId[Team](teamId)
    able ??? teamOption.??(requestable(_, user))
  } yield teamOption filter (_ => able)

  def requestable(team: Team, user: User): Fu[Boolean] = for {
    belongs <- belongsTo(team.id, user.id)
    requested <- RequestRepo.exists(team.id, user.id)
    invited <- InviteRepo.exists(team.id, user.id)
  } yield !belongs && !requested && !invited && team.enabled

  def createRequest(team: Team, setup: RequestSetup, user: User): Funit =
    requestable(team, user) flatMap {
      _ ?? {
        val request = Request.make(team = team.id, user = user.id, message = setup.message)
        coll.request.insert(request).void >>- (cached.nbRequests invalidate team.createdBy)
      }
    }

  def acceptRequest(team: Team, request: Request, tags: MemberTags, mark: Option[String], rating: Option[Int] = None, clazzIds: List[String]): Funit = for {
    _ ??? coll.request.remove(request)
    _ = cached.nbRequests invalidate team.createdBy
    userOption ??? UserRepo byId request.user
    _ ??? userOption.??(user =>
      doJoin(team, user, tags = tags.some, mark = mark, rating = rating, clazzIds = clazzIds) >>- notifier.acceptRequest(team, request))
  } yield ()

  def processRequest(team: Team, request: Request, accept: Boolean, clazzIds: List[String]): Funit = for {
    _ ??? coll.request.remove(request)
    _ = cached.nbRequests invalidate team.createdBy
    userOption ??? UserRepo byId request.user
    _ ??? userOption.filter(_ => accept).??(user =>
      doJoin(team, user, clazzIds = clazzIds) >>- notifier.acceptRequest(team, request))
  } yield ()

  def deleteRequestsByUserId(userId: lila.user.User.ID) =
    RequestRepo.getByUserId(userId) flatMap {
      _.map { request =>
        RequestRepo.remove(request.id) >>
          TeamRepo.creatorOf(request.team).map { _ ?? cached.nbRequests.invalidate }
      }.sequenceFu
    }

  def invitesWithUsers(team: Team): Fu[List[InviteWithUser]] = for {
    invites ??? InviteRepo findByTeam team.id
    users ??? UserRepo usersFromSecondary invites.map(_.user)
  } yield invites zip users map {
    case (invite, user) => InviteWithUser(invite, user)
  }

  def createInvite(team: Team, user: User): Funit =
    coll.invite.insert(
      Invite.make(
        team = team.id,
        user = user.id,
        message = s"???????????????????????? ?? ${team.name} ?? ?????????"
      )
    ).void >>- notifier.inviteSend(team, user.id)

  def processInvite(team: Team, invite: Invite, accept: Boolean, clazzIds: List[String]): Funit = for {
    _ ??? coll.invite.remove(invite)
    userOption ??? UserRepo byId invite.user
    _ ??? userOption.filter(_ => accept).??(user => doJoin(team, user, Member.Role.Coach, clazzIds = clazzIds))
  } yield ()

  def quit(teamId: Team.ID, me: User): Fu[Option[Team]] =
    coll.team.byId[Team](teamId) flatMap {
      _ ?? { team =>
        doQuit(team, me.id) inject team.some
      }
    }

  def doQuit(team: Team, userId: User.ID): Funit = belongsTo(team.id, userId) flatMap {
    _ ?? {
      MemberRepo.remove(team.id, userId) >>
        TeamRepo.incMembers(team.id, -1) >>-
        (cached invalidateTeamIds userId)
    }
  }

  def quitAll(userId: User.ID): Funit = MemberRepo.removeByUser(userId)

  def kick(team: Team, userId: User.ID, me: User): Funit =
    doQuit(team, userId) >>
      !team.isCreator(me.id) ?? {
        modLog.teamKick(me.id, userId, team.name)
      }

  def changeOwner(team: Team, userId: User.ID, me: User): Funit =
    MemberRepo.exists(team.id, userId) flatMap { e =>
      e ?? {
        for {
          _ <- TeamRepo.changeOwner(team.id, userId)
          _ <- MemberRepo.setRole(team.id, userId, Member.Role.Owner)
          _ <- MemberRepo.setRole(team.id, me.id, Member.Role.Trainee)
          _ <- modLog.teamMadeOwner(me.id, userId, team.name)
        } yield {
          bus.publish(SetOwner(id = team.id, name = team.name, userId = userId), 'teamSetOwner)
          bus.publish(UnsetOwner(id = team.id, name = team.name, userId = me.id), 'teamUnsetOwner)
          notifier.madeOwner(team, userId)
        }
      }
    }

  def enable(team: Team): Funit =
    UserRepo.byId(team.createdBy).flatMap { uo =>
      uo.?? { u =>
        if (u.teamId.isEmpty) {
          TeamRepo.enable(team).void >>-
            bus.publish(EnableTeam(id = team.id, name = team.name, userId = team.createdBy), 'teamEnable) >>-
            (indexer ! InsertTeam(team))
        } else fufail("???????????????????????????")
      }
    }

  def disable(team: Team): Funit =
    TeamRepo.disable(team).void >>-
      bus.publish(DisableTeam(id = team.id, name = team.name, userId = team.createdBy), 'teamDisable) >>-
      (indexer ! RemoveTeam(team.id))

  // delete for ever, with members but not forums
  def delete(team: Team): Funit =
    coll.team.remove($id(team.id)) >>
      MemberRepo.removeByteam(team.id) >>-
      (indexer ! RemoveTeam(team.id))

  def syncBelongsTo(teamId: Team.ID, userId: User.ID): Boolean =
    cached.syncTeamIds(userId) contains teamId

  def belongsTo(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    cached.teamIds(userId) map (_ contains teamId)

  def owns(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    TeamRepo ownerOf teamId map (Some(userId) ==)

  def teamName(teamId: Team.ID): Option[String] = cached.name(teamId)

  def nbRequests(teamId: Team.ID) = cached.nbRequests get teamId

  def recomputeNbMembers =
    coll.team.find($empty).cursor[Team](ReadPreference.secondaryPreferred).foldWhileM({}) { (_, team) =>
      for {
        nb <- MemberRepo.countByTeam(team.id)
        _ <- coll.team.updateField($id(team.id), "nbMembers", nb)
      } yield Cursor.Cont({})
    }

  def uploadPicture(id: String, picture: Photographer.Uploaded, processFile: Boolean = false): Fu[DbImage] =
    photographer(id, picture, processFile)

  /*  def addDefaultTag(team: Team, user: User): Funit =
    Tag.defaults(team.id, user.id).map { tag =>
      TagRepo.create(tag)
    }.sequenceFu.void*/

  def addTag(team: Team, user: User, ta: TagAdd): Funit =
    TagRepo.create(
      Tag.make(
        team = team.id,
        label = ta.label,
        value = ta.value,
        typ = ta.realType,
        userId = user.id
      )
    ).void

  def updateTag(tagId: String, te: TagEdit): Funit =
    coll.tag.update(
      $id(tagId),
      $set(
        "label" -> te.label,
        "value" -> te.value
      )
    ).void

  def removeTag(id: String, tag: Tag): Funit =
    TagRepo.remove(tag._id) >> MemberRepo.removeTag(id, tag.field).void

  def addClazz(teamId: String, clazzId: String): Funit =
    TeamRepo.addClazz(teamId, clazzId)

  def removeClazz(teamId: String, clazzId: String): Funit =
    TeamRepo.removeClazz(teamId, clazzId)

  def addMemberClazz(userId: ID, teamId: String, clazzId: String): Funit =
    MemberRepo.addClazz(teamId, userId, clazzId)

  def removeMemberClazz(userId: ID, teamId: String, clazzId: String): Funit =
    MemberRepo.removeClazz(teamId, userId, clazzId)

  def setMemberRating(team: Team, member: Member, k: Int, rating: Double, note: Option[String]): Funit = {
    MemberRepo.updateMember(
      member.copy(
        rating = {
          member.rating.fold(EloRating(rating, 0)) { r =>
            r.copy(
              rating = rating,
              k = if (team.ratingSettingOrDefault.k == k) none else k.some
            )
          }
        }.some
      )
    ) >> (!member.rating.??(_.rating == rating)).?? {
        TeamRatingRepo.insert(
          TeamRating.make(
            userId = member.user,
            rating = member.rating.map(_.rating) | 0,
            diff = BigDecimal(rating - (member.rating.map(_.rating) | 0)).setScale(1, RoundingMode.DOWN).doubleValue(),
            note = note | "",
            typ = TeamRating.Typ.Setting,
            metaData = TeamRatingMetaData()
          )
        )
      }
  }

  def updateOnlineRating(game: Game, whiteOption: Option[User], blackOption: Option[User]): Funit = {
    (whiteOption |@| blackOption).tupled ?? {
      case (white, black) => {
        (for {
          whiteTeamIds <- MemberRepo.teamIdsByUser(white.id)
          blackTeamIds <- MemberRepo.teamIdsByUser(black.id)
        } yield (whiteTeamIds, blackTeamIds)).flatMap {
          case (wtis, btis) => {
            teamFromSecondary((wtis & btis).toSeq) flatMap { teams =>
              teams.map { team =>
                team.ratingSetting.?? { setting =>
                  (for {
                    whiteMemberOption <- MemberRepo.byId(team.id, white.id)
                    blackMemberOption <- MemberRepo.byId(team.id, black.id)
                  } yield (whiteMemberOption |@| blackMemberOption).tupled) flatMap {
                    _.?? {
                      case (whiteMember, blackMember) => {
                        val whiteRating = whiteMember.rating | EloRating(setting.defaultRating, 0)
                        val blackRating = blackMember.rating | EloRating(setting.defaultRating, 0)
                        val newWhiteRating = whiteRating.calc(blackRating.rating, game.whitePlayer.isWinner, setting.k)
                        val newBlackRating = blackRating.calc(whiteRating.rating, game.blackPlayer.isWinner, setting.k)

                        if (game.isContest) {
                          contestBoard(game.id) flatMap {
                            _.?? { board =>
                              board.teamRated.?? {
                                setContestRating(game, whiteMember, board, whiteRating, newWhiteRating, realName(white, whiteMember), realName(black, blackMember)) >>
                                  setContestRating(game, blackMember, board, blackRating, newBlackRating, realName(white, whiteMember), realName(black, blackMember))
                              }
                            }
                          }
                        } else {
                          val clockReach = game.clock.?? { c => (c.limitSeconds + c.incrementSeconds * 40) / 60 >= math.max(setting.minutes, 2) }
                          (setting.open && game.turns > setting.turns && clockReach).?? {
                            game.rated.?? {
                              setGameRating(game, whiteMember, whiteRating, newWhiteRating, realName(white, whiteMember), realName(black, blackMember)) >>
                                setGameRating(game, blackMember, blackRating, newBlackRating, realName(white, whiteMember), realName(black, blackMember))
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }.sequenceFu.void
            }
          }
        }
      }
    }
  }

  private def contestBoard(gameId: String) = (contestActor ? GetContestBoard(gameId)).mapTo[Option[ContestBoard]]

  def updateOfflineRating(result: OffContestRoundResult): Funit = {
    result.teamId.?? { teamId =>
      TeamRepo.byId(teamId) flatMap {
        _.?? { team =>
          team.ratingSetting.?? { setting =>
            setting.open.?? {
              val userIds = result.boards.flatten { board =>
                Map(board.white.userId -> board.white.external, board.black.userId -> board.black.external).filterNot(_._2).keySet
              }

              MemberRepo.memberFromSecondary(teamId, userIds) flatMap { members =>
                result.boards.map { board =>
                  def whiteMember = findMember(team, board.white, members)
                  def blackMember = findMember(team, board.black, members)

                  val whiteRating = whiteMember.rating | EloRating(setting.defaultRating, 0)
                  val blackRating = blackMember.rating | EloRating(setting.defaultRating, 0)
                  val newWhiteRating = whiteRating.calc(blackRating.rating, board.white.isWinner, setting.k)
                  val newBlackRating = blackRating.calc(whiteRating.rating, board.black.isWinner, setting.k)

                  val r = {
                    if (board.white.isWinner | false) {
                      "1-0"
                    } else if (board.black.isWinner | false) {
                      "0-1"
                    } else "1/2-1/2 "
                  }

                  val note = s"${result.contestFullName} ???${result.roundNo}???  ${board.white.realName} $r ${board.black.realName}"
                  setOffContestRating(result, board, board.white, whiteMember, whiteRating, newWhiteRating, note) >>
                    setOffContestRating(result, board, board.black, blackMember, blackRating, newBlackRating, note)
                }.sequenceFu.void
              }
            }
          }
        }
      }
    }
  }

  private def findMember(team: Team, u: OffContestUser, members: List[Member]): Member = {
    if (u.external) {
      Member.make(team.id, u.userId, rating = u.teamRating)
    } else {
      members.find(_.user == u.userId) err s"can not find member ${u.userId}"
    }
  }

  private def setOffContestRating(
    result: OffContestRoundResult,
    board: OffContestBoard,
    u: OffContestUser,
    member: Member,
    oldRating: EloRating,
    newRating: EloRating,
    note: String
  ): Funit = if (u.external) funit else {
    setRating(member, oldRating, newRating, note, TeamRating.Typ.OffContest,
      TeamRatingMetaData(
        contestId = result.contestId.some,
        roundNo = result.roundNo.some,
        boardId = board.id.some
      ))
  }

  private def setGameRating(
    game: Game,
    member: Member,
    oldRating: EloRating,
    newRating: EloRating,
    whiteUserRealName: String,
    blackUserRealName: String
  ): Funit = {
    setRating(
      member,
      oldRating,
      newRating,
      gameResult(game, whiteUserRealName, blackUserRealName),
      TeamRating.Typ.Game,
      TeamRatingMetaData(gameId = game.id.some)
    )
  }

  private def setContestRating(
    game: Game,
    member: Member,
    board: ContestBoard,
    oldRating: EloRating,
    newRating: EloRating,
    whiteUserRealName: String,
    blackUserRealName: String
  ): Funit = {
    val note = s"${board.contestFullName} ???${board.roundNo}???  ${gameResult(game, whiteUserRealName, blackUserRealName)}"
    setRating(
      member,
      oldRating,
      newRating,
      note,
      TeamRating.Typ.Contest,
      TeamRatingMetaData(
        contestId = board.contestId.some,
        roundNo = board.roundNo.some,
        boardId = game.id.some
      )
    )
  }

  private def setRating(
    member: Member,
    oldRating: EloRating,
    newRating: EloRating,
    note: String,
    typ: TeamRating.Typ,
    metaData: TeamRatingMetaData
  ): Funit = {
    MemberRepo.updateMember(member.copy(rating = newRating.some)) >> TeamRatingRepo.insert(
      TeamRating.make(
        userId = member.user,
        rating = oldRating.rating,
        diff = BigDecimal(newRating.rating - oldRating.rating).setScale(1, RoundingMode.DOWN).doubleValue(),
        note = note,
        typ = typ,
        metaData = metaData
      )
    )
  }

  private def gameResult(game: Game, whiteUserRealName: String, blackUserRealName: String): String = {
    val result = {
      game.winnerColor match {
        case None => "1/2-1/2"
        case Some(c) => c.fold("1-0", "0-1")
      }
    }
    s"$whiteUserRealName $result $blackUserRealName"
  }

  private def realName(user: User, member: Member) = {
    user.profile.??(_.realName).fold(member.mark | user.username)(n => n)
  }

}
