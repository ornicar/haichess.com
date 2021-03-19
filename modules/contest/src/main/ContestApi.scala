package lila.contest

import akka.actor.{ ActorSelection, ActorSystem }
import lila.contest.Invite.InviteStatus
import lila.contest.Request.RequestStatus
import lila.db.{ DbFile, DbImage, FileUploader, Photographer }
import lila.game.{ Game, GameRepo }
import lila.hub.{ Duct, DuctMap }
import lila.notify.Notification.Notifies
import lila.notify.{ Notification, NotifyApi }
import lila.user.{ User, UserRepo }
import lila.hub.lightTeam._
import lila.hub.lightClazz._
import lila.round.actorApi.round.ContestRoundStart
import org.joda.time.DateTime
import lila.clazz.ClazzApi
import lila.team.MemberRepo
import scala.concurrent.duration._

class ContestApi(
    system: ActorSystem,
    sequencers: DuctMap[_],
    renderer: ActorSelection,
    timeline: ActorSelection,
    verify: Condition.Verify,
    notifyApi: NotifyApi,
    clazzApi: ClazzApi,
    roundApi: RoundApi,
    roundMap: DuctMap[_],
    photographer: Photographer,
    fileUploader: FileUploader,
    reminder: ContestReminder,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  def verdicts(c: Contest, user: User, getUserTeamIds: User => Fu[TeamIdList], getUserClazzIds: User => Fu[ClazzIdList]): Fu[Condition.All.WithVerdicts] =
    verify(c, user, getUserTeamIds, getUserClazzIds)

  def byId(id: Contest.ID): Fu[Option[Contest]] = ContestRepo.byId(id)

  def fullBoardInfo(gameId: Game.ID): Fu[Option[Board.FullInfo]] =
    for {
      boardOption <- BoardRepo.byId(gameId)
      roundOption <- boardOption.?? { b => RoundRepo.byId(b.roundId) }
      contestOption <- boardOption.?? { b => byId(b.contestId) }
    } yield (boardOption |@| roundOption |@| contestOption).tupled map {
      case (board, round, contest) => Board.FullInfo(board, round, contest)
    }

  def getContestNote(gameId: Game.ID): Fu[String] = fullBoardInfo(gameId) map {
    _.?? { b => s"${b.contest.fullName} 第${b.round.no}轮" }
  }

  def contestBoard(game: Game): Fu[Option[Board]] = BoardRepo.byId(game.id)

  def create(contest: Contest, rounds: List[Round]): Fu[Contest] = {
    ContestRepo.insert(contest) >> RoundRepo.bulkInsert(rounds) inject contest
  }

  def update(old: Contest, c: Contest, rounds: List[Round]): Funit =
    ContestRepo.update(
      old.copy(
        name = c.name,
        groupName = c.groupName,
        logo = c.logo,
        typ = c.typ,
        organizer = c.organizer,
        variant = c.variant,
        position = c.position,
        mode = c.mode,
        clock = c.clock,
        rule = c.rule,
        startsAt = c.startsAt,
        finishAt = c.finishAt,
        deadline = c.deadline,
        deadlineAt = c.deadlineAt,
        maxPlayers = c.maxPlayers,
        minPlayers = c.minPlayers,
        conditions = c.conditions,
        roundSpace = c.roundSpace,
        rounds = c.rounds,
        swissBtss = c.swissBtss,
        roundRobinBtss = c.roundRobinBtss,
        canLateMinute = c.canLateMinute,
        canQuitNumber = c.canQuitNumber,
        enterApprove = c.enterApprove,
        autoPairing = c.autoPairing,
        enterCost = c.enterCost,
        hasPrizes = c.hasPrizes,
        description = c.description,
        attachments = c.attachments
      )
    ) >> RoundRepo.bulkUpdate(old.id, rounds).void

  def remove(id: Contest.ID): Funit =
    ContestRepo.remove(id) >> RoundRepo.removeByContest(id)

  def publish(id: Contest.ID): Funit =
    ContestRepo.setStatus(id, Contest.Status.Published)

  def cancel(contest: Contest): Funit =
    ContestRepo.setStatus(contest.id, Contest.Status.Canceled) >> finishNotify(contest)

  def joinRequest(contest: Contest, request: Request, user: User): Funit =
    RequestRepo.insert(request) >> !contest.enterApprove ?? {
      processRequest(contest, request, true, user)
    }

  def processRequest(c: Contest, request: Request, accept: Boolean, user: User): Funit =
    RequestRepo.setStatus(request.id, RequestStatus.applyByAccept(accept)) >> {
      if (accept) {
        teamRating(c, user) flatMap { teamRating =>
          RoundRepo.byId(Round.makeId(c.id, c.currentRound)) flatMap { roundOption =>
            roundOption.fold(funit) { round =>
              PlayerRepo.findNextNo(c.id) flatMap { no =>
                PlayerRepo.insert(Player.make(
                  contestId = request.contestId,
                  no = no,
                  user = user,
                  perfLens = c.perfLens,
                  teamRating = teamRating,
                  currentRound = c.currentRound,
                  currentRoundOverPairing = round.isOverPairing
                )) >> ContestRepo.incPlayers(c.id, +1) >>- acceptNotify(c, request)
              }
            }
          }
        }
      } else funit
    }

  def requestsWithUsers(c: Contest): Fu[List[RequestWithUser]] = for {
    requests ← RequestRepo.getByContest(c.id)
    users ← UserRepo usersFromSecondary requests.map(_.userId)
  } yield requests zip users map {
    case (request, user) => RequestWithUser(request, user)
  }

  def invite(c: Contest, invite: Invite): Funit =
    InviteRepo.insert(invite) >>- inviteNotify(c, invite)

  def processInvite(c: Contest, invite: Invite, accept: Boolean, user: User): Funit =
    InviteRepo.setStatus(invite.id, InviteStatus.applyByAccept(accept)) >> {
      if (accept) {
        teamRating(c, user) flatMap { teamRating =>
          RoundRepo.byId(Round.makeId(c.id, c.currentRound)) flatMap { roundOption =>
            roundOption.fold(funit) { round =>
              PlayerRepo.findNextNo(c.id) flatMap { no =>
                PlayerRepo.insert(
                  Player.make(
                    contestId = invite.contestId,
                    no = no,
                    user = user,
                    perfLens = c.perfLens,
                    teamRating = teamRating,
                    currentRound = c.currentRound,
                    currentRoundOverPairing = round.isOverPairing
                  )
                ) >> ContestRepo.incPlayers(c.id, +1)
              }
            }
          }
        }
      } else funit
    }

  private def teamRating(c: Contest, user: User): Fu[Option[Int]] = {
    {
      c.typ match {
        case Contest.Type.Public | Contest.Type.TeamInner => MemberRepo.byId(c.organizer, user.id) map (_.??(_.rating.map(_.intValue)))
        case Contest.Type.ClazzInner => clazzApi.byId(c.organizer) flatMap {
          _.?? { clazz =>
            clazz.team.fold(fuccess(none[Int])) { teamId =>
              MemberRepo.byId(teamId, user.id) map (_.??(_.rating.map(_.intValue)))
            }
          }
        }
      }
    }
  }

  def inviteWithUsers(c: Contest): Fu[List[InviteWithUser]] = for {
    invites ← InviteRepo.getByContest(c.id)
    users ← UserRepo usersFromSecondary invites.map(_.userId)
  } yield invites zip users map {
    case (invite, user) => InviteWithUser(invite, user)
  }

  def playersWithUsers(c: Contest): Fu[List[PlayerWithUser]] = for {
    players ← PlayerRepo.getByContest(c.id)
    users ← UserRepo usersFromSecondary players.map(_.userId)
  } yield players zip users map {
    case (player, user) => PlayerWithUser(player, user)
  }

  def removePlayer(id: Contest.ID, playerId: Player.ID): Funit =
    for {
      _ <- InviteRepo.remove(playerId)
      _ <- RequestRepo.remove(playerId)
      _ <- PlayerRepo.remove(playerId)
      _ <- reorderPlayer(id)
      res <- ContestRepo.incPlayers(id, -1)
    } yield res

  private def reorderPlayer(id: Contest.ID): Funit =
    PlayerRepo.getByContest(id) flatMap { players =>
      players.zipWithIndex.map {
        case (p, i) => PlayerRepo.setNo(p.id, i + 1)
      }.sequenceFu.void
    }

  def reorderPlayerByPlayerIds(playerIds: List[String]): Funit =
    PlayerRepo.byOrderedIds(playerIds) flatMap { players =>
      players.zipWithIndex.map {
        case (p, i) => PlayerRepo.setNo(p.id, i + 1)
      }.sequenceFu.void
    }

  private def inviteNotify(c: Contest, invite: Invite): Funit = {
    notifyApi.addNotification(Notification.make(
      Notifies(invite.userId),
      lila.notify.GenericLink(
        url = s"/contest/${c.id}",
        title = "参赛邀请".some,
        text = s"您被邀请参加【${c.fullName}】比赛".some,
        icon = "赛"
      )
    ))
  }

  private def acceptNotify(c: Contest, request: Request): Funit = {
    notifyApi.addNotification(Notification.make(
      Notifies(request.userId),
      lila.notify.GenericLink(
        url = s"/contest/${c.id}",
        title = "参赛申请通过".some,
        text = s"您已经通过【${c.fullName}】的参赛申请，请提前 ${c.canLateMinute} 分钟进入比赛".some,
        icon = "赛"
      )
    ))
  }

  private def finishNotify(c: Contest): Funit = {
    PlayerRepo.getByContest(c.id) map { players =>
      players.foreach { player =>
        notifyApi.addNotification(Notification.make(
          Notifies(player.userId),
          lila.notify.GenericLink(
            url = s"/contest/${c.id}",
            title = "比赛结束".some,
            text = s"比赛【${c.fullName}】已经结束".some,
            icon = "赛"
          )
        ))
      }
    }
  }

  def enterStop: Funit =
    ContestRepo.published map { contests =>
      contests foreach { contest =>
        if (contest.shouldEnterStop) {
          setEnterStop(contest.id)
        }
      }
    }

  def start: Funit =
    ContestRepo.enterStopped map { contests =>
      contests foreach { contest =>
        if (contest.shouldStart) {
          setStart(contest.id)
        }
      }
    }

  def finishGame(game: Game): Unit =
    game.contestId.?? { id =>
      Sequencing(id)(byId) { contest =>
        val currentRoundNo = contest.currentRound
        val currentRoundId = Round.makeId(contest.id, currentRoundNo)
        val wp = game.whitePlayer.userId err s"contest game miss player userId ${game}"
        val bp = game.blackPlayer.userId err s"contest game miss player userId ${game}"
        for {
          _ <- BoardRepo.finishGame(game)
          _ <- PlayerRepo.finishGame(contest, wp, game)
          _ <- PlayerRepo.finishGame(contest, bp, game)
          roundOption <- RoundRepo.byId(currentRoundId)
          isAllBoardFinished <- roundOption.?? { round => BoardRepo.allFinished(round.id, round.boards) }
          _ <- isAllBoardFinished.?? { RoundRepo.finish(currentRoundId) }
          contest <- if (isAllBoardFinished && contest.autoPairing) {
            roundApi.publishResult(contest, currentRoundId, currentRoundNo)
          } else fuccess(contest)
          _ <- contest.allRoundFinished.?? { contest.autoPairing.?? { publishScoreAndFinish(contest) } }
          res <- isAllBoardFinished.?? {
            toNextRound(contest, currentRoundNo + 1)
          }
        } yield res
      }
    }

  private def toNextRound(contest: Contest, no: Round.No) =
    (!contest.isFinishedOrCanceled && !contest.allRoundFinished).?? {
      setPlayerAbsent(contest, no) >> nextRound(contest, no, publishScoreAndFinish)
    }

  private def nextRound(contest: Contest, no: Round.No, publishScoreAndFinish: Contest => Funit): Funit = {
    contest.autoPairing.?? {
      RoundRepo.byId(Round.makeId(contest.id, no)).flatMap { roundOption =>
        roundOption.?? { r =>
          delayNextRound(contest, r) map { nr =>
            roundApi.pairing(contest, nr, publishScoreAndFinish).void
          }
        }
      }
    }
  }

  private def delayNextRound(contest: Contest, round: Round): Fu[Round] = {
    val now = DateTime.now.withSecondOfMinute(0).withMillisOfSecond(0)
    if (round.actualStartsAt.isBefore(now.plusMinutes(Round.beforeStartMinutes))) {
      logger.info(s"${contest.fullName} 第${round.no}轮 推迟执行")
      val st = now.plusMinutes(Round.beforeStartMinutes)
      RoundRepo.setStartsTime(round.id, st) inject round.copy(actualStartsAt = st)
    } else fuccess(round)
  }

  // 上轮 离开、退赛、踢出，本轮继续
  private def setPlayerAbsent(contest: Contest, no: Round.No): Funit = {
    PlayerRepo.getByContest(contest.id).flatMap { players =>
      val absents = players.filter(p => p.absent && p.roundOutcome(no).isEmpty)
      val leaves = absents.filter(_.leave).map(_.no)
      val quits = absents.filter(_.quit).map(_.no)
      val kicks = absents.filter(_.kick).map(_.no)
      PlayerRepo.setOutcomes(contest.id, leaves, Board.Outcome.Leave, 0) >>
        PlayerRepo.setOutcomes(contest.id, quits, Board.Outcome.Quit, 0) >>
        PlayerRepo.setOutcomes(contest.id, kicks, Board.Outcome.Kick, 0)
    }
  }

  def publishScoreAndFinish(contest: Contest): Funit = {
    logger.info(s"比赛结束：${contest}")
    computeAllScoreWithoutCancelled(contest) >> ContestRepo.finish(contest) >> finishNotify(contest)
  }

  private def computeAllScoreWithoutCancelled(contest: Contest): Funit = {
    ScoreSheetRepo.getByContest(contest.id) flatMap { scoreSheets =>
      scoreSheets.groupBy(_.roundNo).map {
        case (_, list) => list.filterNot(_.cancelled).zipWithIndex.map {
          case (s, i) => ScoreSheetRepo.setRank(s.id, i + 1)
        }.sequenceFu.void
      }.sequenceFu.void
    }
  } >> ScoreSheetRepo.setCancelledRank(contest.id)

  def cancelScore(scoreSheet: ScoreSheet): Funit = {
    ScoreSheetRepo.setCancelScore(scoreSheet) >> PlayerRepo.setCancelScore(Player.makeId(scoreSheet.contestId, scoreSheet.playerUid))
  }

  def setEnterStop(id: Contest.ID): Unit =
    Sequencing(id)(ContestRepo.publishedById) { contest =>
      if (contest.nbPlayers < contest.minPlayers) {
        logger.info(s"比赛取消：${contest}")
        ContestRepo.setStatus(id, Contest.Status.Canceled)
      } else {
        logger.info(s"比赛报名截止：${contest}")
        ContestRepo.setStatus(id, Contest.Status.EnterStopped) >>- contest.autoPairing.?? {
          RoundRepo.byId(Round.makeId(id, 1)) flatMap {
            case None => {
              ContestRepo.setStatus(id, Contest.Status.Canceled) >> fufail(s"can not find first round of ${contest}")
            }
            case Some(r) => roundApi.pairing(contest, r, publishScoreAndFinish)
          }
        }
      }
    }

  def setStart(id: Contest.ID): Unit =
    Sequencing(id)(ContestRepo.enterStoppedById) { contest =>
      logger.info(s"比赛开始：${contest}")
      ContestRepo.setStatus(id, Contest.Status.Started)
    }

  def setCancel(id: Contest.ID): Funit =
    ContestRepo.setStatus(id, Contest.Status.Canceled)

  def launch: Funit = {
    launchBoards.flatMap { list =>
      val gameIds = list.filter(_.canStarted).map(_.board.id)
      val now = DateTime.now
      for {
        _ <- GameRepo.gameStartBatch(gameIds, now)
        _ <- BoardRepo.gameStartBatch(gameIds, now)
      } yield {
        gameIds.foreach { gameId =>
          roundMap.tell(gameId, ContestRoundStart(now))
        }
      }
    }
  }

  def launchBoards: Fu[List[Board.FullInfo]] = for {
    boards ← BoardRepo.pending
    rounds ← boards.nonEmpty ?? { RoundRepo.byOrderedIds(boards.map(_.roundId)) }
    contests <- boards.nonEmpty ?? { ContestRepo.byOrderedIds(boards.map(_.contestId)) }
  } yield boards zip rounds zip contests map {
    case ((board, round), contest) => Board.FullInfo(board, round, contest)
  }

  def remind: Funit =
    remindBoards map { list =>
      list.foreach { info =>
        info.board.players.foreach { player =>
          if (!info.contest.isFinishedOrCanceled && (info.round.isPublished || info.round.isStarted)) {
            if (!info.board.appt || (info.board.appt && info.board.apptComplete)) {
              BoardRepo.setReminded(info.board.id) >>-
                reminder(
                  info,
                  player.userId
                )
            }
          }
        }
      }
    }

  def remindBoards: Fu[List[Board.FullInfo]] = for {
    boards ← BoardRepo.remindAtSoon
    rounds ← boards.nonEmpty ?? { RoundRepo.byOrderedIds(boards.map(_.roundId)) }
    contests <- boards.nonEmpty ?? { ContestRepo.byOrderedIds(boards.map(_.contestId)) }
  } yield boards zip rounds zip contests map {
    case ((board, round), contest) => Board.FullInfo(board, round, contest)
  }

  def setAutoPairing(contest: Contest, auto: Boolean): Funit =
    ContestRepo.setAutoPairing(contest.id, auto) flatMap { _ =>
      if (auto) {
        RoundRepo.byId(Round.makeId(contest.id, contest.currentRound)).flatMap { roundOption =>
          roundOption.?? { round =>
            val c = contest.copy(autoPairing = true)
            round.status match {
              case Round.Status.Created => (c.isEnterStopped || c.isStarted).?? { delayNextRound(c, round).map { nr => roundApi.pairing(c, nr, publishScoreAndFinish).void } }
              case Round.Status.Pairing => (c.isEnterStopped || c.isStarted).?? { delayNextRound(c, round).map { nr => roundApi.publish(c, nr) } }
              case Round.Status.Published | Round.Status.Started => funit
              case Round.Status.Finished => c.isStarted.?? {
                roundApi.publishResult(c, round.id, round.no).flatMap { nc =>
                  if (nc.allRoundFinished) {
                    publishScoreAndFinish(contest)
                  } else toNextRound(nc, c.currentRound + 1)
                }
              }
              case Round.Status.PublishResult => c.isStarted.?? {
                if (c.allRoundFinished) {
                  publishScoreAndFinish(c)
                } else toNextRound(c, c.currentRound + 1)
              }
            }
          }
        }
      } else funit
    }

  private val championTop5Cache = asyncCache.single[List[(Contest, User.ID)]](
    name = "contest.championTop5",
    f = {
      for {
        contests <- ContestRepo.findRecently(5)
        rounds <- ScoreSheetRepo.findChampion(contests.map(_.id))
      } yield {
        contests.map { contest =>
          contest -> rounds.find(_._1 == contest.id).map(_._2)
        }.map(d => d._1 -> (d._2 | "-"))
      }
    },
    expireAfter = _.ExpireAfterWrite(30 minute)
  )

  def championTop5: Fu[List[(Contest, User.ID)]] = championTop5Cache.get

  private def Sequencing(contestId: Contest.ID)(fetch: Contest.ID => Fu[Option[Contest]])(run: Contest => Funit): Unit =
    doSequence(contestId) {
      fetch(contestId) flatMap {
        case Some(t) => run(t)
        case None => fufail(s"Can't run sequenced operation on missing contest $contestId")
      }
    }

  private def doSequence(contestId: Contest.ID)(fu: => Funit): Unit =
    sequencers.tell(contestId, Duct.extra.LazyFu(() => fu))

  def uploadPicture(id: String, picture: Photographer.Uploaded, processFile: Boolean = false): Fu[DbImage] =
    photographer(id, picture, processFile)

  def uploadFile(id: String, file: FileUploader.Uploaded): Fu[DbFile] =
    fileUploader(id, file)

}
