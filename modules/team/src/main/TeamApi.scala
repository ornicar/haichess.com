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
import lila.team.Team.TeamWithMember
import lila.user.User.ID

final class TeamApi(
    coll: Colls,
    cached: Cached,
    notifier: Notifier,
    bus: lila.common.Bus,
    indexer: ActorSelection,
    timeline: ActorSelection,
    modLog: ModlogApi,
    photographer: Photographer,
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
        MemberRepo.updateMembersRating(team.id, s.ratingSetting.defaultRating)
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
    requests ← RequestRepo findByTeam team.id
    users ← UserRepo usersFromSecondary requests.map(_.user)
    teams <- teamFromSecondary(requests.map(_.team))
  } yield requests zip users zip teams map {
    case ((request, user), team) => RequestWithUser(request, user, team)
  }

  def requestsWithUsers(user: User): Fu[List[RequestWithUser]] = for {
    teamIds ← TeamRepo teamIdsByCreator user.id
    requests ← RequestRepo findByTeams teamIds
    users ← UserRepo usersFromSecondary requests.map(_.user)
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
        MemberRepo.add(team.id, user.id, role, tags, mark, rating, clazzIds.some) >>
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
    teamOption ← coll.team.byId[Team](teamId)
    able ← teamOption.??(requestable(_, user))
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

  def acceptRequest(team: Team, request: Request, tags: MemberTags, mark: Option[String], rating: Option[Int], clazzIds: List[String]): Funit = for {
    _ ← coll.request.remove(request)
    _ = cached.nbRequests invalidate team.createdBy
    userOption ← UserRepo byId request.user
    _ ← userOption.??(user =>
      doJoin(team, user, tags = tags.some, mark = mark, rating = rating, clazzIds = clazzIds) >>- notifier.acceptRequest(team, request))
  } yield ()

  def processRequest(team: Team, request: Request, accept: Boolean, clazzIds: List[String]): Funit = for {
    _ ← coll.request.remove(request)
    _ = cached.nbRequests invalidate team.createdBy
    userOption ← UserRepo byId request.user
    _ ← userOption.filter(_ => accept).??(user =>
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
    invites ← InviteRepo findByTeam team.id
    users ← UserRepo usersFromSecondary invites.map(_.user)
  } yield invites zip users map {
    case (invite, user) => InviteWithUser(invite, user)
  }

  def createInvite(team: Team, user: User): Funit =
    coll.invite.insert(
      Invite.make(
        team = team.id,
        user = user.id,
        message = s"管理员邀请您加入 « ${team.name} » 俱乐部"
      )
    ).void >>- notifier.inviteSend(team, user.id)

  def processInvite(team: Team, invite: Invite, accept: Boolean, clazzIds: List[String]): Funit = for {
    _ ← coll.invite.remove(invite)
    userOption ← UserRepo byId invite.user
    _ ← userOption.filter(_ => accept).??(user => doJoin(team, user, Member.Role.Coach, clazzIds = clazzIds))
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
        } else fufail("成员已经拥有俱乐部")
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

}
