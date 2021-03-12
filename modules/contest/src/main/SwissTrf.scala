package lila.contest

// https://www.fide.com/FIDE/handbook/C04Annex2_TRF16.pdf
final class SwissTrf(baseUrl: String) {

  private type Bits = List[(Int, String)]

  val sep = System.getProperty("line.separator")

  def apply(contest: Contest): Fu[String] = {
    for {
      rounds <- PlayerRepo.rounds(contest)
      players <- PlayerRepo.getByContest(contest.id)
      forbiddens <- ForbiddenRepo.getByContest(contest.id)
    } yield {
      tournamentLines(contest, forbiddenPairs(players, forbiddens)) concat sep concat
        rounds.map {
          case (p, m) => playerLine(contest)(p, m)
        }.map(formatLine).mkString(sep)
    }
  }

  private def forbiddenPairs(players: List[Player], forbiddens: List[Forbidden]): Set[(Player.No, Player.No)] = {
    val playerMap = players.map(p => p.id -> p.no).toMap
    forbiddens.flatMap { forbidden =>
      forbidden.pairs.map {
        case (p1, p2) => (playerMap.get(p1) err s"can not find player $p1") -> (playerMap.get(p2) err s"can not find player $p2")
      }
    }.toSet
  }

  private def tournamentLines(contest: Contest, forbiddenPairs: Set[(Player.No, Player.No)]) = {
    forbiddenPairs.map {
      case (p1, p2) => s"XXP $p1 $p2"
    } ++ Set(
      s"XXR ${contest.actualRound}"
    )
  }.mkString(sep)

  private def playerLine(contest: Contest)(p: Player, historyRoundBoardMap: Map[Round.No, Board]): Bits =
    List(
      3 -> "001",
      8 -> p.no.toString,
      47 -> p.userId,
      84 -> f"${p.points}%1.1f"
    ) ::: contest.historyRoundList.zip(p.outcomes).flatMap {
        case (rn, outcome) =>
          val boardOption = historyRoundBoardMap get rn
          List(
            95 -> boardOption.fold("0000") { board =>
              board.opponentOf(p.no).fold("0000")(_.toString)
            },
            97 -> boardOption.fold("-") { board =>
              board.colorOf(p.no).fold("-")(_.fold("w", "b"))
            },
            99 -> {
              import Board.Outcome._
              outcome match {
                case Win => "1"
                case Loss | NoStart => "0"
                case Draw => "="
                case Bye => "U"
                case Leave | Quit | Kick | ManualAbsent => "Z"
                case Half => "H"
              }
            }
          ).map { case (l, s) => (l + (rn - 1) * 10, s) }
      } ::: p.absent.?? {
        List( // http://www.rrweb.org/javafo/aum/JaVaFo2_AUM.htm#_Unusual_info_extensions
          95 -> "0000",
          97 -> "-",
          99 -> "Z"
        ).map { case (l, s) => (l + (contest.currentRound - 1) * 10, s) }
      }

  private def formatLine(bits: Bits): String =
    bits.foldLeft("") {
      case (acc, (pos, txt)) => s"""$acc${" " * (pos - txt.size - acc.size)}$txt"""
    }

  private val dateFormatter = org.joda.time.format.DateTimeFormat forStyle "M-"
}
