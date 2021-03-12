package lila.offlineContest

import ornicar.scalalib.Random

final class OffPairingDirector(pairingSystem: OffPairingSystem) {

  private[offlineContest] def roundPairingTest(contest: OffContest): Fu[Boolean] =
    pairingSystem(contest)
      .flatMap { pendings =>
        if (pendings.isEmpty) fuccess(false)
        else fuccess(true)
      }
      .recover {
        case OffPairingSystem.BBPairingException(msg, trfContent) =>
          logger.warn(s"BBPairing failed, contest: $contest, msg: " + msg + ", \n" + trfContent)
          false
      }

  private[offlineContest] def roundPairing(contest: OffContest, round: OffRound, players: List[OffPlayer]): Fu[Option[List[OffBoard]]] =
    pairingSystem(contest)
      .flatMap { pendings =>
        if (pendings.isEmpty) fuccess(none)
        else {
          val boards = buildBoards(contest, round, pendings, players)
          for {
            _ <- OffBoardRepo.bulkInsert(round.id, boards)
            _ <- setPlayerBey(contest, pendings)
            _ <- setPlayerUnAbsent(contest)
            _ <- setPlayerAbsent(contest, round, players)
            _ <- OffRoundRepo.setBoards(round.id, boards.size)
            _ <- OffRoundRepo.setStatus(round.id, OffRound.Status.Pairing)
          } yield Some(boards)
        }
      }
      .recover {
        case OffPairingSystem.BBPairingException(msg, trfContent) =>
          logger.warn(s"BBPairing failed, contest: $contest, msg: " + msg + ", \n" + trfContent)
          Some(List.empty[OffBoard])
      }

  private def buildBoards(contest: OffContest, round: OffRound, byeOrPendings: List[OffPairingSystem.ByeOrPending], players: List[OffPlayer]): List[OffBoard] = {
    byeOrPendings.zipWithIndex.collect {
      case (Right(OffPairingSystem.Pending(w, b)), i) => {
        val white = players.find(_.no == w) err s"cannot find player $w"
        val black = players.find(_.no == b) err s"cannot find player $b"
        OffBoard(
          id = Random nextString 8,
          no = i + 1,
          contestId = contest.id,
          roundId = round.id,
          roundNo = round.no,
          status = OffBoard.Status.Created,
          whitePlayer = OffBoard.MiniPlayer(white.id, white.userId, white.no, None),
          blackPlayer = OffBoard.MiniPlayer(black.id, black.userId, black.no, None)
        )
      }
    }
  }

  private def setPlayerBey(contest: OffContest, byeOrPendings: List[OffPairingSystem.ByeOrPending]): Funit = {
    val byes = byeOrPendings.collect {
      case Left(bye) => bye.player
    }
    OffPlayerRepo.setOutcomes(contest.id, byes, OffBoard.Outcome.Bye, 1)
  }

  // 上轮弃权，本轮恢复
  private def setPlayerUnAbsent(contest: OffContest): Funit = {
    OffPlayerRepo.unAbsentByContest(contest.id)
  }

  // 上轮 离开、退赛、踢出，本轮继续
  private def setPlayerAbsent(contest: OffContest, round: OffRound, players: List[OffPlayer]): Funit = {
    val absents = players.filter(p => p.absent && p.roundOutcome(round.no).isEmpty)
    val kicks = absents.filter(_.kick).map(_.no)
    OffPlayerRepo.setOutcomes(contest.id, kicks, OffBoard.Outcome.Kick, 0)
  }

}

