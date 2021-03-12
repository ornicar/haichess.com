package lila

package object contest extends PackageObject {

  private[contest] val logger = lila.log("contest")

  private[contest] val pairingLogger = logger branch "pairing"

  private[contest] type Players = List[contest.Player]

  private[contest] type Boards = List[contest.Board]

  private[contest] type Ranking = Map[lila.user.User.ID, Int]

}
