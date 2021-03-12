package lila.contest

import akka.actor.ActorSystem
import chess.{ Black, Color, White }
import lila.contest.actorApi.{ ContestBoardSetTime, ContestRoundPublish }
import lila.game.{ Game, GameRepo }
import lila.hub.{ Duct, DuctMap }
import lila.notify.{ Notification, NotifyApi }
import lila.notify.Notification.Notifies
import org.joda.time.DateTime
import lila.hub.actorApi.calendar.{ CalendarCreate, CalendarsCreate, CalendarsRemove }

class RoundApi(
    system: ActorSystem,
    sequencers: DuctMap[_],
    roundMap: DuctMap[_],
    notifyApi: NotifyApi,
    pairingDirector: PairingDirector
) {

  private val bus = system.lilaBus

  def pairing(contest: Contest, round: Round, publishScoreAndFinish: Contest => Funit): Fu[Boolean] = {
    PlayerRepo.getByContest(contest.id) flatMap { players =>
      pairingDirector.roundPairing(contest, round, players) flatMap {
        case None => {
          logger.warn("Pairing impossible under the current rules, force finished!")
          pairingFailed(contest, round, publishScoreAndFinish).inject(false)
        }
        case Some(boards) => {
          if (boards.isEmpty) {
            logger.warn("force finished!")
            pairingFailed(contest, round, publishScoreAndFinish).inject(false)
          } else {
            logger.info(s"Pairing Finished：${round}")
            contest.autoPairing.?? {
              logger.info(s"Pairing Published：${round}")
              addGames(contest, round, boards, players) >>
                RoundRepo.setStatus(round.id, Round.Status.Published) >>- {
                  bus.publish(ContestRoundPublish(contest, round), 'contestRoundPublish)
                  publishCalendar(contest, boards)
                }
            }.inject(true)
          }
        }
      }
    }
  }

  private def pairingFailed(contest: Contest, round: Round, publishScoreAndFinish: Contest => Funit): Funit = {
    ContestRepo.setCurrentRound(contest.id, Math.max(round.no - 1, 1)) >> ContestRepo.setAllRoundFinished(contest.id) >> contest.autoPairing.?? {
      publishScoreAndFinish(contest)
    }
  }

  def publish(contest: Contest, round: Round): Funit = {
    logger.info(s"Pairing Published：${round}")
    for {
      boards <- BoardRepo.getByRound(round.id)
      players <- PlayerRepo.getByContest(contest.id)
      _ <- addGames(contest, round, boards, players)
      _ <- RoundRepo.setStatus(round.id, Round.Status.Published)
    } yield {
      bus.publish(ContestRoundPublish(contest, round), 'contestRoundPublish)
      publishCalendar(contest, boards)
    }
  }

  private def addGames(contest: Contest, round: Round, boards: List[Board], players: List[Player]): Funit = {
    //logger.info(s"addGames start contest：${contest}, round：${round}, boards：${boards.size}, players：${players.size}")
    val playerMap = Player.toMap(players)
    val games = boards.map(makeGame(contest, round, playerMap))
    //logger.info(s"addGames over contest：${contest}, round：${round}, boards：${boards.size}, players：${players.size}, games：${games.size}")
    lila.common.Future.applySequentially(games) { game =>
      GameRepo.insertDenormalized(game)
    }
  }

  private def makeGame(contest: Contest, round: Round, players: Map[Player.No, Player])(board: Board): Game = {
    //logger.info(s"makeGame start contest：${contest}, round：${round}, board：${board.id}")
    val game = Game.make(
      chess = chess.Game(
        variantOption = Some {
          if (contest.position.initial) chess.variant.Standard
          else chess.variant.FromPosition
        },
        fen = contest.position.fen.some
      ) |> { g =>
          val turns = g.player.fold(0, 1)
          g.copy(
            clock = contest.clock.toClock.some,
            turns = turns,
            startedAtTurn = turns
          )
        },
      whitePlayer = makePlayer(White, players get board.whitePlayer.no err s"Missing board white $board"),
      blackPlayer = makePlayer(Black, players get board.blackPlayer.no err s"Missing board black $board"),
      mode = chess.Mode(contest.mode.rated),
      source = lila.game.Source.Contest,
      pgnImport = None,
      movedAt = Some {
        if (contest.appt) { round.actualStartsAt.plusMinutes(contest.roundSpace).minusMinutes(contest.apptDeadline | 0) } else round.actualStartsAt
      }
    )
      .withId(board.gameId)
      .withContestId(contest.id)
      .withContestCanLateMinutes(contest.canLateMinute)
      .withAppt(contest.appt)
    //logger.info(s"makeGame over contest：${contest}, round：${round}, board：${board.id}, game：${game}")
    game
  }

  private def makePlayer(color: Color, player: Player): lila.game.Player =
    lila.game.Player.make(color, player.userId, player.rating, player.provisional)

  def publishResult(contest: Contest, id: Round.ID, no: Round.No): Fu[Contest] = {
    logger.info(s"Result Published：$id")
    for {
      _ <- computeScore(contest, no)
      _ <- RoundRepo.setStatus(id, Round.Status.PublishResult)
      contest <- if (contest.isAllRoundFinished) {
        ContestRepo.setAllRoundFinished(contest.id) inject contest.copy(allRoundFinished = true)
      } else fuccess(contest)
      contest <- if (!contest.isAllRoundFinished) {
        ContestRepo.setCurrentRound(contest.id, no + 1) inject contest.copy(currentRound = no + 1)
      } else fuccess(contest.copy(currentRound = no + 1))
    } yield contest
  }

  def isContestFinished(contest: Contest): Fu[Boolean] =
    if (contest.currentRound >= contest.actualRound) fuccess(true)
    else pairingDirector.roundPairingTest(contest).map(!_)

  private def computeScore(contest: Contest, no: Round.No): Funit = {
    for {
      players <- PlayerRepo.getByContest(contest.id)
      boards <- BoardRepo.getByContest(contest.id)
    } yield {
      val playerBtssScores = Btss.PlayerBtssScores(players.map(Btss.PlayerBtssScore(_)))
      val newPlayerBtssScores = contest.btsss.foldLeft(playerBtssScores) {
        case (old, btss) => btss.score(boards, old)
      }

      val scoreSheets = newPlayerBtssScores.sort.zipWithIndex.map {
        case (playerBtssScore, i) => ScoreSheet(
          id = ScoreSheet.makeId(playerBtssScore.player.id, no),
          contestId = playerBtssScore.player.contestId,
          roundNo = no,
          playerUid = playerBtssScore.player.userId,
          playerNo = playerBtssScore.player.no,
          score = playerBtssScore.player.score,
          rank = i + 1,
          btssScores = playerBtssScore.btsss,
          cancelled = playerBtssScore.player.cancelled
        )
      }
      ScoreSheetRepo.insertMany(contest.id, scoreSheets)
    }
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

  def start: Funit =
    RoundRepo.published map { rounds =>
      rounds foreach { round =>
        if (round.shouldStart) {
          ContestRepo.byId(round.contestId) foreach {
            _.foreach(contest =>
              if (contest.isStarted) {
                setStart(contest, round.id)
              })
          }
        }
      }
    }

  def setStart(contest: Contest, id: Round.ID): Unit =
    Sequencing(id)(RoundRepo.publishedById) { round =>
      logger.info(s"第${round.id}轮 - 开始")
      RoundRepo.setStatus(round.id, Round.Status.Started)
    }

  def manualAbsent(round: Round, joins: List[Player.ID], absents: List[Player.ID]): Funit =
    PlayerRepo.byIds(joins).flatMap { players =>
      players.filter(p => p.absent && p.manualAbsent && !p.absentOr && p.roundOutcome(round.no).??(_ == Board.Outcome.ManualAbsent)).map { player =>
        PlayerRepo.update(
          player.copy(
            absent = false,
            manualAbsent = false,
            outcomes = player.outcomes.dropRight(1)
          ) |> { player =>
              player.copy(
                score = player.allScore,
                points = player.allScore
              )
            }
        )
      }.sequenceFu.void
    } >> PlayerRepo.byIds(absents).flatMap { players =>
      players.filter(p => !p.manualAbsent && p.roundOutcome(round.no).isEmpty).map { player =>
        PlayerRepo.update(
          player.copy(
            absent = true,
            manualAbsent = true,
            outcomes = player.outcomes :+ Board.Outcome.ManualAbsent
          ) |> { player =>
            player.copy(
              score = player.allScore,
              points = player.allScore
            )
          }
        )
      }.sequenceFu.void
    }

  def manualPairing(data: ManualPairing, round: Round): Funit =
    if (!data.source.isBye_ && !data.target.isBye_) {
      for {
        sourceBoardOption <- data.source.board.??(BoardRepo.byId(_))
        targetBoardOption <- data.target.board.??(BoardRepo.byId(_))
      } yield {
        val sourceBoard = sourceBoardOption.err(s"can find board ${data.source.board_}")
        val targetBoard = targetBoardOption.err(s"can find board ${data.target.board_}")
        val sourceWhite = data.source.color_ == 1
        val targetWhite = data.target.color_ == 1
        val sourcePlayer = sourceBoard.player(chess.Color(sourceWhite))
        val targetPlayer = targetBoard.player(chess.Color(targetWhite))
        if (sourceBoard.is(targetBoard)) {
          BoardRepo.update(
            sourceBoard.copy(
              whitePlayer = targetPlayer,
              blackPlayer = sourcePlayer
            )
          )
        } else {
          val b1 = if (sourceWhite) sourceBoard.copy(whitePlayer = targetPlayer) else sourceBoard.copy(blackPlayer = targetPlayer)
          val b2 = if (targetWhite) targetBoard.copy(whitePlayer = sourcePlayer) else targetBoard.copy(blackPlayer = sourcePlayer)
          BoardRepo.update(b1) >> BoardRepo.update(b2)
        }
      }
    } else if (data.source.isBye_ && !data.target.isBye_) {
      for {
        sourcePlayerOption <- data.source.player.??(PlayerRepo.byId(_))
        targetBoardOption <- data.target.board.??(BoardRepo.byId(_))
        targetPlayerOption <- targetBoardOption.??(b => PlayerRepo.byId(b.player(chess.Color(data.target.color_ == 1)).id))
      } yield {
        val sourcePlayer = sourcePlayerOption.err(s"can find board ${data.source.board_}")
        val targetPlayer = targetPlayerOption.err(s"can find board ${data.source.board_}")
        val targetBoard = targetBoardOption.err(s"can find board ${data.target.board_}")
        val targetWhite = data.target.color_ == 1
        val newTargetBoard =
          if (targetWhite) {
            targetBoard.copy(whitePlayer = Board.MiniPlayer(sourcePlayer.id, sourcePlayer.userId, sourcePlayer.no, None))
          } else targetBoard.copy(blackPlayer = Board.MiniPlayer(sourcePlayer.id, sourcePlayer.userId, sourcePlayer.no, None))

        PlayerRepo.update(
          sourcePlayer.copy(
            outcomes = sourcePlayer.outcomes.dropRight(1)
          ) |> { player =>
              player.copy(
                score = player.allScore,
                points = player.allScore
              )
            }
        ) >> PlayerRepo.update(
            targetPlayer.copy(
              outcomes = targetPlayer.outcomes :+ Board.Outcome.Bye
            ) |> { player =>
              player.copy(
                score = player.allScore,
                points = player.allScore
              )
            }
          ) >> BoardRepo.update(newTargetBoard)
      }
    } else if (!data.source.isBye_ && data.target.isBye_) {
      for {
        sourceBoardOption <- data.source.board.??(BoardRepo.byId(_))
        sourcePlayerOption <- sourceBoardOption.??(b => PlayerRepo.byId(b.player(chess.Color(data.source.color_ == 1)).id))
        targetPlayerOption <- data.target.player.??(PlayerRepo.byId(_))
      } yield {
        val sourcePlayer = sourcePlayerOption.err(s"can find board ${data.source.board_}")
        val targetPlayer = targetPlayerOption.err(s"can find board ${data.source.board_}")
        val sourceBoard = sourceBoardOption.err(s"can find board ${data.target.board_}")
        val sourceWhite = data.source.color_ == 1
        val newSourceBoard =
          if (sourceWhite) {
            sourceBoard.copy(whitePlayer = Board.MiniPlayer(targetPlayer.id, targetPlayer.userId, targetPlayer.no, None))
          } else sourceBoard.copy(blackPlayer = Board.MiniPlayer(targetPlayer.id, targetPlayer.userId, targetPlayer.no, None))

        PlayerRepo.update(
          sourcePlayer.copy(
            outcomes = sourcePlayer.outcomes :+ Board.Outcome.Bye
          ) |> { player =>
            player.copy(
              score = player.allScore,
              points = player.allScore
            )
          }
        ) >> PlayerRepo.update(
            targetPlayer.copy(
              outcomes = targetPlayer.outcomes.dropRight(1)
            ) |> { player =>
                player.copy(
                  score = player.allScore,
                  points = player.allScore
                )
              }
          ) >> BoardRepo.update(newSourceBoard)
      }
    } else funit

  def manualResult(board: Board, result: String): Funit =
    for {
      whiteOption <- PlayerRepo.byId(board.whitePlayer.id)
      blackOption <- PlayerRepo.byId(board.blackPlayer.id)
    } yield (whiteOption |@| blackOption).tupled ?? {
      case (white, black) => {
        val whiteOutcome = result match {
          case "1" => Board.Outcome.Win
          case "0" => Board.Outcome.Loss
          case "-" => Board.Outcome.Draw
        }

        val blackOutcome = result match {
          case "1" => Board.Outcome.Loss
          case "0" => Board.Outcome.Win
          case "-" => Board.Outcome.Draw
        }

        val winner = result match {
          case "1" => chess.Color.White.some
          case "0" => chess.Color.Black.some
          case "-" => None
        }

        val newWhite = white.manualResult(board.roundNo, whiteOutcome)
        val newBlack = black.manualResult(board.roundNo, blackOutcome)
        PlayerRepo.update(newWhite) >>
          PlayerRepo.update(newBlack) >>
          BoardRepo.setWinner(board.id, winner)
      }
    }

  def setStartsTime(contest: Contest, id: Round.ID, st: DateTime): Funit =
    RoundRepo.setStartsTime(id, st) >> (!contest.appt).??(BoardRepo.setStartsTimeByRound(id, st))

  def setBoardTime(contest: Contest, round: Round, board: Board, st: DateTime): Funit =
    BoardRepo.apptComplete(board.id, st) >> GameRepo.apptComplete(board.id, st) >>- {
      bus.publish(ContestBoardSetTime(contest, board, st), 'contestBoardSetTime)
      resetCalendar(contest, board)
    }

  def apptComplete(gameId: String, time: DateTime): Funit =
    BoardRepo.apptComplete(gameId, time) >> GameRepo.apptComplete(gameId, time)

  private def Sequencing(id: Round.ID)(fetch: Round.ID => Fu[Option[Round]])(run: Round => Funit): Unit =
    doSequence(id) {
      fetch(id) flatMap {
        case Some(t) => run(t)
        case None => fufail(s"Can't run sequenced operation on missing contest round $id")
      }
    }

  private def doSequence(id: Round.ID)(fu: => Funit): Unit =
    sequencers.tell(id, Duct.extra.LazyFu(() => fu))

  private def resetCalendar(contest: Contest, board: Board) = {
    if (!contest.appt) {
      val ids = board.players.map { player => s"${board.id}@${player.userId}" }
      val calendars = makeCalendar(contest, board)
      bus.publish(CalendarsRemove(ids), 'calendarRemoveBus)
      bus.publish(CalendarsCreate(calendars), 'calendarCreateBus)
    }
  }

  private def publishCalendar(contest: Contest, boards: Boards): Unit = {
    if (!contest.appt) {
      val calendars = boards.foldLeft(List.empty[CalendarCreate]) {
        case (lst, b) => lst ++ makeCalendar(contest, b)
      }
      bus.publish(CalendarsCreate(calendars), 'calendarCreateBus)
    }
  }

  private def makeCalendar(contest: Contest, board: Board): List[CalendarCreate] = {
    board.players.map { player =>
      CalendarCreate(
        id = s"${board.id}@${player.userId}".some,
        typ = "contest",
        user = player.userId,
        sdt = board.startsAt,
        edt = board.startsAt.plusMinutes(contest.roundSpace),
        content = s"${contest.name} 第${board.roundNo}轮 #${board.no}",
        onlySdt = false,
        link = s"/${board.id}".some,
        icon = "奖".some,
        bg = "#ae8300".some
      )
    }

  }

}
