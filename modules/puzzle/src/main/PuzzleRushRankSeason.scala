package lila.puzzle

import lila.user.User
import org.joda.time.DateTime

case class PuzzleRushRankSeason(
    id: PuzzleRushRankSeason.ID,
    mode: String,
    userId: User.ID,
    season: Int,
    rushId: PuzzleRush.ID,
    win: Int,
    updateTime: DateTime
) {

}

object PuzzleRushRankSeason {

  type ID = String

  def make(rush: PuzzleRush): PuzzleRushRankSeason = PuzzleRushRankSeason(
    id = makeId(rush),
    mode = rush.mode.id,
    userId = rush.userId,
    season = rush.season,
    rushId = rush.id,
    win = rush.result ?? { _.win },
    updateTime = DateTime.now
  )

  def makeId(rush: PuzzleRush): String = s"${rush.season}@${rush.mode.id}@${rush.userId}"

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit val PuzzleRushRankMonthBSONHandler = Macros.handler[PuzzleRushRankSeason]

}

