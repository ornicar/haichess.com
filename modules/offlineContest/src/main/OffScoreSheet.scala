package lila.offlineContest

import lila.user.User

case class OffScoreSheet(
    id: OffScoreSheet.ID,
    contestId: OffContest.ID,
    roundNo: OffRound.No,
    playerUid: User.ID,
    playerNo: OffPlayer.No,
    score: Double,
    rank: Int,
    btssScores: List[OffBtss.BtssScore]
)

object OffScoreSheet {

  type ID = String

  private[offlineContest] def makeId(playerId: String, no: OffRound.No) = playerId + "@" + no

}