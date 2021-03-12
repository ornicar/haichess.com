package lila.puzzle

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime

private[puzzle] final class PuzzleRushRankHistoryApi(puzzleRushRankHistoryColl: Coll) {

  import PuzzleRushRankHistory.PuzzleRushRankHistoryBSONHandler

  def id(mode: PuzzleRush.Mode, userId: User.ID) = s"${mode.id}@${userId}"

  def byId(rush: PuzzleRush): Fu[Option[PuzzleRushRankHistory]] = puzzleRushRankHistoryColl.byId(PuzzleRushRankHistory.makeId(rush))

  def createBySub(rush: PuzzleRush): Funit = byId(rush) flatMap {
    case None => create(rush)
    case Some(r) => if (rush.result.??(_.win) > r.win) {
      create(rush)
    } else funit
  }

  def create(rush: PuzzleRush): Funit =
    puzzleRushRankHistoryColl.update(
      $id(PuzzleRushRankHistory.makeId(rush)),
      PuzzleRushRankHistory.make(rush),
      upsert = true
    ).void

  def userRank(mode: PuzzleRush.Mode, userId: User.ID) =
    puzzleRushRankHistoryColl.byId(id(mode, userId)) flatMap { rank =>
      rank.fold(fuccess(-1, -1)) { r =>
        userRankNo(mode, r.updateTime, r.win).map { no =>
          ((no + 1), r.win)
        }
      }
    }

  def userRankNo(mode: PuzzleRush.Mode, updateTime: DateTime, win: Int): Fu[Int] = puzzleRushRankHistoryColl.countSel(
    $doc(
      "mode" -> mode.id,
      "win" $gt win
    )
  ) zip puzzleRushRankHistoryColl.countSel(
      $doc(
        "mode" -> mode.id,
        "win" -> win,
        "updateTime" $gt updateTime
      )
    ) map (gtAndEq => gtAndEq._1 + gtAndEq._2)

  def rankList(mode: PuzzleRush.Mode, userIds: Option[List[User.ID]] = None): Fu[List[PuzzleRushRankHistory]] =
    puzzleRushRankHistoryColl.find($doc(
      "mode" -> mode.id
    ) ++ userIds.?? { uids =>
        $doc("userId" -> $in(uids: _*))
      })
      .sort($doc("win" -> -1, "updateTime" -> -1))
      .list[PuzzleRushRankHistory](20)

}
