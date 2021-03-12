package lila.puzzle

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime

private[puzzle] final class PuzzleRushRankTodayApi(puzzleRushRankTodayColl: Coll) {

  import PuzzleRushRankToday.PuzzleRushRankTodayBSONHandler

  def id(mode: PuzzleRush.Mode, date: Int, userId: User.ID) = s"${date}@${mode.id}@${userId}"

  def byId(rush: PuzzleRush): Fu[Option[PuzzleRushRankToday]] = puzzleRushRankTodayColl.byId(PuzzleRushRankToday.makeId(rush))

  def createBySub(rush: PuzzleRush): Funit = byId(rush) flatMap {
    case None => create(rush)
    case Some(r) => if (rush.result.??(_.win) > r.win) {
      create(rush)
    } else funit
  }

  def create(rush: PuzzleRush) =
    puzzleRushRankTodayColl.update(
      $id(PuzzleRushRankToday.makeId(rush)),
      PuzzleRushRankToday.make(rush),
      upsert = true
    ).void

  def userRank(mode: PuzzleRush.Mode, date: Int, userId: User.ID) =
    puzzleRushRankTodayColl.byId(id(mode, date, userId)) flatMap { rank =>
      rank.fold(fuccess(-1, -1)) { r =>
        userRankNo(mode, date, r.updateTime, r.win).map { no =>
          ((no + 1), r.win)
        }
      }
    }

  def userRankNo(mode: PuzzleRush.Mode, date: Int, updateTime: DateTime, win: Int): Fu[Int] = puzzleRushRankTodayColl.countSel(
    $doc(
      "mode" -> mode.id,
      "date" -> date,
      "win" $gt win
    )
  ) zip puzzleRushRankTodayColl.countSel(
      $doc(
        "mode" -> mode.id,
        "date" -> date,
        "win" -> win,
        "updateTime" $gt updateTime
      )
    ) map (gtAndEq => gtAndEq._1 + gtAndEq._2)

  def rankList(mode: PuzzleRush.Mode, date: Int, userIds: Option[List[User.ID]] = None): Fu[List[PuzzleRushRankHistory]] =
    puzzleRushRankTodayColl.find(
      $doc(
        "mode" -> mode.id,
        "date" -> date
      ) ++ userIds.?? { uids =>
          $doc("userId" -> $in(uids: _*))
        }
    )
      .sort($doc("win" -> -1, "updateTime" -> -1))
      .list[PuzzleRushRankHistory](20)

}
