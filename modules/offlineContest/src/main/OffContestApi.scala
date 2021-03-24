package lila.offlineContest

import akka.actor.ActorSystem
import lila.clazz.ClazzApi
import lila.db.{ DbImage, Photographer }
import lila.notify.NotifyApi
import lila.user.UserRepo
import lila.team.{ MemberRepo, TagRepo, TeamRepo }

class OffContestApi(
    system: ActorSystem,
    notifyApi: NotifyApi,
    clazzApi: ClazzApi,
    photographer: Photographer
) {

  def byId(id: OffContest.ID): Fu[Option[OffContest]] = OffContestRepo.byId(id)

  def create(contest: OffContest, rounds: List[OffRound]): Fu[OffContest] = {
    OffContestRepo.insert(contest) >> OffRoundRepo.bulkInsert(rounds) inject contest
  }

  def update(old: OffContest, c: OffContest, rounds: List[OffRound]): Funit =
    OffContestRepo.update(
      old.copy(
        name = c.name,
        groupName = c.groupName,
        logo = c.logo,
        typ = c.typ,
        teamRated = c.teamRated,
        organizer = c.organizer,
        rule = c.rule,
        rounds = c.rounds,
        swissBtss = c.swissBtss,
        roundRobinBtss = c.roundRobinBtss
      )
    ) >> OffRoundRepo.bulkUpdate(old.id, rounds).void

  def remove(id: OffContest.ID): Funit =
    OffContestRepo.remove(id) >> OffRoundRepo.removeByContest(id) >> OffPlayerRepo.removeByContest(id)

  def start(id: OffContest.ID): Funit =
    OffContestRepo.setStatus(id, OffContest.Status.Started)

  def cancel(contest: OffContest): Funit =
    OffContestRepo.setStatus(contest.id, OffContest.Status.Canceled)

  def finish(contest: OffContest): Funit = {
    logger.info(s"比赛结束：$contest")
    OffContestRepo.setStatus(contest.id, OffContest.Status.Finished)
  }

  def teamClazzs(c: OffContest): Fu[List[(String, String)]] = {
    c.typ match {
      case OffContest.Type.Public | OffContest.Type.TeamInner => TeamRepo.byId(c.organizer) flatMap { team =>
        team.?? {
          _.clazzIds.?? { clazzIds =>
            clazzApi.byIds(clazzIds).map { clazzs =>
              clazzs.filterNot(_.deleted | false) map (c => c.id -> c.name)
            }
          }
        }
      }
      case OffContest.Type.ClazzInner => fuccess(List.empty[(String, String)])
    }
  }

  def teamTags(c: OffContest): Fu[List[lila.team.Tag]] = {
    c.typ match {
      case OffContest.Type.Public | OffContest.Type.TeamInner => TagRepo.findByTeam(c.organizer)
      case OffContest.Type.ClazzInner => clazzApi.byId(c.organizer) flatMap {
        _.?? { clazz =>
          clazz.team.?? { teamId =>
            TagRepo.findByTeam(teamId)
          }
        }
      }
    }
  }

  def allPlayersWithUsers(c: OffContest): Fu[List[OffPlayer.AllPlayerWithUser]] =
    c.typ match {
      case OffContest.Type.Public | OffContest.Type.TeamInner => for {
        members <- MemberRepo.memberByTeam(c.organizer)
        users <- UserRepo usersFromSecondary members.map(_.user)
      } yield users zip members map {
        case (user, member) => OffPlayer.AllPlayerWithUser(user, member.some)
      }
      case OffContest.Type.ClazzInner => clazzApi.byId(c.organizer) flatMap {
        _.?? { clazz =>
          for {
            userIds <- fuccess(clazz.studentsId)
            users <- UserRepo usersFromSecondary userIds
            members <- clazz.team.fold(fuccess(userIds.map(_ => none[lila.team.Member]))) { teamId =>
              MemberRepo.memberOptionFromSecondary(teamId, userIds)
            }
          } yield users zip members map {
            case (user, member) => OffPlayer.AllPlayerWithUser(user, member)
          }
        }
      }
    }

  def playersWithUsers(c: OffContest): Fu[List[OffPlayer.PlayerWithUser]] = for {
    players ← OffPlayerRepo.getByContest(c.id)
    users ← userWithExternal(players)
    members ← c.typ match {
      case OffContest.Type.Public | OffContest.Type.TeamInner => MemberRepo.memberOptionFromSecondary(c.organizer, players.map(_.userId))
      case OffContest.Type.ClazzInner => clazzApi.byId(c.organizer) flatMap {
        _.?? { clazz =>
          clazz.team.fold(fuccess(players.map(_ => none[lila.team.Member]))) { teamId =>
            MemberRepo.memberOptionFromSecondary(teamId, players.map(_.userId))
          }
        }
      }
    }
  } yield players zip users zip members map {
    case ((player, user), member) => OffPlayer.PlayerWithUser(player, user, member)
  }

  def userWithExternal(players: List[OffPlayer]): Fu[List[lila.user.User]] = {
    UserRepo.optionsByOrderedIds(players.map(_.userId)) map { users =>
      players zip users map {
        case (player, user) => if (player.external) player.virtualUser else user err s"can not find user ${player.userId}"
      }
    }
  }

  def setPlayers(c: OffContest, userIds: List[String]): Funit = {
    {
      c.typ match {
        case OffContest.Type.Public | OffContest.Type.TeamInner => MemberRepo.memberOptionFromSecondary(c.organizer, userIds)
        case OffContest.Type.ClazzInner => clazzApi.byId(c.organizer) flatMap {
          _.?? { clazz =>
            clazz.team.fold(fuccess(userIds.map(_ => none[lila.team.Member]))) { teamId =>
              MemberRepo.memberOptionFromSecondary(teamId, userIds)
            }
          }
        }
      }
    } flatMap { members =>
      OffRoundRepo.byId(OffRound.makeId(c.id, c.currentRound)) flatMap { roundOption =>
        roundOption.?? { round =>
          OffPlayerRepo.findNextNo(c.id) flatMap { no =>
            val players = members.zip(userIds).zipWithIndex map {
              case ((memberOption, userId), index) => {
                OffPlayer.make(c.id, if (c.isCreated) index + 1 else no + index, userId, memberOption.??(_.rating.map(_.intValue)), OffPlayer.isExternal(userId), c.currentRound, round.isOverPairing)
              }
            }
            if (c.isCreated) {
              OffPlayerRepo.bulkUpdate(c.id, players) >> OffContestRepo.setPlayers(c.id, userIds.size)
            } else {
              OffPlayerRepo.bulkInsert(players) >> OffContestRepo.incPlayers(c.id, players.size)
            }
          }
        }
      }
    }
  }

  def externalPlayer(c: OffContest, srcUsername: String, teamRating: Option[Int]): Funit = {
    OffRoundRepo.byId(OffRound.makeId(c.id, c.currentRound)) flatMap { roundOption =>
      roundOption.?? { round =>
        OffPlayerRepo.findNextNo(c.id) flatMap { no =>
          val player = OffPlayer.make(c.id, no, OffPlayer.withExternal(srcUsername), teamRating, true, c.currentRound, round.isOverPairing)
          OffPlayerRepo.insert(player) >> OffContestRepo.incPlayers(c.id, 1)
        }
      }
    }
  }

  def kickPlayer(c: OffContest, playerId: OffPlayer.ID): Funit =
    OffPlayerRepo.byId(playerId) flatMap {
      _.?? { player =>
        val outcome = player.roundOutcome(c.currentRound)
        OffPlayerRepo.kick(playerId, outcome.isEmpty)
      }
    }

  def removePlayer(c: OffContest, playerId: OffPlayer.ID): Funit =
    for {
      _ <- OffPlayerRepo.remove(playerId)
      _ <- reorderPlayer2(c.id)
      res <- OffContestRepo.incPlayers(c.id, -1)
    } yield res

  private def reorderPlayer2(id: OffContest.ID): Funit =
    OffPlayerRepo.getByContest(id) flatMap { players =>
      players.zipWithIndex.map {
        case (p, i) => OffPlayerRepo.setNo(p.id, i + 1)
      }.sequenceFu.void
    }

  def reorderPlayer(playerIds: List[String]): Funit =
    OffPlayerRepo.byOrderedIds(playerIds) flatMap { players =>
      players.zipWithIndex.map {
        case (p, i) => OffPlayerRepo.setNo(p.id, i + 1)
      }.sequenceFu.void
    }

  def uploadPicture(id: String, picture: Photographer.Uploaded, processFile: Boolean = false): Fu[DbImage] =
    photographer(id, picture, processFile)
}
