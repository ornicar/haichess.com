package lila.puzzle

import lila.db.dsl._
import PuzzleRound.{ BSONFields => F }

private[puzzle] final class PuzzleRoundApi(puzzleRoundColl: Coll) {

  import PuzzleRound.PuzzleRoundBSONHandler

  def createBySub(res: lila.puzzle.Puzzle.UserResult): Funit =
    puzzleRoundColl.insert(PuzzleRound.makeByResult(res)).void

  def rushRounds(rushId: PuzzleRush.ID, filterTimeout: Boolean = true): Fu[List[PuzzleRound]] =
    puzzleRoundColl.find(
      $doc(
        F.rushId -> rushId
      ) ++ filterTimeout.?? {
          $doc(F.timeout $exists false)
        }
    ).sort($sort asc F.createTime).list[PuzzleRound]()

  def rushLastRound(rushId: PuzzleRush.ID): Fu[Option[PuzzleRound]] =
    puzzleRoundColl.find($doc(F.rushId -> rushId, F.timeout $exists false)).sort($sort desc F.createTime).uno[PuzzleRound]

}
