package lila.contest

import lila.user.User

case class ScoreSheet(
    id: ScoreSheet.ID,
    contestId: Contest.ID,
    roundNo: Round.No,
    playerUid: User.ID,
    playerNo: Player.No,
    score: Double,
    rank: Int,
    btssScores: List[Btss.BtssScore],
    cancelled: Boolean = false
)

object ScoreSheet {

  type ID = String

  private[contest] def makeId(playerId: String, no: Round.No) = playerId + "@" + no

}