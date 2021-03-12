package lila.puzzle

import lila.user.User
import org.joda.time.DateTime

case class PuzzleRushRankToday(
    id: PuzzleRushRankToday.ID,
    date: Int,
    mode: String,
    userId: User.ID,
    rushId: PuzzleRush.ID,
    win: Int,
    updateTime: DateTime
) {

}

object PuzzleRushRankToday {

  type ID = String

  def make(rush: PuzzleRush): PuzzleRushRankToday = PuzzleRushRankToday(
    id = makeId(rush),
    date = date(rush),
    mode = rush.mode.id,
    userId = rush.userId,
    rushId = rush.id,
    win = rush.result ?? { _.win },
    updateTime = DateTime.now
  )

  def date(rush: PuzzleRush) = rush.endTime ?? { _.toString("yyyyMMdd").toInt }

  def makeId(rush: PuzzleRush): String = s"${date(rush)}@${rush.mode.id}@${rush.userId}"

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit val PuzzleRushRankTodayBSONHandler = Macros.handler[PuzzleRushRankToday]

}
