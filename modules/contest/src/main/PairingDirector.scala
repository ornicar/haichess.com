package lila.contest

import lila.game.IdGenerator

final class PairingDirector(pairingSystem: PairingSystem) {

  private[contest] def roundPairingTest(contest: Contest): Fu[Boolean] =
    pairingSystem(contest)
      .flatMap { pendings =>
        if (pendings.isEmpty) fuccess(false)
        else fuccess(true)
      }
      .recover {
        case PairingSystem.BBPairingException(msg, trfContent) =>
          logger.warn(s"BBPairing failed, contest: ${contest}, msg: " + msg + ", \n" + trfContent)
          false
      }

  private[contest] def roundPairing(contest: Contest, round: Round, players: List[Player]): Fu[Option[List[Board]]] =
    pairingSystem(contest)
      .flatMap { pendings =>
        if (pendings.isEmpty) fuccess(none)
        else {
          for {
            boards <- boards(contest, round, pendings, players)
            _ <- BoardRepo.insertMany(round.id, boards)
            _ <- setPlayerBey(contest, pendings)
            //_ <- setPlayerAbsent(contest, round: Round, players)
            _ <- setPlayerUnAbsent(contest)
            _ <- RoundRepo.setBoards(round.id, boards.size)
            _ <- RoundRepo.setStatus(round.id, Round.Status.Pairing)
          } yield Some(boards)
        }
      }
      .recover {
        case PairingSystem.BBPairingException(msg, trfContent) =>
          logger.warn(s"BBPairing failed, contest: ${contest}, msg: " + msg + ", \n" + trfContent)
          Some(List.empty[Board])
      }

  private def boards(contest: Contest, round: Round, byeOrPendings: List[PairingSystem.ByeOrPending], players: List[Player]): Fu[List[Board]] = {
    byeOrPendings.zipWithIndex.collect {
      case (Right(PairingSystem.Pending(w, b)), i) =>
        IdGenerator.game dmap { id =>
          val white = players.find(_.no == w) err s"cannot find player $w"
          val black = players.find(_.no == b) err s"cannot find player $b"
          Board(
            id = id,
            no = i + 1,
            contestId = contest.id,
            roundId = round.id,
            roundNo = round.no,
            status = chess.Status.Created,
            whitePlayer = Board.MiniPlayer(white.id, white.userId, white.no, None),
            blackPlayer = Board.MiniPlayer(black.id, black.userId, black.no, None),
            startsAt = if (contest.appt) { round.actualStartsAt.plusMinutes(contest.roundSpace).minusMinutes(contest.apptDeadline | 0) } else round.actualStartsAt,
            appt = contest.appt
          )
        }
    }.sequenceFu
  }

  private def setPlayerBey(contest: Contest, byeOrPendings: List[PairingSystem.ByeOrPending]): Funit = {
    val byes = byeOrPendings.collect {
      case Left(bye) => bye.player
    }
    PlayerRepo.setOutcomes(contest.id, byes, Board.Outcome.Bye, 1)
  }

  /*  // 上轮 离开、退赛、踢出，本轮继续
  private def setPlayerAbsent(contest: Contest, round: Round, players: List[Player]): Funit = {
    val absents = players.filter(p => p.absent && p.roundOutcome(round.no).isEmpty)
    val leaves = absents.filter(_.leave).map(_.no)
    val quits = absents.filter(_.quit).map(_.no)
    val kicks = absents.filter(_.kick).map(_.no)
    PlayerRepo.setOutcomes(contest.id, leaves, Board.Outcome.Leave, 0) >>
      PlayerRepo.setOutcomes(contest.id, quits, Board.Outcome.Quit, 0) >>
      PlayerRepo.setOutcomes(contest.id, kicks, Board.Outcome.Kick, 0)
  }*/

  // 上轮弃权，本轮恢复
  private def setPlayerUnAbsent(contest: Contest): Funit = {
    PlayerRepo.unAbsentByContest(contest.id)
  }

}

