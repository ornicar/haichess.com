package lila.gameSearch

import chess.{ Mode, Status }
import org.joda.time.DateTime
import lila.rating.RatingRange
import lila.search.Range
import lila.game.Source

case class Query(
    user1: Option[String] = None,
    user2: Option[String] = None,
    winner: Option[String] = None,
    loser: Option[String] = None,
    winnerColor: Option[Int] = None,
    perf: Option[Int] = None,
    source: Option[Int] = None,
    status: Option[Int] = None,
    turns: Range[Int] = Range.none,
    averageRating: Range[Int] = Range.none,
    hasAi: Option[Boolean] = None,
    aiLevel: Range[Int] = Range.none,
    rated: Option[Boolean] = None,
    date: Range[DateTime] = Range.none,
    duration: Range[Int] = Range.none,
    clock: Clocking = Clocking(),
    sorting: Sorting = Sorting.default,
    analysed: Option[Boolean] = None,
    whiteUser: Option[String] = None,
    blackUser: Option[String] = None
) {

  def nonEmpty =
    user1.nonEmpty ||
      user2.nonEmpty ||
      winner.nonEmpty ||
      loser.nonEmpty ||
      winnerColor.nonEmpty ||
      perf.nonEmpty ||
      source.nonEmpty ||
      status.nonEmpty ||
      turns.nonEmpty ||
      averageRating.nonEmpty ||
      hasAi.nonEmpty ||
      aiLevel.nonEmpty ||
      rated.nonEmpty ||
      date.nonEmpty ||
      duration.nonEmpty ||
      clock.nonEmpty ||
      analysed.nonEmpty
}

object Query {

  import lila.common.Form._
  import play.api.libs.json._

  import Range.rangeJsonWriter
  private implicit val sortingJsonWriter = Json.writes[Sorting]
  private implicit val clockingJsonWriter = Json.writes[Clocking]
  implicit val jsonWriter = Json.writes[Query]

  val durations = {
    val day = 60 * 60 * 24
    ((30, "30 秒") ::
      options(List(60, 60 * 2, 60 * 3, 60 * 5, 60 * 10, 60 * 15, 60 * 20, 60 * 30), _ / 60, "%d 分钟").toList) :+
      (60 * 60 * 1, "1 小时") :+
      (60 * 60 * 2, "2 小时") :+
      (60 * 60 * 3, "3 小时")
  }

  val clockInits = List(
    (0, "0 分钟"),
    (30, "30 分钟"),
    (45, "45 分钟")
  ) ::: options(List(60 * 1, 60 * 2, 60 * 3, 60 * 5, 60 * 10, 60 * 15, 60 * 20, 60 * 30, 60 * 45, 60 * 60, 60 * 90, 60 * 120, 60 * 150, 60 * 180), _ / 60, "%d 分钟").toList

  val clockIncs =
    options(List(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 150, 180), "%d 分钟").toList

  val winnerColors = List(1 -> "白方", 2 -> "黑方", 3 -> "无")

  val perfs = lila.rating.PerfType.nonPuzzleShort map { v => v.id -> v.name }

  //val sources = lila.game.Source.searchable map { v => v.id -> v.name.capitalize }
  val sources = List(
    Source.Lobby.id -> "大厅",
    Source.Friend.id -> "好友",
    Source.Ai.id -> "AI",
    Source.Position.id -> "指定起始位置" /*,
    Source.Import.id -> "导入",
    Source.Tournament.id -> "锦标赛",
    Source.Simul.id -> "车轮赛"*/
  )

  //val modes = Mode.all map { mode => mode.id -> mode.name.capitalize }
  val modes = List(Mode.Casual.id -> "临时", Mode.Rated.id -> "积分")

  val turns = options(
    (1 to 5) ++ (10 to 45 by 5) ++ (50 to 90 by 10) ++ (100 to 300 by 25),
    "%d 步"
  )

  val averageRatings = (RatingRange.min to RatingRange.max by 100).toList map { e => e -> (e + " 分") }

  val hasAis = List(0 -> "人类对手", 1 -> "电脑对手")

  val aiLevels = (1 to 8) map { l => l -> ("level " + l) }

  val dates = List("0d" -> "现在") ++
    options(List(1, 2, 6), "h", "%d 小时{s} 以前") ++
    options(1 to 6, "d", "%d 天{s} 以前") ++
    options(1 to 3, "w", "%d 周{s} 以前") ++
    options(1 to 6, "m", "%d 月{s} 以前") ++
    options(1 to 5, "y", "%d 年{s} 以前")

  val statuses = List(
    Status.Mate.id -> "将杀",
    Status.Resign.id -> "认输",
    Status.Stalemate.id -> "逼和",
    Status.Draw.id -> "和棋",
    Status.Outoftime.id -> "超时"
  )

  /*  val statuses = Status.finishedNotCheated.map {
    case s if s.is(_.Timeout) => none
    case s if s.is(_.NoStart) => none
    case s if s.is(_.UnknownFinish) => none
    case s if s.is(_.Outoftime) => Some(s.id -> "Clock Flag")
    case s if s.is(_.VariantEnd) => Some(s.id -> "Variant End")
    case s => Some(s.id -> s.toString)
  }.flatten*/
}
