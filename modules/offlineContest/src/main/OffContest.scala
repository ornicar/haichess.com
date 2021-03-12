package lila.offlineContest

import org.joda.time.DateTime
import ornicar.scalalib.Random
import lila.user.User

case class OffContest(
    id: OffContest.ID,
    name: String,
    groupName: Option[String],
    logo: Option[String],
    typ: OffContest.Type,
    organizer: String, // teamid / classid
    rule: OffContest.Rule,
    rounds: Int,
    swissBtss: OffBtsss,
    roundRobinBtss: OffBtsss,
    nbPlayers: Int = 0,
    currentRound: Int = 1, // Round PublishResult 之后更新此字段
    status: OffContest.Status = OffContest.Status.Created,
    createdBy: User.ID,
    createdAt: DateTime
) {

  def fullName = s"$name${groupName.fold("") { " " + _ }}"

  def isCreated = status == OffContest.Status.Created
  def isStarted = status == OffContest.Status.Started
  def isFinished = status == OffContest.Status.Finished
  def isCanceled = status == OffContest.Status.Canceled
  def isOverStarted = status >= OffContest.Status.Started
  def isFinishedOrCanceled = isFinished || isCanceled

  def isCreator(user: User) = user.id == createdBy

  def playerRemoveable = isCreated

  def playerKickable = isStarted

  def historyRounds = (1 to currentRound - 1).toList

  def btsss: List[OffBtss] = {
    rule match {
      case OffContest.Rule.Auto => {
        if (nbPlayers <= 3) roundRobinBtss.list
        else if (nbPlayers <= 6) roundRobinBtss.list
        else swissBtss.list
      }
      case OffContest.Rule.Swiss => swissBtss.list
      case OffContest.Rule.RoundRobin => roundRobinBtss.list
      case OffContest.Rule.DBRoundRobin => roundRobinBtss.list
    }
  } :+ OffBtss.No

  override def toString = s"$fullName（${typ.name}）"
}

object OffContest {

  type ID = String

  def make(
    by: User.ID,
    name: String,
    groupName: Option[String],
    logo: Option[String],
    typ: OffContest.Type,
    organizer: String, // teamid / classid
    rule: OffContest.Rule,
    rounds: Int,
    swissBtss: OffBtsss,
    roundRobinBtss: OffBtsss
  ) = OffContest(
    id = makeId,
    name = name,
    groupName = groupName,
    logo = logo,
    typ = typ,
    organizer = organizer,
    rule = rule,
    rounds = rounds,
    swissBtss = swissBtss,
    roundRobinBtss = roundRobinBtss,
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
    case object Started extends Status(20, "比赛中")
    case object Finished extends Status(50, "比赛结束")
    case object Canceled extends Status(60, "比赛取消")

    val all = List(Created, Started, Finished, Canceled)
    val current = List(Created, Started)
    val history = List(Finished, Canceled)

    def byId = all map { v => (v.id, v) } toMap
    def apply(id: Int): Status = byId get id err s"Bad Status $id"

    val empty = none -> "所有"
    def allChoice = empty :: all.map(s => s.id.some -> s.name)
    def currentChoice = empty :: current.map(s => s.id.some -> s.name)
    def historyChoice = empty :: history.map(s => s.id.some -> s.name)
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
