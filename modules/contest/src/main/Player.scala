package lila.contest

import lila.common.LightUser
import lila.game.Game
import org.joda.time.DateTime
import lila.rating.Perf
import lila.user.{ Perfs, User }
import play.api.libs.json.Json

case class Player(
    id: Player.ID,
    no: Player.No,
    contestId: Contest.ID,
    userId: User.ID,
    rating: Int,
    provisional: Boolean,
    teamRating: Option[Int], // 俱乐部等级分
    score: Double = 0,
    points: Double = 0,
    absent: Boolean = false, // 不参与匹配
    leave: Boolean = false, // 不移动导致退赛
    quit: Boolean = false, // 棋手主动退赛
    kick: Boolean = false, // 管理员在比赛未开始时退赛
    manualAbsent: Boolean = false, // 管理员设置本轮弃权,当本轮匹配完之后设置=true
    cancelled: Boolean = false,
    outcomes: List[Board.Outcome] = List.empty,
    external: Boolean = false, // 临时用户
    entryTime: DateTime
) {

  def is(uid: User.ID): Boolean = uid == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: Player): Boolean = is(other.userId)
  def absentAnd = leave && quit && kick
  def absentOr = leave || quit || kick

  // 不包含本轮的累计得分
  def roundScore(rn: Round.No): Double =
    outcomes.zipWithIndex.foldLeft(0.0) {
      case (s, (o, i)) => if (i < rn - 1) s + scoreByOutcome(o) else s
    }

  def allScore: Double =
    outcomes.foldLeft(0.0) {
      case (s, o) => s + scoreByOutcome(o)
    }

  // 获得本轮结果
  def roundOutcome(rn: Round.No): Option[Board.Outcome] =
    outcomes.zipWithIndex.find(_._2 == rn - 1).map(_._1)

  def roundOutcomeFormat(rn: Round.No): String = {
    import Board.Outcome._
    roundOutcome(rn).map {
      case Win => "1-0"
      case Loss => "0-1"
      case Draw => "1/2-1/2"
      case Bye => "轮空"
      case NoStart => "未移动"
      case Leave => "退赛"
      case Quit => "退赛"
      case Kick => "退赛"
      case ManualAbsent => "弃权"
      case Half => "半分轮空"
    } | "-"
  }

  def roundOutcomeSort(rn: Round.No): Int = {
    import Board.Outcome._
    roundOutcome(rn).map {
      case Win => 100
      case Loss => 90
      case Draw => 80
      case NoStart => 70
      case Bye => 60
      case Half => 50
      case ManualAbsent => 40
      case Leave => 30
      case Quit => 20
      case Kick => 10
    } | 0
  }

  def scoreByOutcome(o: Board.Outcome): Double = {
    import Board.Outcome._
    o match {
      case Win => 1.0
      case Loss => 0.0
      case Draw => 0.5
      case Bye => 1.0
      case NoStart => 0.0
      case Leave => 0.0
      case Quit => 0.0
      case Kick => 0.0
      case ManualAbsent => 0.0
      case Half => 0.5
    }
  }

  def isBye(rn: Round.No): Boolean =
    outcomes.zipWithIndex.exists {
      case (o, i) => rn == i + 1 && o == Board.Outcome.Bye
    }

  def isAbsent(rn: Round.No): Boolean =
    outcomes.zipWithIndex.exists {
      case (o, i) => rn == i + 1 && (o == Board.Outcome.Leave || o == Board.Outcome.Quit || o == Board.Outcome.Kick || o == Board.Outcome.ManualAbsent)
    }

  def isHalf(rn: Round.No): Boolean =
    outcomes.zipWithIndex.exists {
      case (o, i) => rn == i + 1 && o == Board.Outcome.Half
    }

  def noBoard(rn: Round.No): Boolean = isAbsent(rn) || isHalf(rn) || isBye(rn)

  def pointsFor(game: Game): Double = {
    game.winner match {
      case None => 0.5
      case Some(p) => {
        p.userId.??(_ == userId) match {
          case true => 1.0
          case false => 0.0
        }
      }
    }
  }

  def outcome(game: Game): Board.Outcome = {
    game.winner match {
      case None => Board.Outcome.Draw
      case Some(p) => {
        p.userId.??(_ == userId) match {
          case true => Board.Outcome.Win
          case false => game.playerWhoDidNotMove match {
            case None => Board.Outcome.Loss
            case Some(wdm) => if (wdm.userId.has(userId)) Board.Outcome.NoStart else Board.Outcome.Loss
          }
        }
      }
    }
  }

  def finish(game: Game, canQuitNumber: Int): Player = {
    val p = pointsFor(game)
    copy(
      score = score + p,
      points = points + p,
      outcomes = outcomes :+ outcome(game)
    ) |> { player =>
      val noStartCount = player.outcomes.count(_ == Board.Outcome.NoStart)
      player.copy(
        leave = noStartCount != 0 && noStartCount >= canQuitNumber,
        absent = player.absent || (noStartCount != 0 && noStartCount >= canQuitNumber)
      )
    }
  }

  def manualResult(rn: Round.No, newOutcome: Board.Outcome): Player = {
    val oldOutcome = roundOutcome(rn) err s"can not find outcome of round $contestId - $rn"
    val oldScore = scoreByOutcome(oldOutcome)
    val newScore = scoreByOutcome(newOutcome)

    copy(
      score = score - oldScore + newScore,
      points = points - oldScore + newScore,
      outcomes = outcomes.zipWithIndex.map {
        case (o, i) => if (i == rn - 1) newOutcome else o
      }
    ) |> { player =>
        player.copy(
          score = player.allScore,
          points = player.allScore
        )
      }
  }

}

object Player {

  type ID = String
  type No = Int

  /*  private[contest] def make(
    contestId: Contest.ID,
    no: Player.No,
    user: User,
    perfLens: Perfs => Perf,
    teamRating: Option[Int],
    external: Boolean = false
  ): Player = new Player(
    id = makeId(contestId, user.id),
    no = no,
    contestId = contestId,
    userId = user.id,
    rating = perfLens(user.perfs).intRating,
    provisional = perfLens(user.perfs).provisional,
    teamRating = teamRating,
    external = external,
    entryTime = DateTime.now
  )*/

  private[contest] def make(
    contestId: Contest.ID,
    no: Player.No,
    user: User,
    perfLens: Perfs => Perf,
    teamRating: Option[Int],
    external: Boolean = false,
    currentRound: Int,
    currentRoundOverPairing: Boolean
  ): Player = new Player(
    id = makeId(contestId, user.id),
    no = no,
    contestId = contestId,
    userId = user.id,
    rating = perfLens(user.perfs).intRating,
    provisional = perfLens(user.perfs).provisional,
    teamRating = teamRating,
    score = if (currentRoundOverPairing) currentRound * 0.5 else (currentRound - 1) * 0.5,
    points = if (currentRoundOverPairing) currentRound * 0.5 else (currentRound - 1) * 0.5,
    outcomes = if (currentRoundOverPairing) {
      (1 to currentRound).map { _ => Board.Outcome.Half }.toList
    } else {
      if (currentRound == 1) List.empty[Board.Outcome]
      else (1 to (currentRound - 1)).map { _ => Board.Outcome.Half }.toList
    },
    external = external,
    entryTime = DateTime.now
  )

  private[contest] def makeId(contestId: String, userId: String) = userId + "@" + contestId

  def toMap(players: List[Player]): Map[Player.No, Player] =
    players.map(p => p.no -> p).toMap

}

case class PlayerWithUser(player: Player, user: User) {
  def no = player.no
  def playerId = player.id
  def userId = user.id
  def username = user.username
  def profile = user.profileOrDefault

  def json = {
    Json.obj(
      "id" -> userId,
      "name" -> user.realNameOrUsername,
      "teamRating" -> player.teamRating,
      "level" -> user.profileOrDefault.currentLevel.level,
      "sex" -> user.profileOrDefault.sex
    )
  }
}

case class PlayerResult(player: Player, lightUser: LightUser, rank: Int)
