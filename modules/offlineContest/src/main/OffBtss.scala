package lila.offlineContest

private[offlineContest] abstract class OffBtss(val id: String, val name: String) {

  def score(boards: Boards, playerBtsss: OffBtss.PlayerBtssScores): OffBtss.PlayerBtssScores

}

private[offlineContest] object OffBtss {

  case class BtssScore(btss: OffBtss, score: Double)
  case class PlayerBtssScore(player: OffPlayer, btsss: List[BtssScore] = List.empty) {
    def +(btssScore: BtssScore) = copy(player, btsss :+ btssScore)
  }
  case class PlayerBtssScores(players: List[PlayerBtssScore]) {

    def opponentPlayer(player: OffPlayer, board: OffBoard) = {
      val opponentNo = board.opponentOf(player.no) err s"can not find opponent no of ${player.no}"
      (players.find(_.player.no == opponentNo) err s"can not find opponent of ${player.no}").player
    }

    def sort: List[PlayerBtssScore] = players.sortWith { (playerBtssScore1, playerBtssScore2) =>
      {
        val s1 = playerBtssScore1.player.score
        val s2 = playerBtssScore2.player.score
        if (s1 > s2) true
        else if (s1 == s2) {
          playerBtssScore1.btsss.zip(playerBtssScore2.btsss).map {
            case (btssScore1, btssScore2) => {
              if (btssScore1.btss == OffBtss.No) {
                btssScore2.score - btssScore1.score
              } else {
                btssScore1.score - btssScore2.score
              }
            }
          }.foldLeft((true, true)) { // (result, isContinue)
            case (result, diff) => {
              if (result._2) {
                if (diff > 0) {
                  (true, false)
                } else if (diff < 0) {
                  (false, false)
                } else {
                  (true, true)
                }
              } else {
                result
              }
            }
          }._1
        } else false
      }
    }
  }

  case object Opponent extends OffBtss("opponent", "对手分") {

    override def score(boards: Boards, playerBtssScores: PlayerBtssScores): PlayerBtssScores = {
      PlayerBtssScores(
        playerBtssScores.players.map { playerBtssScore =>
          val player = playerBtssScore.player
          val score =
            boards.filter(_.contains(player.no)).foldLeft(0D) {
              case (s, b) => s + playerBtssScores.opponentPlayer(player, b).score
            }
          playerBtssScore + BtssScore(this, score)
        }
      )
    }
  }

  case object Mid extends OffBtss("mid", "中间分") {

    override def score(boards: Boards, playerBtssScores: PlayerBtssScores): PlayerBtssScores = {
      PlayerBtssScores(
        playerBtssScores.players.map { playerBtssScore =>
          val player = playerBtssScore.player
          val opponentScores = boards.filter(_.contains(player.no)).map { board =>
            (board, playerBtssScores.opponentPlayer(player, board).score)
          }.sortWith { (opponentScore1, opponentScore2) =>
            opponentScore1._2 < opponentScore2._2
          }

          val score = {
            if (opponentScores.size > 2) {
              // 去掉 最低分 最高分
              opponentScores.drop(1).reverse.drop(1).foldLeft(0D) {
                case (s, (_, os)) => s + os
              }
            } else 0
          }
          playerBtssScore + BtssScore(this, score)
        }
      )
    }
  }

  case object Sauber extends OffBtss("sauber", "索伯分") {

    override def score(boards: Boards, playerBtssScores: PlayerBtssScores): PlayerBtssScores = {
      PlayerBtssScores(
        playerBtssScores.players.map { playerBtssScore =>
          val player = playerBtssScore.player
          val score =
            boards.filter(_.contains(player.no)).foldLeft(0D) {
              case (s, b) => {
                val opponent = playerBtssScores.opponentPlayer(player, b)
                if (b.isWin(player.no)) s + opponent.score
                else if (b.isDraw) s + opponent.score / 2
                else s
              }
            }
          playerBtssScore + BtssScore(this, score)
        }
      )
    }
  }

  // 对手没走棋不算分
  case object Vict extends OffBtss("vict", "胜局数") {

    override def score(boards: Boards, playerBtssScores: PlayerBtssScores): PlayerBtssScores = {
      PlayerBtssScores(
        playerBtssScores.players.map { playerBtssScore =>
          val player = playerBtssScore.player
          val score =
            boards.filter(_.contains(player.no)).foldLeft(0D) {
              case (s, b) => {
                val opponent = playerBtssScores.opponentPlayer(player, b)
                val opponentNoStart = opponent.roundOutcome(b.roundNo).??(_ == OffBoard.Outcome.NoStart)
                if (b.isWin(player.no) && !opponentNoStart) s + 1
                else s
              }
            }
          playerBtssScore + BtssScore(this, score)
        }
      )
    }
  }

  case object Res extends OffBtss("res", "直胜") {

    override def score(boards: Boards, playerBtssScores: PlayerBtssScores): PlayerBtssScores = {
      PlayerBtssScores(
        playerBtssScores.players.map { playerBtssScore =>
          val player = playerBtssScore.player
          // 相同分的选手(不包括自己)
          val sameScorePlayers = playerBtssScores.players.filter(playerBtssScore => playerBtssScore.player.score == player.score && !playerBtssScore.player.is(player)).map(_.player)
          val score =
            boards.filter(_.contains(player.no)).foldLeft(0D) {
              case (s, b) => {
                // 对手
                val opponent = playerBtssScores.opponentPlayer(player, b)
                // 相同分对手
                if (sameScorePlayers.contains(opponent)) {
                  // 选手赢了+1分, 否则不变
                  if (b.isWin(player.no)) s + 1 else s
                } else s
              }
            }
          playerBtssScore + BtssScore(this, score)
        }
      )
    }
  }

  case object No extends OffBtss("no", "序号") {

    override def score(boards: Boards, playerBtssScores: PlayerBtssScores): PlayerBtssScores = {
      PlayerBtssScores(
        playerBtssScores.players.map { playerBtssScore =>
          val player = playerBtssScore.player
          playerBtssScore + BtssScore(this, player.no)
        }
      )
    }
  }

  val all = List(Opponent, Mid, Sauber, Vict, Res, No)

  val byId = all map { b => (b.id, b) } toMap

  def apply(id: String): OffBtss = byId get id err s"Bad Btss $id"

  def list = all.map { r => (r.id -> r.name) }

  def swissDefault = List(Opponent, Mid, Vict)
  def roundRobinDefault = List(Res, Vict, Sauber)
}

case class OffBtsss(list: List[OffBtss]) {

}