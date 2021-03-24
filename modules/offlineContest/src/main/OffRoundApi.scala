package lila.offlineContest

import lila.clazz.ClazzApi
import lila.notify.NotifyApi

class OffRoundApi(pairingDirector: OffPairingDirector, notifyApi: NotifyApi, clazzApi: ClazzApi, bus: lila.common.Bus) {

  def pairing(contest: OffContest, round: OffRound, finish: OffContest => Funit): Fu[Boolean] = {
    OffPlayerRepo.getByContest(contest.id) flatMap { players =>
      pairingDirector.roundPairing(contest, round, players) flatMap {
        case None => {
          logger.warn("Pairing impossible under the current rules, force finished!")
          pairingFailed(contest, round, finish).inject(false)
        }
        case Some(boards) => {
          if (boards.isEmpty) {
            logger.warn("force finished!")
            pairingFailed(contest, round, finish).inject(false)
          } else {
            logger.info(s"Pairing Finished：$round")
            fuTrue
          }
        }
      }
    }
  }

  private def pairingFailed(contest: OffContest, round: OffRound, finish: OffContest => Funit): Funit = {
    val cr = Math.max(round.no - 1, 1)
    OffContestRepo.setCurrentRound(contest.id, cr) >> finish(contest.copy(currentRound = cr))
  }

  def publish(contest: OffContest, round: OffRound): Funit = {
    logger.info(s"Pairing Published：$round")
    OffRoundRepo.setStatus(round.id, OffRound.Status.Published) >> OffBoardRepo.batchStart(round.id)
  }

  import lila.hub.actorApi.offContest._
  def publishResult(contest: OffContest, id: OffRound.ID, no: OffRound.No, finish: OffContest => Funit, playersWithUsers: OffContest => Fu[List[OffPlayer.PlayerWithUser]]): Funit = {
    logger.info(s"Result Published：$id")
    val nextRound = no + 1
    for {
      _ <- computeScore(contest, no)
      _ <- OffRoundRepo.setStatus(id, OffRound.Status.PublishResult)
      players <- playersWithUsers(contest)
      boards <- OffBoardRepo.getByRound(id)
      team <- belongTeam(contest)
      res <- {
        if (nextRound > contest.rounds) {
          finish(contest.copy(currentRound = nextRound))
        } else {
          OffContestRepo.setCurrentRound(contest.id, nextRound)
        }
      }
    } yield {

      val result = OffContestRoundResult(
        contest.id,
        contest.fullName,
        team,
        contest.teamRated,
        no,
        boards.map { board =>
          val white = findPlayer(board.whitePlayer.no, players)
          val black = findPlayer(board.blackPlayer.no, players)
          OffContestBoard(
            board.id,
            OffContestUser(white.userId, white.realNameOrUsername, board.whitePlayer.isWinner),
            OffContestUser(black.userId, black.realNameOrUsername, board.blackPlayer.isWinner)
          )
        }
      )
      bus.publish(result, 'offContestRoundResult)
    }
  }

  private def findPlayer(no: OffPlayer.No, players: List[OffPlayer.PlayerWithUser]): OffPlayer.PlayerWithUser =
    players.find(_.player.no == no) err s"can not find player：$no"

  def belongTeam(c: OffContest): Fu[Option[String]] = {
    c.typ match {
      case OffContest.Type.Public | OffContest.Type.TeamInner => fuccess(c.organizer.some)
      case OffContest.Type.ClazzInner => {
        clazzApi.byId(c.organizer).map {
          _.??(_.team)
        }
      }
    }
  }

  private def computeScore(contest: OffContest, no: OffRound.No): Funit = {
    for {
      players <- OffPlayerRepo.getByContest(contest.id)
      boards <- OffBoardRepo.getByContest(contest.id)
    } yield {
      val playerBtssScores = OffBtss.PlayerBtssScores(players.map(OffBtss.PlayerBtssScore(_)))
      val newPlayerBtssScores = contest.btsss.foldLeft(playerBtssScores) {
        case (old, btss) => btss.score(boards, old)
      }

      val scoreSheets = newPlayerBtssScores.sort.zipWithIndex.map {
        case (playerBtssScore, i) => OffScoreSheet(
          id = OffScoreSheet.makeId(playerBtssScore.player.id, no),
          contestId = playerBtssScore.player.contestId,
          roundNo = no,
          playerUid = playerBtssScore.player.userId,
          playerNo = playerBtssScore.player.no,
          score = playerBtssScore.player.score,
          rank = i + 1,
          btssScores = playerBtssScore.btsss
        )
      }
      OffScoreSheetRepo.bulkInsert(contest.id, scoreSheets)
    }
  }

  def manualAbsent(round: OffRound, joins: List[OffPlayer.ID], absents: List[OffPlayer.ID]): Funit =
    OffPlayerRepo.byIds(joins).flatMap { players =>
      players.filter(p => p.absent && p.manualAbsent && !p.absentOr && p.roundOutcome(round.no).??(_ == OffBoard.Outcome.ManualAbsent)).map { player =>
        OffPlayerRepo.update(
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
    } >> OffPlayerRepo.byIds(absents).flatMap { players =>
      players.filter(p => !p.manualAbsent && p.roundOutcome(round.no).isEmpty).map { player =>
        OffPlayerRepo.update(
          player.copy(
            absent = true,
            manualAbsent = true,
            outcomes = player.outcomes :+ OffBoard.Outcome.ManualAbsent
          ) |> { player =>
            player.copy(
              score = player.allScore,
              points = player.allScore
            )
          }
        )
      }.sequenceFu.void
    }

  def manualResult(board: OffBoard, result: String): Funit =
    for {
      whiteOption <- OffPlayerRepo.byId(board.whitePlayer.id)
      blackOption <- OffPlayerRepo.byId(board.blackPlayer.id)
    } yield (whiteOption |@| blackOption).tupled ?? {
      case (white, black) => {
        val whiteOutcome = result match {
          case "1" => OffBoard.Outcome.Win
          case "0" => OffBoard.Outcome.Loss
          case "-" => OffBoard.Outcome.Draw
        }

        val blackOutcome = result match {
          case "1" => OffBoard.Outcome.Loss
          case "0" => OffBoard.Outcome.Win
          case "-" => OffBoard.Outcome.Draw
        }

        val winner = result match {
          case "1" => chess.Color.White.some
          case "0" => chess.Color.Black.some
          case "-" => None
        }

        val newWhite = white.manualResult(board.roundNo, whiteOutcome)
        val newBlack = black.manualResult(board.roundNo, blackOutcome)
        OffPlayerRepo.update(newWhite) >>
          OffPlayerRepo.update(newBlack) >>
          OffBoardRepo.setWinner(board.id, winner)
      }
    }

  def manualPairing(data: ManualPairing, round: OffRound): Funit =
    if (!data.source.isBye_ && !data.target.isBye_) {
      for {
        sourceBoardOption <- data.source.board.??(OffBoardRepo.byId _)
        targetBoardOption <- data.target.board.??(OffBoardRepo.byId _)
      } yield {
        val sourceBoard = sourceBoardOption.err(s"can find board ${data.source.board_}")
        val targetBoard = targetBoardOption.err(s"can find board ${data.target.board_}")
        val sourceWhite = data.source.color_ == 1
        val targetWhite = data.target.color_ == 1
        val sourcePlayer = sourceBoard.player(chess.Color(sourceWhite))
        val targetPlayer = targetBoard.player(chess.Color(targetWhite))
        if (sourceBoard.is(targetBoard)) {
          OffBoardRepo.update(
            sourceBoard.copy(
              whitePlayer = targetPlayer,
              blackPlayer = sourcePlayer
            )
          )
        } else {
          val b1 = if (sourceWhite) sourceBoard.copy(whitePlayer = targetPlayer) else sourceBoard.copy(blackPlayer = targetPlayer)
          val b2 = if (targetWhite) targetBoard.copy(whitePlayer = sourcePlayer) else targetBoard.copy(blackPlayer = sourcePlayer)
          OffBoardRepo.update(b1) >> OffBoardRepo.update(b2)
        }
      }
    } else if (data.source.isBye_ && !data.target.isBye_) {
      for {
        sourcePlayerOption <- data.source.player.??(OffPlayerRepo.byId _)
        targetBoardOption <- data.target.board.??(OffBoardRepo.byId _)
        targetPlayerOption <- targetBoardOption.??(b => OffPlayerRepo.byId(b.player(chess.Color(data.target.color_ == 1)).id))
      } yield {
        val sourcePlayer = sourcePlayerOption.err(s"can find board ${data.source.board_}")
        val targetPlayer = targetPlayerOption.err(s"can find board ${data.source.board_}")
        val targetBoard = targetBoardOption.err(s"can find board ${data.target.board_}")
        val targetWhite = data.target.color_ == 1
        val newTargetBoard =
          if (targetWhite) {
            targetBoard.copy(whitePlayer = OffBoard.MiniPlayer(sourcePlayer.id, sourcePlayer.userId, sourcePlayer.no, None))
          } else targetBoard.copy(blackPlayer = OffBoard.MiniPlayer(sourcePlayer.id, sourcePlayer.userId, sourcePlayer.no, None))

        OffPlayerRepo.update(
          sourcePlayer.copy(
            outcomes = sourcePlayer.outcomes.dropRight(1)
          ) |> { player =>
              player.copy(
                score = player.allScore,
                points = player.allScore
              )
            }
        ) >> OffPlayerRepo.update(
            targetPlayer.copy(
              outcomes = targetPlayer.outcomes :+ OffBoard.Outcome.Bye
            ) |> { player =>
              player.copy(
                score = player.allScore,
                points = player.allScore
              )
            }
          ) >> OffBoardRepo.update(newTargetBoard)
      }
    } else if (!data.source.isBye_ && data.target.isBye_) {
      for {
        sourceBoardOption <- data.source.board.??(OffBoardRepo.byId(_))
        sourcePlayerOption <- sourceBoardOption.??(b => OffPlayerRepo.byId(b.player(chess.Color(data.source.color_ == 1)).id))
        targetPlayerOption <- data.target.player.??(OffPlayerRepo.byId(_))
      } yield {
        val sourcePlayer = sourcePlayerOption.err(s"can find board ${data.source.board_}")
        val targetPlayer = targetPlayerOption.err(s"can find board ${data.source.board_}")
        val sourceBoard = sourceBoardOption.err(s"can find board ${data.target.board_}")
        val sourceWhite = data.source.color_ == 1
        val newSourceBoard =
          if (sourceWhite) {
            sourceBoard.copy(whitePlayer = OffBoard.MiniPlayer(targetPlayer.id, targetPlayer.userId, targetPlayer.no, None))
          } else sourceBoard.copy(blackPlayer = OffBoard.MiniPlayer(targetPlayer.id, targetPlayer.userId, targetPlayer.no, None))

        OffPlayerRepo.update(
          sourcePlayer.copy(
            outcomes = sourcePlayer.outcomes :+ OffBoard.Outcome.Bye
          ) |> { player =>
            player.copy(
              score = player.allScore,
              points = player.allScore
            )
          }
        ) >> OffPlayerRepo.update(
            targetPlayer.copy(
              outcomes = targetPlayer.outcomes.dropRight(1)
            ) |> { player =>
                player.copy(
                  score = player.allScore,
                  points = player.allScore
                )
              }
          ) >> OffBoardRepo.update(newSourceBoard)
      }
    } else funit
}
