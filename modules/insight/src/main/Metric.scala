package lila.insight

import scalatags.Text.all._

import reactivemongo.bson._

sealed abstract class Metric(
    val key: String,
    val name: String,
    val dbKey: String,
    val position: Position,
    val per: Position,
    val dataType: Metric.DataType,
    val description: Frag
)

object Metric {

  sealed trait DataType {
    def name = toString.toLowerCase
  }
  object DataType {
    case object Seconds extends DataType
    case object Count extends DataType
    case object Average extends DataType
    case object Percent extends DataType
  }

  import DataType._
  import Position._
  import Entry.{ BSONFields => F }

  case object MeanCpl extends Metric("acpl", "平均厘兵损失", F moves "c", Move, Move, Average,
    raw("""您行棋与引擎最佳走法的差异度，代表行棋的精确度，分数越低表示越精确。"""))

  case object Movetime extends Metric("movetime", "平均走棋时间", F moves "t", Move, Move, Seconds,
    Dimension.MovetimeRange.description)

  case object Result extends Metric("result", "结果", F.result, Game, Game, Percent,
    Dimension.Result.description)

  case object Termination extends Metric("termination", "结束方式", F.termination, Game, Game, Percent,
    Dimension.Termination.description)

  case object RatingDiff extends Metric("ratingDiff", "积分变化", F.ratingDiff, Game, Game, Average,
    raw("对局结束时您获得或失去的等级分。"))

  case object OpponentRating extends Metric("opponentRating", "对手等级分", F.opponentRating, Game, Game, Average,
    raw("对局中您对手的等级分。"))

  case object NbMoves extends Metric("nbMoves", "每局步数", F moves "r", Move, Game, Average,
    raw("对局中您走的步数，不含对手的步数。"))

  case object PieceRole extends Metric("piece", "棋子", F moves "r", Move, Move, Percent,
    Dimension.PieceRole.description)

  case object Opportunism extends Metric("opportunism", "机会", F moves "o", Move, Move, Percent,
    raw("抓住对手错误的概率。100%表示您抓住了全部机会，0%表示您全部错过了。"))

  case object Luck extends Metric("luck", "运气", F moves "l", Move, Move, Percent,
    raw("对手错过您犯的错误的概率。100%表示对手全部错过了；0%表示对手全部抓住错误，对您进行了惩罚。"))

  case object Material extends Metric("material", "子力价值对比", F moves "i", Move, Move, Average,
    Dimension.MaterialRange.description)

  val all = List(MeanCpl, Movetime, Result, Termination, RatingDiff, OpponentRating, NbMoves, PieceRole, Opportunism, Luck, Material)
  val byKey = all map { p => (p.key, p) } toMap

  def requiresAnalysis(m: Metric) = m match {
    case MeanCpl => true
    case _ => false
  }

  def requiresStableRating(m: Metric) = m match {
    case RatingDiff => true
    case OpponentRating => true
    case _ => false
  }

  def isStacked(m: Metric) = m match {
    case Result => true
    case Termination => true
    case PieceRole => true
    case _ => false
  }

  def valuesOf(metric: Metric): List[MetricValue] = metric match {
    case Result => lila.insight.Result.all.map { r =>
      MetricValue(BSONInteger(r.id), MetricValueName(r.name))
    }
    case Termination => lila.insight.Termination.all.map { r =>
      MetricValue(BSONInteger(r.id), MetricValueName(r.name))
    }
    case PieceRole => chess.Role.all.reverse.map { r =>
      MetricValue(BSONString(r.forsyth.toString), MetricValueName(r.toString))
    }
    case _ => Nil
  }

  case class MetricValueName(name: String)
  case class MetricValue(key: BSONValue, name: MetricValueName)
}
