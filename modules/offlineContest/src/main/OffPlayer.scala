package lila.offlineContest

import lila.user.User
import lila.common.LightUser
import play.api.libs.json.Json

case class OffPlayer(
    id: OffPlayer.ID,
    no: OffPlayer.No,
    contestId: OffContest.ID,
    userId: User.ID,
    teamRating: Option[Int], // 俱乐部等级分
    score: Double = 0, // 页面显示
    points: Double = 0, // 匹配使用
    absent: Boolean = false, // 不参与匹配
    kick: Boolean = false, // 管理员在比赛未开始时退赛
    manualAbsent: Boolean = false, // 管理员设置本轮弃权,当本轮匹配完之后设置=true
    outcomes: List[OffBoard.Outcome] = List.empty,
    external: Boolean = false // 临时用户
) {

  def is(uid: User.ID): Boolean = uid == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: OffPlayer): Boolean = is(other.userId)

  def absentOr = kick

  def virtualUser = User.virtual(userId, userId.replace(OffPlayer.externalSymbol, ""))

  // 不包含本轮的累计得分
  def roundScore(rn: OffRound.No): Double =
    outcomes.zipWithIndex.foldLeft(0.0) {
      case (s, (o, i)) => if (i < rn - 1) s + scoreByOutcome(o) else s
    }

  def allScore: Double =
    outcomes.foldLeft(0.0) {
      case (s, o) => s + scoreByOutcome(o)
    }

  // 获得本轮结果
  def roundOutcome(rn: OffRound.No): Option[OffBoard.Outcome] =
    outcomes.zipWithIndex.find(_._2 == rn - 1).map(_._1)

  def roundOutcomeFormat(rn: OffRound.No): String = {
    import OffBoard.Outcome._
    roundOutcome(rn).map {
      case Win => "1-0"
      case Loss => "0-1"
      case Draw => "1/2-1/2"
      case Bye => "轮空"
      case NoStart => "未移动"
      case Kick => "退赛"
      case ManualAbsent => "弃权"
      case Half => "半分轮空"
    } | "-"
  }

  def roundOutcomeSort(rn: OffRound.No): Int = {
    import OffBoard.Outcome._
    roundOutcome(rn).map {
      case Win => 100
      case Loss => 90
      case Draw => 80
      case NoStart => 70
      case Bye => 60
      case Half => 50
      case ManualAbsent => 40
      case Kick => 10
    } | 0
  }

  def scoreByOutcome(o: OffBoard.Outcome): Double = {
    import OffBoard.Outcome._
    o match {
      case Win => 1.0
      case Loss => 0.0
      case Draw => 0.5
      case Bye => 1.0
      case NoStart => 0.0
      case Kick => 0.0
      case ManualAbsent => 0.0
      case Half => 0.5
    }
  }

  def isBye(rn: OffRound.No): Boolean =
    outcomes.zipWithIndex.exists {
      case (o, i) => rn == i + 1 && o == OffBoard.Outcome.Bye
    }

  def isAbsent(rn: OffRound.No): Boolean =
    outcomes.zipWithIndex.exists {
      case (o, i) => rn == i + 1 && (o == OffBoard.Outcome.Kick || o == OffBoard.Outcome.ManualAbsent)
    }

  def isHalf(rn: OffRound.No): Boolean =
    outcomes.zipWithIndex.exists {
      case (o, i) => rn == i + 1 && o == OffBoard.Outcome.Half
    }

  def noBoard(rn: OffRound.No): Boolean = isAbsent(rn) || isHalf(rn) || isBye(rn)

  def manualResult(rn: OffRound.No, newOutcome: OffBoard.Outcome): OffPlayer = {
    val oldOutcome = roundOutcome(rn)
    val oldScore = oldOutcome.??(scoreByOutcome _)
    val newScore = scoreByOutcome(newOutcome)

    copy(
      score = score - oldScore + newScore,
      points = points - oldScore + newScore,
      outcomes = {
        if (outcomes.length < rn) outcomes :+ newOutcome else {
          outcomes.zipWithIndex.map {
            case (o, i) => if (i == rn - 1) newOutcome else o
          }
        }
      }
    ) |> { player =>
        player.copy(
          score = player.allScore,
          points = player.allScore
        )
      }
  }

}

object OffPlayer {

  type ID = String
  type No = Int

  val externalSymbol = "@external"

  def withExternal(srcUsername: String) = srcUsername + externalSymbol

  def isExternal(username: String) = username endsWith externalSymbol

  case class PlayerWithUser(player: OffPlayer, user: User, member: Option[lila.team.Member]) {
    def no = player.no
    def userId = user.id
    def playerId = player.id
    def profile = user.profileOrDefault

    def realNameOrUsername = if (player.external) (realName | user.username) + "（临）" else realName | user.username

    def realName = {
      if (user.profileOrDefault.nonEmptyRealName.isDefined) {
        user.profileOrDefault.nonEmptyRealName
      } else if (member.??(_.mark).isDefined) {
        member.??(_.mark)
      } else None
    }

    def json = {
      var attr = Json.obj(
        "id" -> userId,
        "name" -> realNameOrUsername,
        "teamRating" -> member.??(_.rating.map(_.intValue)),
        "clazz" -> member.??(_.clazzIds | List.empty[String]),
        "level" -> user.profileOrDefault.currentLevel.level,
        "sex" -> user.profileOrDefault.sex
      )
      member.foreach { member =>
        member.tags.foreach { tags =>
          tags.tagMap.foreach {
            case (f, t) => attr = attr.add(f, t.value)
          }
        }
      }
      attr
    }
  }

  case class AllPlayerWithUser(user: User, member: Option[lila.team.Member]) {
    def userId = user.id
    def profile = user.profileOrDefault

    def realNameOrUsername = realName | user.username
    def realName = {
      if (user.profileOrDefault.nonEmptyRealName.isDefined) {
        user.profileOrDefault.nonEmptyRealName
      } else if (member.??(_.mark).isDefined) {
        member.??(_.mark)
      } else None
    }

    def json = {
      var attr = Json.obj(
        "id" -> userId,
        "name" -> realNameOrUsername,
        "teamRating" -> member.??(_.rating.map(_.intValue)),
        "clazz" -> member.??(_.clazzIds | List.empty[String]),
        "level" -> user.profileOrDefault.currentLevel.level,
        "sex" -> user.profileOrDefault.sex
      )
      member.foreach { member =>
        member.tags.foreach { tags =>
          tags.tagMap.foreach {
            case (f, t) => attr = attr.add(f, t.value)
          }
        }
      }
      attr
    }
  }

  case class PlayerResult(player: OffPlayer, lightUser: LightUser, rank: Int)

  /*  private[offlineContest] def make(
    contestId: OffContest.ID,
    no: OffPlayer.No,
    userId: User.ID,
    teamRating: Option[Int],
    external: Boolean = false
  ): OffPlayer = new OffPlayer(
    id = makeId(contestId, userId),
    no = no,
    contestId = contestId,
    userId = userId,
    teamRating = teamRating,
    external = external
  )*/

  private[offlineContest] def make(
    contestId: OffContest.ID,
    no: OffPlayer.No,
    userId: User.ID,
    teamRating: Option[Int],
    external: Boolean = false,
    currentRound: Int,
    currentRoundOverPairing: Boolean
  ): OffPlayer = new OffPlayer(
    id = makeId(contestId, userId),
    no = no,
    contestId = contestId,
    userId = userId,
    teamRating = teamRating,
    external = external,
    score = if (currentRoundOverPairing) currentRound * 0.5 else (currentRound - 1) * 0.5,
    points = if (currentRoundOverPairing) currentRound * 0.5 else (currentRound - 1) * 0.5,
    outcomes = if (currentRoundOverPairing) {
      (1 to currentRound).map { _ => OffBoard.Outcome.Half }.toList
    } else {
      if (currentRound == 1) List.empty[OffBoard.Outcome]
      else (1 to currentRound - 1).map { _ => OffBoard.Outcome.Half }.toList
    }
  )

  private[offlineContest] def makeId(contestId: String, userId: String) = userId + "@" + contestId

  def toMap(players: List[OffPlayer]): Map[OffPlayer.No, OffPlayer] =
    players.map(p => p.no -> p).toMap

}

