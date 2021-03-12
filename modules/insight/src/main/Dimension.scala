package lila.insight

import scalatags.Text.all._
import reactivemongo.bson._
import play.api.libs.json._

import chess.opening.EcopeningDB
import chess.{ Color, Role }
import lila.db.dsl._
import lila.rating.PerfType

sealed abstract class Dimension[A: BSONValueHandler](
    val key: String,
    val name: String,
    val dbKey: String,
    val position: Position,
    val description: Frag
) {

  def bson = implicitly[BSONValueHandler[A]]

  def isInGame = position == Position.Game
  def isInMove = position == Position.Move
}

object Dimension {

  import BSONHandlers._
  import Position._
  import Entry.{ BSONFields => F }
  import lila.rating.BSONHandlers.perfTypeIdHandler

  case object Period extends Dimension[Period](
    "period", "日期", F.date, Game,
    raw("对局日期")
  )

  case object Date extends Dimension[lila.insight.DateRange](
    "date", "日期", F.date, Game,
    raw("对局日期")
  )

  case object Perf extends Dimension[PerfType](
    "variant", "类型", F.perf, Game,
    raw("积分对局类型，如超快棋、快棋、中速棋等。")
  )

  case object Phase extends Dimension[Phase](
    "phase", "阶段", F.moves("p"), Move,
    raw("对局阶段：开局、中局、残局。")
  )

  case object Result extends Dimension[Result](
    "result", "结果", F.result, Game,
    raw("对局结果：胜、负或和。")
  )

  case object Termination extends Dimension[Termination](
    "termination", "结束方式", F.termination, Game,
    raw("对局结束方式，如将杀、认输等。")
  )

  case object Color extends Dimension[Color](
    "color", "棋色", F.color, Game,
    raw("棋子颜色：白、黑。")
  )

  case object Opening extends Dimension[chess.opening.Ecopening](
    "opening", "开局", F.eco, Game,
    raw("ECO开局，如：\"A58 Benko Gambit\"。")
  )

  case object OpponentStrength extends Dimension[RelativeStrength](
    "opponentStrength", "对手水平", F.opponentStrength, Game,
    raw("对手水平：非常弱:-200, 较弱:-100, 较强:+100, 非常强:+200。")
  )

  case object PieceRole extends Dimension[Role](
    "piece", "棋子", F.moves("r"), Move,
    raw("行棋的棋子，如：马、象、后。")
  )

  case object MovetimeRange extends Dimension[MovetimeRange](
    "movetime", "平均走棋时间", F.moves("t"), Move,
    raw("平均每步棋思考的时间，单位为秒。")
  )

  case object MyCastling extends Dimension[Castling](
    "myCastling", "我的易位方向", F.myCastling, Game,
    raw("易位方向：短易位、长易位或不易位。")
  )

  case object OpCastling extends Dimension[Castling](
    "opCastling", "对手易位方向", F.opponentCastling, Game,
    raw("对手的易位方向：短易位、长易位或不易位。")
  )

  case object QueenTrade extends Dimension[QueenTrade](
    "queenTrade", "是否换后", F.queenTrade, Game,
    raw("残局前是否交换了后。")
  )

  case object MaterialRange extends Dimension[MaterialRange](
    "material", "子力价值对比", F.moves("i"), Move,
    raw("子力价值对比：兵=1, 象/马=3, 车=5, 后=9")
  )

  def requiresStableRating(d: Dimension[_]) = d match {
    case OpponentStrength => true
    case _ => false
  }

  def valuesOf[X](d: Dimension[X]): List[X] = d match {
    case Period => lila.insight.Period.selector
    case Date => Nil // Period is used instead
    case Perf => PerfType.nonPuzzleShort2
    case Phase => lila.insight.Phase.all
    case Result => lila.insight.Result.all
    case Termination => lila.insight.Termination.all
    case Color => chess.Color.all
    case Opening => EcopeningDB.all
    case OpponentStrength => RelativeStrength.all
    case PieceRole => chess.Role.all.reverse
    case MovetimeRange => lila.insight.MovetimeRange.all
    case MyCastling | OpCastling => lila.insight.Castling.all
    case QueenTrade => lila.insight.QueenTrade.all
    case MaterialRange => lila.insight.MaterialRange.all
  }

  def valueByKey[X](d: Dimension[X], key: String): Option[X] = d match {
    case Period => parseIntOption(key) map lila.insight.Period.apply
    case Date => None
    case Perf => PerfType.byKey get key
    case Phase => parseIntOption(key) flatMap lila.insight.Phase.byId.get
    case Result => parseIntOption(key) flatMap lila.insight.Result.byId.get
    case Termination => parseIntOption(key) flatMap lila.insight.Termination.byId.get
    case Color => chess.Color(key)
    case Opening => EcopeningDB.allByEco get key
    case OpponentStrength => parseIntOption(key) flatMap RelativeStrength.byId.get
    case PieceRole => chess.Role.all.find(_.name == key)
    case MovetimeRange => parseIntOption(key) flatMap lila.insight.MovetimeRange.byId.get
    case MyCastling | OpCastling => parseIntOption(key) flatMap lila.insight.Castling.byId.get
    case QueenTrade => lila.insight.QueenTrade(key == "true").some
    case MaterialRange => parseIntOption(key) flatMap lila.insight.MaterialRange.byId.get
  }

  def valueToJson[X](d: Dimension[X])(v: X): play.api.libs.json.JsObject = {
    play.api.libs.json.Json.obj(
      "key" -> valueKey(d)(v),
      "name" -> valueJson(d)(v)
    )
  }

  def valueKey[X](d: Dimension[X])(v: X): String = (d match {
    case Date => v.toString
    case Period => v.days.toString
    case Perf => v.key
    case Phase => v.id
    case Result => v.id
    case Termination => v.id
    case Color => v.name
    case Opening => v.eco
    case OpponentStrength => v.id
    case PieceRole => v.name
    case MovetimeRange => v.id
    case MyCastling | OpCastling => v.id
    case QueenTrade => v.id
    case MaterialRange => v.id
  }).toString

  def valueJson[X](d: Dimension[X])(v: X): JsValue = d match {
    case Date => JsNumber(v.min.getSeconds)
    case Period => JsString(v.toString)
    case Perf => JsString(v.name)
    case Phase => JsString(v.name)
    case Result => JsString(v.name)
    case Termination => JsString(v.name)
    //case Color => JsString(v.toString)
    case Color => v.toString match {
      case "White" => JsString("白方")
      case "Black" => JsString("黑方")
    }
    case Opening => JsString(v.ecoName)
    case OpponentStrength => JsString(v.name)
    case PieceRole => JsString(pieceRoleName(v.toString))
    case MovetimeRange => JsString(v.name)
    case MyCastling | OpCastling => JsString(v.name)
    case QueenTrade => JsString(v.name)
    case MaterialRange => JsString(v.name)
  }

  def filtersOf[X](d: Dimension[X], selected: List[X]): Bdoc = d match {
    case Dimension.MovetimeRange => selected match {
      case Nil => $empty
      case xs => $doc(d.dbKey $in xs.flatMap(_.tenths.toList))
    }
    case Dimension.Period => selected.sortBy(-_.days).headOption.fold($empty) { period =>
      $doc(d.dbKey $gt period.min)
    }
    case _ => selected map d.bson.write match {
      case Nil => $empty
      case List(x) => $doc(d.dbKey -> x)
      case xs => $doc(d.dbKey -> $doc("$in" -> BSONArray(xs)))
    }
  }

  def dataTypeOf[X](d: Dimension[X]): String = d match {
    case Date => "date"
    case _ => "text"
  }

  def pieceRoleName(r: String) = r match {
    case "Pawn" => "兵"
    case "Knight" => "马"
    case "Bishop" => "象"
    case "Rook" => "车"
    case "Queen" => "后"
    case "King" => "王"
  }
}
