package lila.puzzle

import lila.user.User
import org.joda.time.DateTime

case class PuzzleRushRankHistory(
    id: PuzzleRushRankHistory.ID,
    mode: String,
    userId: User.ID,
    rushId: PuzzleRush.ID,
    win: Int,
    updateTime: DateTime
) {

}

object PuzzleRushRankHistory {

  type ID = String

  def make(rush: PuzzleRush): PuzzleRushRankHistory = PuzzleRushRankHistory(
    id = makeId(rush),
    mode = rush.mode.id,
    userId = rush.userId,
    rushId = rush.id,
    win = rush.result ?? { _.win },
    updateTime = DateTime.now
  )

  def makeId(rush: PuzzleRush): String = s"${rush.mode.id}@${rush.userId}"

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit val PuzzleRushRankHistoryBSONHandler = Macros.handler[PuzzleRushRankHistory]

}
