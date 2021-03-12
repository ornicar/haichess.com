package lila

package object offlineContest extends PackageObject {

  private[offlineContest] val logger = lila.log("offlineContest")

  private[offlineContest] val pairingLogger = logger branch "pairing"

  private[offlineContest] type Players = List[OffPlayer]

  private[offlineContest] type Boards = List[OffBoard]

  private[offlineContest] type Ranking = Map[lila.user.User.ID, Int]

}
