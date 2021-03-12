package lila.contest

import play.api.data._
import play.api.data.Forms._
import chess.variant._
import lila.common.Form._
import chess.{ Mode, StartingPosition }
import lila.user.User
import ContestSetup._
import chess.format.Forsyth
import org.joda.time.DateTime
import lila.game.Game

final class DataForm(val captcher: akka.actor.ActorSelection) extends lila.hub.CaptchedForm {

  import DataForm._

  def contest(user: User, id: Option[Contest.ID]) = Form(mapping(
    "basics" -> mapping(
      "name" -> nonEmptyText(minLength = 2, maxLength = 30),
      "groupName" -> optional(nonEmptyText(minLength = 2, maxLength = 30)),
      "logo" -> optional(text(minLength = 5, maxLength = 150)),
      "typ" -> stringIn(Contest.Type.list),
      "organizer" -> nonEmptyText(minLength = 6, maxLength = 8),
      "variant" -> text.verifying(v => guessVariant(v).isDefined),
      "position" -> nonEmptyText.verifying("Fen格式错误", validFen _),
      "rated" -> boolean,
      "clockTime" -> numberInDouble(clockTimeChoices),
      "clockIncrement" -> numberIn(clockIncrementChoices),
      "rule" -> stringIn(Contest.Rule.list),
      "startsAt" -> futureDateTime,
      "finishAt" -> futureDateTime
    )(Basics.apply)(Basics.unapply),
    "conditions" -> mapping(
      "deadline" -> numberIn(deadlineMinuteChoices),
      "maxPlayers" -> number(min = 4, max = 200),
      "minPlayers" -> number(min = 2, max = 100),
      "enterCost" -> number(min = 0, max = 10000),
      "enterApprove" -> numberIn(booleanChoices),
      "all" -> Condition.DataForm.all
    )(Conditions.apply)(Conditions.unapply),
    "rounds" -> mapping(
      "spaceDay" -> numberIn(roundSpaceDayChoices),
      "spaceHour" -> numberIn(roundSpaceHourChoices),
      "spaceMinute" -> numberIn(roundSpaceMinuteChoices),
      "rounds" -> number(min = 1, max = 16),
      "appt" -> numberIn(booleanChoices),
      "apptDeadline" -> optional(numberIn(apptDeadlineMinuteChoices)),
      "list" -> list(mapping(
        "startsAt" -> futureDateTime
      )(RoundSetup.apply)(RoundSetup.unapply))
    )(Rounds.apply)(Rounds.unapply),
    "others" -> mapping(
      "swissBtss" -> list(stringIn(Btss.list)),
      "roundRobinBtss" -> list(stringIn(Btss.list)),
      "canLateMinute" -> number(min = 1, max = 30),
      "canQuitNumber" -> number(min = 0, max = 10),
      "autoPairing" -> numberIn(booleanChoices),
      "hasPrizes" -> numberIn(booleanChoices),
      "description" -> optional(nonEmptyText),
      "attachments" -> optional(nonEmptyText)
    )(Others.apply)(Others.unapply)
  )(ContestSetup.apply)(ContestSetup.unapply)
    .verifying("比赛名称重复", !_.validName(id).awaitSeconds(2))
    .verifying("非认证俱乐部不能创建公开赛", _.validPublic(user))
    .verifying("无效的时钟", _.validClock)
    .verifying("请上调时钟或下调允许迟到时间", _.validCanLateTime)
    .verifying("无效的比赛起始时间", _.validFinishAt)
    .verifying("报名人数上限必须大于或等于报名人数下限", _.validPlayers)
    .verifying("至少1个轮次，并且不大于16个轮次", _.validRoundNumber)
    .verifying("轮次间隔时间至少1分钟", _.validRoundSpace)
    .verifying("无效的轮次开始时间", _.validRoundStartsAt)
    .verifying("混乱的轮次开始时间", _.validRoundStartsAtBetween)
    .verifying("必须定义约棋截止时间", _.validAppt)
    .verifying("轮次间隔大于1天才可以定义约棋模式", _.validApptSpaceMinutes))

  def contestDefault(user: User) = contest(user, None) fill ContestSetup.default

  def contestOf(user: User, c: Contest, rounds: List[Round]) = contest(user, c.id.some) fill ContestSetup(
    basics = Basics(
      name = c.name,
      groupName = c.groupName,
      logo = c.logo,
      typ = c.typ.id,
      organizer = c.organizer,
      variant = c.variant.id.toString,
      position = c.position.fen,
      rated = c.mode.id == 1,
      clockTime = c.clock.limitInMinutes,
      clockIncrement = c.clock.incrementSeconds,
      rule = c.rule.id,
      startsAt = c.startsAt,
      finishAt = c.finishAt
    ),
    conditions = Conditions(
      deadline = c.deadline,
      maxPlayers = c.maxPlayers,
      minPlayers = c.minPlayers,
      enterCost = c.enterCost.toInt,
      enterApprove = if (c.enterApprove) 1 else 0,
      all = Condition.DataForm.AllSetup(c.conditions)
    ),
    rounds = Rounds(
      spaceDay = c.roundSpace / (24 * 60),
      spaceHour = c.roundSpace % (24 * 60) / 60,
      spaceMinute = c.roundSpace % (24 * 60) % 60,
      rounds = c.rounds,
      appt = if (c.appt) 1 else 0,
      apptDeadline = c.apptDeadline,
      list = rounds.map(r => RoundSetup(r.startsAt))
    ),
    others = Others(
      swissBtss = c.swissBtss.list.map(_.id),
      roundRobinBtss = c.roundRobinBtss.list.map(_.id),
      canLateMinute = c.canLateMinute,
      canQuitNumber = c.canQuitNumber,
      autoPairing = if (c.autoPairing) 1 else 0,
      hasPrizes = if (c.hasPrizes) 1 else 0,
      description = c.description,
      attachments = c.attachments
    )
  )

  val joinForm = Form(mapping(
    "message" -> text(minLength = 10, maxLength = 2000),
    "gameId" -> text,
    "move" -> text
  )(JoinSetup.apply)(JoinSetup.unapply)
    .verifying(captchaFailMessage, validateCaptcha _)) fill JoinSetup(
    message = "您好，我想加入比赛！",
    gameId = "",
    move = ""
  )

  val manualPairingForm = Form(mapping(
    "source" -> manualPairingPlayerMapping,
    "target" -> manualPairingPlayerMapping
  )(ManualPairing.apply)(ManualPairing.unapply))

  def manualPairingPlayerMapping = mapping(
    "isBye" -> numberIn(booleanChoices),
    "board" -> optional(nonEmptyText(minLength = 8, maxLength = 8)),
    "color" -> optional(numberIn(booleanChoices)),
    "player" -> optional(nonEmptyText(minLength = 10, maxLength = 50))
  )(ManualPairingPlayer.apply)(ManualPairingPlayer.unapply)

  def forbidden = Form(mapping(
    "name" -> nonEmptyText(minLength = 2, maxLength = 20),
    "playerIds" -> nonEmptyText(minLength = 2, maxLength = 600)
  )(ForbiddenData.apply)(ForbiddenData.unapply))

  def forbiddenOf(f: Forbidden) = forbidden fill ForbiddenData(f.name, f.playerIds.mkString(","))

}

object DataForm {

  val clockTimes: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ (2d to 7d by 1d) ++ (10d to 30d by 5d) ++ (40d to 60d by 10d)
  val clockTimeDefault = 5d
  private def formatLimit(l: Double) =
    chess.Clock.Config(l * 60 toInt, 0).limitString + {
      if (l <= 1) " 分钟" else " 分钟"
    }
  val clockTimeChoices = optionsDouble(clockTimes, formatLimit)

  val clockIncrements = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val clockIncrementDefault = 0
  val clockIncrementChoices = options(clockIncrements, "%d 秒")
  val apptDeadlineMinuteChoices = Seq((10 -> "下轮开始前10分钟"), (30 -> "下轮开始前30分钟"), (60 -> "下轮开始前1小时"), (60 * 24 -> "下轮开始前24小时"))
  val deadlineMinuteChoices = Seq( /*(1 -> "比赛开始前1分钟"),*/ (3 -> "比赛开始前3分钟"), (5 -> "比赛开始前5分钟"), (30 -> "比赛开始前30分钟"), (60 -> "比赛开始前1小时"), (60 * 24 -> "比赛开始前1天"))
  val booleanChoices = Seq((0 -> "否"), (1 -> "是"))
  val canLateMinuteChoices = Seq((1 -> "1 分钟"), (3 -> "3 分钟"), (5 -> "5 分钟"), (10 -> "10 分钟"), (20 -> "20 分钟"), (30 -> "30 分钟"))
  val roundSpaceDayChoices = Seq((0 -> "0 天"), (1 -> "1 天"), (2 -> "2 天"), (7 -> "7 天"))
  val roundSpaceHourChoices = Seq((0 -> "0 小时"), (1 -> "1 小时"), (2 -> "2 小时"), (3 -> "3 小时"), (4 -> "4 小时"))
  val roundSpaceMinuteChoices = Seq((0 -> "0 分钟"), (5 -> "5 分钟"), (10 -> "10 分钟"), (15 -> "15 分钟"), (20 -> "20 分钟"), (30 -> "30 分钟"), (45 -> "45 分钟"))

  val positions = StartingPosition.allWithInitial.map(_.fen)
  val positionChoices = StartingPosition.allWithInitial.map { p =>
    p.fen -> p.fullName
  }
  val positionDefault = StartingPosition.initial.fen

  def startingPosition(fen: String): StartingPosition =
    Thematic.byFen(fen) | StartingPosition(
      eco = "",
      name = "",
      fen = fen,
      wikiPath = "",
      moves = "",
      featurable = false
    )

  val validVariants = List(Standard)
  def guessVariant(from: String): Option[Variant] = validVariants.find { v =>
    v.key == from || parseIntOption(from).exists(v.id ==)
  }

  def validFen(fen: String) = (Forsyth <<< fen).??(f => f.situation.playable(false))
}

case class ContestSetup(
    basics: Basics,
    conditions: Conditions,
    rounds: Rounds,
    others: Others
) {

  def realMode = Mode(basics.rated)
  def realType = Contest.Type(basics.typ)
  def realVariant = DataForm.guessVariant(basics.variant) | chess.variant.Standard
  def bool(v: Int) = if (v == 1) true else false
  def clockConfig = chess.Clock.Config((basics.clockTime * 60).toInt, basics.clockIncrement)

  def toContest(user: User, myTeams: List[(String, String)], myClazzs: List[(String, String)]) = Contest.make(
    by = user.id,
    name = basics.name,
    groupName = basics.groupName,
    logo = basics.logo,
    typ = realType,
    organizer = basics.organizer,
    variant = realVariant,
    position = DataForm.startingPosition(basics.position),
    mode = realMode,
    clock = clockConfig,
    rule = Contest.Rule(basics.rule),
    startsAt = basics.startsAt,
    finishAt = basics.finishAt,
    deadline = conditions.deadline,
    maxPlayers = conditions.maxPlayers,
    minPlayers = conditions.minPlayers,
    roundSpace = rounds.toSpaceMinutes,
    rounds = rounds.rounds,
    appt = rounds.appt == 1,
    apptDeadline = rounds.apptDeadline,
    swissBtss = Btsss(others.swissBtss.map(Btss(_))),
    roundRobinBtss = Btsss(others.roundRobinBtss.map(Btss(_))),
    canLateMinute = others.canLateMinute,
    canQuitNumber = others.canQuitNumber,
    enterApprove = bool(conditions.enterApprove),
    autoPairing = bool(others.autoPairing),
    enterCost = conditions.enterCost,
    hasPrizes = bool(others.hasPrizes),
    description = others.description,
    attachments = others.attachments
  ) |> { contest =>
      contest.perfType.fold(contest) { perfType =>
        contest.copy(conditions = conditions.all.convert(perfType, myTeams.toMap, myClazzs.toMap))
      }
    }

  def roundList(contestId: Contest.ID) = rounds.list.zipWithIndex map {
    case (r, i) => Round.make(
      no = i + 1,
      contestId = contestId,
      startsAt = r.startsAt
    )
  }

  def validName(id: Option[Contest.ID]) = ContestRepo.nameExists(basics.name.trim, basics.groupName.map(_.trim), id)
  def validClock = basics.validClock
  def validPublic(user: User) = {
    realType match {
      case Contest.Type.Public => user.isTeam
      case _ => true
    }
  }
  def validFinishAt = basics.validFinishAt
  def validPlayers = conditions.validPlayers
  def validRoundNumber = rounds.validRoundNumber
  def validRoundSpace = rounds.validRoundSpace
  def validRoundStartsAt = rounds.validRoundStartsAt(basics.startsAt, basics.finishAt)
  def validRoundStartsAtBetween = rounds.validRoundStartsAtBetween
  def validCanLateTime = basics.clockTime > others.canLateMinute + 1
  def validAppt = rounds.validAppt
  def validApptSpaceMinutes = rounds.validApptSpaceMinutes

}

object ContestSetup {

  def default = {
    val now = DateTime.now
    ContestSetup(
      basics = Basics(
        name = "",
        groupName = None,
        logo = None,
        typ = "public",
        organizer = "",
        variant = "1",
        position = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        rated = true,
        clockTime = DataForm.clockTimeDefault,
        clockIncrement = DataForm.clockIncrementDefault,
        rule = Contest.Rule.Swiss.id,
        startsAt = now.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).plusHours(1),
        finishAt = now.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).plusHours(2)
      ),
      conditions = Conditions(
        deadline = 5,
        maxPlayers = 50,
        minPlayers = 2,
        enterCost = 0,
        enterApprove = 1,
        all = Condition.DataForm.AllSetup.default
      ),
      rounds = Rounds(
        spaceDay = 0,
        spaceHour = 0,
        spaceMinute = 30,
        rounds = 5,
        appt = 0,
        apptDeadline = None,
        list = List.empty
      ),
      others = Others(
        swissBtss = Btss.swissDefault.map(_.id),
        roundRobinBtss = Btss.roundRobinDefault.map(_.id),
        canLateMinute = 3,
        canQuitNumber = 3,
        autoPairing = 1,
        hasPrizes = 0,
        description = None,
        attachments = None
      )
    )
  }

  case class Basics(
      name: String,
      groupName: Option[String],
      logo: Option[String],
      typ: String,
      organizer: String,
      variant: String,
      position: String,
      rated: Boolean,
      clockTime: Double,
      clockIncrement: Int,
      rule: String,
      startsAt: DateTime,
      finishAt: DateTime
  ) {

    def validClock = (clockTime + clockIncrement) > 0

    def validFinishAt = finishAt.isAfter(startsAt)

    def durationMillis = finishAt.getMillis - startsAt.getMillis

    // There are 2 players, and they don't always use all their time (0.8)
    def estimatedGameSeconds: Double = {
      (60 * clockTime + 40 * clockIncrement) * 2 * 0.8
    }
  }

  case class Conditions(
      deadline: Int,
      maxPlayers: Int,
      minPlayers: Int,
      enterCost: Int,
      enterApprove: Int,
      all: Condition.DataForm.AllSetup
  ) {

    def validPlayers = maxPlayers >= minPlayers
  }

  case class Rounds(
      spaceDay: Int,
      spaceHour: Int,
      spaceMinute: Int,
      rounds: Int,
      appt: Int,
      apptDeadline: Option[Int],
      list: List[RoundSetup]
  ) {

    def toSpaceMinutes = spaceDay * 24 * 60 + spaceHour * 60 + spaceMinute

    def validRoundNumber = list.size > 0 && list.size <= 16

    def validRoundSpace = toSpaceMinutes >= 1

    def validAppt = if (appt == 1) {
      apptDeadline.isDefined
    } else true

    def validApptSpaceMinutes = if (appt == 1) {
      toSpaceMinutes >= 60 * 24
    } else true

    // 校验轮次开始时间小于比赛结束时间
    def validRoundStartsAt(startsAt: DateTime, finishAt: DateTime) = list.forall(d =>
      finishAt.getMillis > d.startsAt.getMillis && d.startsAt.getMillis >= startsAt.getMillis)

    // 校验下一轮次开始时间大于上一轮开始时间
    def validRoundStartsAtBetween = {
      val arr = list.toArray
      val len = arr.length
      (0 to (len - 1)).forall(i => {
        (i == len - 1) || arr(i).startsAt.isBefore(arr(i + 1).startsAt)
      })
    }

    // 校验 每场比赛的持续时间不能低于评估时间的 （上限/下限）* 20%
    /*    def validGameDurationSeconds(estimatedGameSeconds: Double) = {
      val ceilSeconds = (estimatedGameSeconds + estimatedGameSeconds * 0.2).toInt
      val floorSeconds = (estimatedGameSeconds * 0.8).toInt
      list.forall(r => {
        val oneGameDurationSeconds = (r.finishAt.getMillis - r.startsAt.getMillis) / 1000
        ceilSeconds > oneGameDurationSeconds && oneGameDurationSeconds > floorSeconds
      })
    }*/

  }

  case class Others(
      swissBtss: List[String],
      roundRobinBtss: List[String],
      canLateMinute: Int,
      canQuitNumber: Int,
      autoPairing: Int,
      hasPrizes: Int,
      description: Option[String],
      attachments: Option[String]
  )

  case class RoundSetup(
      startsAt: DateTime
  )

}

case class JoinSetup(
    message: String,
    gameId: String,
    move: String
) {

  def toRequest(contest: Contest, user: User) = Request.make(
    contestId = contest.id,
    contestName = contest.name,
    userId = user.id,
    message = message
  )

}

case class ManualPairing(source: ManualPairingPlayer, target: ManualPairingPlayer)
case class ManualPairingPlayer(isBye: Int, board: Option[Game.ID], color: Option[Int], player: Option[Player.ID]) {
  def board_ = board.get
  def color_ = color.get
  def player_ = player.get
  def isBye_ = isBye == 1
}

case class ForbiddenData(name: String, playerIds: String) {

  def toForbidden(contestId: String, forbidden: Option[Forbidden]) = {
    forbidden.fold(
      Forbidden.make(
        name = name,
        contestId = contestId,
        playerIds = toList
      )
    ) { fb =>
        fb.copy(
          name = name,
          playerIds = toList
        )
      }
  }

  def toList = playerIds.split(",").toList

}
