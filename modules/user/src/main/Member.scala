package lila.user

import lila.db.dsl._
import lila.db.BSON
import org.joda.time.{ DateTime, Period, PeriodType }
import reactivemongo.bson.{ BSONDocument, BSONHandler, BSONString, Macros }
import MemberLevel._

case class Member(
    code: String,
    levels: MemberLevels,
    points: Int, // 积分
    coin: Int // 金币
) {

  def lv: MemberLevel = lvWithExpire.level

  def lvWithExpire: MemberLevelWithExpire = levels.get(code) err s"can not find code $code"

  def sortedLevels = levels.map.toList.map(_._2).sortBy(_.level.id)

  def isGoldAvailable = goldLevel.??(!_.expired)

  def isSilverAvailable = silverLevel.??(!_.expired)

  def goldLevel = levels.get(MemberLevel.Gold.code)

  def silverLevel = levels.get(MemberLevel.Silver.code)

  def merge(level: MemberLevel, days: Int, pointsDiff: Int, coinDiff: Int) =
    copy(
      levels = levels.merge(level, days),
      points = points + pointsDiff,
      coin = coin + coinDiff
    ) |> { m =>
        m.copy(
          code = m.levels.adaptLevel.code
        )
      }

  def mergeLevel(level: MemberLevel, days: Int) =
    copy(
      levels = levels.merge(level, days)
    ) |> { m =>
        m.copy(
          code = m.levels.adaptLevel.code
        )
      }

  def mergePoints(pointsDiff: Int) = copy(
    points = points + pointsDiff
  )

  def mergeCoin(coinDiff: Int) = copy(
    coin = coin + coinDiff
  )

}

case object Member {

  def MaxYear = 100

  def default = Member(
    code = MemberLevel.General.code,
    levels = MemberLevels(
      Map(
        MemberLevel.General.code -> {
          val dateNow = DateTime.now
          MemberLevelWithExpire(
            level = MemberLevel.General,
            expireAt = dateNow.plusYears(MaxYear),
            updateAt = dateNow
          )
        }
      )
    ),
    points = 0,
    coin = 0
  )

  implicit val MemberLevelBSONHandler = new BSONHandler[BSONString, MemberLevel] {
    def read(b: BSONString): MemberLevel = MemberLevel(b.value)
    def write(x: MemberLevel) = BSONString(x.code)
  }
  implicit val MemberLevelWithExpireBSONHandler = Macros.handler[MemberLevelWithExpire]
  implicit val MemberLevelsBSONHandler = new BSONHandler[Bdoc, MemberLevels] {
    private val mapHandler = BSON.MapDocument.MapHandler[String, MemberLevelWithExpire]
    def read(b: Bdoc) = MemberLevels(mapHandler.read(b) map {
      case (code, level) => code -> level
    })
    def write(levels: MemberLevels) = BSONDocument(levels.map.mapValues(MemberLevelWithExpireBSONHandler.write))
  }
  implicit val MemberBSONHandler = Macros.handler[Member]

}

case class MemberLevels(map: Map[String, MemberLevelWithExpire]) {

  def get(code: String): Option[MemberLevelWithExpire] = map.get(code)

  def adaptLevel: MemberLevel = {
    val id = map.foldLeft(MemberLevel.General.id) {
      case (id, mwe) => {
        if (!mwe._2.expired && mwe._2.level.id > id) mwe._2.level.id else id
      }
    }
    MemberLevel.byId(id)
  }

  def merge(level: MemberLevel, days: Int) = {
    get(level.code).fold(
      copy(map = map + (level.code -> MemberLevelWithExpire.of(level, days)))
    ) { old =>
        if (old.expired) {
          copy(map = map + (level.code -> MemberLevelWithExpire.of(level, days)))
        } else {
          copy(map = map + (level.code -> MemberLevelWithExpire.of(level, old.expireAt.plusDays(days))))
        }
      }
  }

}

case class MemberLevelWithExpire(level: MemberLevel, expireAt: DateTime, updateAt: DateTime) {

  def isForever = expireAt.isAfter(new DateTime(2021 + Member.MaxYear, 1, 1, 0, 0))

  def expired = expireAt.isBeforeNow && !isForever

  def expireNote = if (isForever) "永久" else expireAt.toString("yyyy年MM月dd日 HH:mm")

  def remainderDays = {
    if (!expired) {
      new Period(DateTime.now.getMillis, expireAt.getMillis, PeriodType.days).getDays
    } else 0
  }

  def remainderChargeDays = {
    if (!expired) {
      val now = DateTime.now
      if (isForever) {
        if (updateAt.plusYears(5).isAfter(now)) {
          new Period(now.getMillis, updateAt.plusYears(5).getMillis, PeriodType.days).getDays
        } else 0
      } else {
        new Period(now.getMillis, expireAt.getMillis, PeriodType.days).getDays
      }
    } else 0
  }

}

object MemberLevelWithExpire {

  def of(level: MemberLevel, days: Int) = {
    val dateNow = DateTime.now
    MemberLevelWithExpire(level, dateNow.plusDays(days), dateNow)
  }

  def of(level: MemberLevel, expireAt: DateTime) = MemberLevelWithExpire(level, expireAt, DateTime.now)

}

sealed abstract class MemberLevel(val id: Int, val code: String, val name: String, val cap: String, val prices: Prices, val permissions: MemberPermission)

object MemberLevel {

  case object General extends MemberLevel(10, "general", "注册会员", "R", Prices(0.00, 0.00), MemberPermission(puzzle = 5, themePuzzle = 5, puzzleRush = 1, Study(false, false, false, true), resource = false, insight = false))
  case object Silver extends MemberLevel(20, "silver", "银牌会员", "S", Prices(19.90, 199.00), MemberPermission(puzzle = 25, themePuzzle = 25, puzzleRush = 5, Study(true, true, true, true), resource = true, insight = true))
  case object Gold extends MemberLevel(30, "gold", "金牌会员", "G", Prices(29.90, 299.00), MemberPermission(puzzle = Integer.MAX_VALUE, themePuzzle = Integer.MAX_VALUE, puzzleRush = Integer.MAX_VALUE, Study(true, true, true, true), resource = true, insight = true))

  def all = List(General, Silver, Gold)
  def nofree = List(Gold, Silver)
  def choices: List[(String, String)] = nofree.map(d => d.code -> d.name)
  def apply(code: String): MemberLevel = all.find(_.code == code) getOrElse General
  def byId(id: Int): MemberLevel = all.find(_.id == id) getOrElse General
  def yearPriceOf(code: String): BigDecimal = apply(code).prices.year
  def monthPriceOf(code: String): BigDecimal = apply(code).prices.month
  def dayPriceOf(code: String): BigDecimal = apply(code).prices.day
  def defaultView = Gold

  case class Prices(month: BigDecimal, year: BigDecimal) {
    def day: BigDecimal = month / 30
  }

  case class MemberPermission(
      puzzle: Int,
      themePuzzle: Int,
      puzzleRush: Int,
      study: Study,
      resource: Boolean,
      insight: Boolean
  )

  case class Study(Private: Boolean, Student: Boolean, Team: Boolean, Public: Boolean)

}