package lila.insight

import lila.rating.PerfType

case class Preset(name: String, question: Question[_])

object Preset {

  import lila.insight.{ Dimension => D, Metric => M }

  val all = List(

    Preset(
      "我在对阵比自己强的对手获得的分数多，还是比自己弱的选手获得分数多？",
      Question(D.OpponentStrength, M.RatingDiff, Nil)
    ),

    Preset(
      "在超快棋和快棋中，我走棋的速度如何？",
      Question(D.PieceRole, M.Movetime, List(
        Filter(D.Perf, List(PerfType.Bullet, PerfType.Blitz))
      ))
    ),

    Preset(
      "作为白棋，我常用的开局类型胜率如何？",
      Question(D.Opening, M.Result, List(
        Filter(D.Perf, List(PerfType.Bullet, PerfType.Blitz, PerfType.Rapid, PerfType.Classical, PerfType.Correspondence)),
        Filter(D.Color, List(chess.White))
      ))
    ),

    Preset(
      "在各个阶段，我经常能抓住对手错误的错误吗？",
      Question(D.Phase, M.Opportunism, Nil)
    ),

    Preset(
      "如果不易位，我获得等级分的情况如何？",
      Question(D.Perf, M.RatingDiff, List(
        Filter(D.MyCastling, List(Castling.Queenside, Castling.None))
      ))
    ),

    Preset(
      "当我跟对手交换了后，结果如何？",
      Question(D.Perf, M.Result, List(
        Filter(D.QueenTrade, List(QueenTrade.Yes))
      ))
    ),

    Preset(
      "在不同类型的对局中，我对手的平均水平（等级分）如何？",
      Question(D.Perf, M.OpponentRating, Nil)
    ),

    Preset(
      "开局阶段，我走子的精度如何？",
      Question(D.PieceRole, M.MeanCpl, List(
        Filter(D.Phase, List(Phase.Opening))
      ))
    )
  )
}
