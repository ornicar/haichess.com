package lila.contest

import org.joda.time.{ DateTime, Duration, Interval }
import ornicar.scalalib.Random
import chess.{ Mode, Speed, StartingPosition }
import lila.user.User
import chess.variant.Variant
import chess.Clock.{ Config => ClockConfig }
import lila.game.PerfPicker
import lila.rating.PerfType

case class Contest(
    id: Contest.ID,
    name: String,
    groupName: Option[String],
    logo: Option[String],
    typ: Contest.Type,
    organizer: String, // teamid / classid
    variant: Variant,
    position: StartingPosition,
    mode: Mode, // rated ?
    teamRated: Boolean,
    clock: ClockConfig,
    rule: Contest.Rule,
    startsAt: DateTime,
    finishAt: DateTime,
    deadline: Int,
    deadlineAt: DateTime,
    maxPlayers: Int,
    minPlayers: Int,
    conditions: Condition.All,
    roundSpace: Int,
    rounds: Int,
    appt: Boolean,
    apptDeadline: Option[Int],
    swissBtss: Btsss,
    roundRobinBtss: Btsss,
    canLateMinute: Int,
    canQuitNumber: Int,
    enterApprove: Boolean,
    autoPairing: Boolean,
    enterCost: Int,
    hasPrizes: Boolean,
    description: Option[String],
    attachments: Option[String],
    nbPlayers: Int = 0,
    allRoundFinished: Boolean = false, // 所有轮次都结束了
    currentRound: Int = 1, // Round PublishResult 之后更新此字段
    status: Contest.Status = Contest.Status.Created,
    realFinishAt: Option[DateTime] = None,
    createdBy: User.ID,
    createdAt: DateTime
) {

  def fullName = s"$name${groupName.fold("") { " " + _ }}"

  def isFromPosition = variant == chess.variant.FromPosition

  def isCreated = status == Contest.Status.Created
  def isPublished = status == Contest.Status.Published
  def isEnterStopped = status == Contest.Status.EnterStopped
  def isStarted = status == Contest.Status.Started
  def isFinished = status == Contest.Status.Finished
  def isCanceled = status == Contest.Status.Canceled
  def isEnterable = isPublished

  def isOverPublished = status >= Contest.Status.Published
  def isOverEnterStopped = status >= Contest.Status.EnterStopped
  def isOverStarted = status >= Contest.Status.Started
  def isFinishedOrCanceled = status == Contest.Status.Finished || status == Contest.Status.Canceled

  def quitable = isPublished || isEnterStopped || isStarted
  def inviteable = isPublished || isEnterStopped || isStarted
  def playerKickable = isPublished || isEnterStopped || isStarted
  def playerRemoveable = isPublished

  def secondsToStart = (startsAt.getSeconds - nowSeconds).toInt atLeast 0

  def secondsToFinish = (finishAt.getSeconds - nowSeconds).toInt atLeast 0

  def isRecentlyFinished = isFinished && realFinishAt ?? (d => (nowSeconds - d.getSeconds) < 30 * 60)

  def isRecentlyStarted = isStarted && (nowSeconds - startsAt.getSeconds) < 15

  def shouldEnterStop = deadlineAt.getMillis <= DateTime.now.getMillis

  def shouldStart = startsAt.getMillis <= DateTime.now.getMillis

  def isNowOrSoon = startsAt.isBefore(DateTime.now plusMinutes 15) && !isFinished

  def isDistant = startsAt.isAfter(DateTime.now plusDays 1)

  def durationMillis = finishAt.getMillis - startsAt.getMillis

  def isDeadlined = DateTime.now.isBefore(deadlineAt)

  def duration = new Duration(durationMillis)

  def interval = new Interval(startsAt, duration)

  def overlaps(other: Contest) = interval overlaps other.interval

  def clockStatus = secondsToFinish |> { s => "%02d:%02d".format(s / 60, s % 60) }

  def isCreator(user: User) = user.id == createdBy

  def rated = mode.rated

  def speed = Speed(clock)

  def perfType: Option[PerfType] = PerfPicker.perfType(speed, variant, none)

  def perfLens = PerfPicker.mainOrDefault(speed, variant, none)

  def isPlayerFull = nbPlayers >= maxPlayers

  def guessNbRounds = (nbPlayers - 1) atMost rounds atLeast 2

  def actualNbRounds = if (isFinished) currentRound else guessNbRounds

  // 2-3人双循环, 4-6人单循环, 6人以上瑞士制
  def autoRound = {
    if (nbPlayers <= 3) (nbPlayers - 1) * 2
    else if (nbPlayers <= 6) nbPlayers - 1
    else rounds
  }

  def actualRound =
    if (isOverEnterStopped) {
      rule match {
        case Contest.Rule.Auto => Math.min(rounds, autoRound)
        case Contest.Rule.Swiss => rounds
        case Contest.Rule.RoundRobin => Math.min(nbPlayers - 1, rounds)
        case Contest.Rule.DBRoundRobin => Math.min((nbPlayers - 1) * 2, rounds)
      }
    } else rounds

  def btsss: List[Btss] = {
    rule match {
      case Contest.Rule.Auto => {
        if (nbPlayers <= 3) roundRobinBtss.list
        else if (nbPlayers <= 6) roundRobinBtss.list
        else swissBtss.list
      }
      case Contest.Rule.Swiss => swissBtss.list
      case Contest.Rule.RoundRobin => roundRobinBtss.list
      case Contest.Rule.DBRoundRobin => roundRobinBtss.list
    }
  } :+ Btss.No

  def roundList: List[Round.No] = (1 to actualRound).toList
  def historyRoundList: List[Round.No] = if (currentRound == 1) List.empty[Round.No] else (1 to (currentRound - 1)).toList

  def isAllRoundFinished: Boolean = currentRound >= actualRound

  override def toString = s"$fullName（${typ.name}）"
}

object Contest {

  type ID = String

  def make(
    by: User.ID,
    name: String,
    groupName: Option[String],
    logo: Option[String],
    typ: Contest.Type,
    organizer: String, // teamid / classid
    variant: Variant,
    position: StartingPosition,
    mode: Mode, // rated ?
    teamRated: Boolean,
    clock: ClockConfig,
    rule: Contest.Rule,
    startsAt: DateTime,
    finishAt: DateTime,
    deadline: Int,
    maxPlayers: Int,
    minPlayers: Int,
    roundSpace: Int,
    rounds: Int,
    appt: Boolean,
    apptDeadline: Option[Int],
    swissBtss: Btsss,
    roundRobinBtss: Btsss,
    canLateMinute: Int,
    canQuitNumber: Int,
    enterApprove: Boolean,
    autoPairing: Boolean,
    enterCost: Int,
    hasPrizes: Boolean,
    description: Option[String],
    attachments: Option[String]
  ) = Contest(
    id = makeId,
    name = name,
    groupName = groupName,
    logo = logo,
    typ = typ,
    organizer = organizer,
    variant = variant,
    position = position,
    mode = mode,
    teamRated = teamRated,
    clock = clock,
    rule = rule,
    startsAt = startsAt,
    finishAt = finishAt,
    deadline = deadline,
    deadlineAt = startsAt.minusMinutes(deadline),
    maxPlayers = maxPlayers,
    minPlayers = minPlayers,
    conditions = Condition.All.empty,
    roundSpace = roundSpace,
    rounds = rounds,
    appt = appt,
    apptDeadline = apptDeadline,
    swissBtss = swissBtss,
    roundRobinBtss = roundRobinBtss,
    canLateMinute = canLateMinute,
    canQuitNumber = canQuitNumber,
    enterApprove = enterApprove,
    autoPairing = autoPairing,
    enterCost = enterCost,
    hasPrizes = hasPrizes,
    description = description,
    attachments = attachments,
    createdBy = by,
    createdAt = DateTime.now
  )

  def makeId = Random nextString 8

  sealed abstract class Status(val id: Int, val name: String) extends Ordered[Status] {
    def compare(other: Status) = Integer.compare(id, other.id)
    def is(s: Status): Boolean = this == s
    def is(f: Status.type => Status): Boolean = is(f(Status))
  }
  object Status {
    case object Created extends Status(10, "筹备中")
    case object Published extends Status(20, "报名中")
    case object EnterStopped extends Status(30, "报名截止")
    case object Started extends Status(40, "比赛中")
    case object Finished extends Status(50, "比赛结束")
    case object Canceled extends Status(60, "比赛取消")

    val all = List(Created, Published, EnterStopped, Started, Finished, Canceled)
    val byId = all map { v => (v.id, v) } toMap
    def apply(id: Int): Status = byId get id err s"Bad Status $id"

    val all2 = List(Published, EnterStopped, Started)
    val belong = List(Published, EnterStopped, Started, Finished, Canceled)
    val owner = List(Created, Published, EnterStopped, Started, Finished, Canceled)
    val finish = List(Finished, Canceled)

    val empty = (none -> "所有")
    def allSelect = empty :: all2.map(s => (s.id.some -> s.name))
    def belongSelect = empty :: belong.map(s => (s.id.some -> s.name))
    def ownerSelect = empty :: owner.map(s => (s.id.some -> s.name))
    def finishSelect = empty :: finish.map(s => (s.id.some -> s.name))

  }

  sealed abstract class Type(val id: String, val name: String)
  object Type {
    case object Public extends Type("public", "公开赛")
    case object TeamInner extends Type("team-inner", "俱乐部内部赛")
    case object ClazzInner extends Type("clazz-inner", "班级内部赛")

    val all = List(Public, TeamInner, ClazzInner)

    def apply(id: String) = all.find(_.id == id) err s"Bad Type $id"

    def keySet = all.map(_.id).toSet

    def list = all.map { r => (r.id -> r.name) }

    def byId = all.map { x => x.id -> x }.toMap
  }

  sealed abstract class Rule(val id: String, val name: String)
  object Rule {
    case object Auto extends Rule("auto", "人数自适应")
    case object Swiss extends Rule("swiss", "瑞士制")
    case object RoundRobin extends Rule("round-robin", "单循环")
    case object DBRoundRobin extends Rule("db-round-robin", "双循环")

    val all = List( /*Auto, */ Swiss /*, RoundRobin, DBRoundRobin*/ )

    val byId = all map { v => (v.id, v) } toMap

    def apply(id: String): Rule = byId get id err s"Bad Rule $id"

    def list = all.map { r => (r.id -> r.name) }

  }
}
