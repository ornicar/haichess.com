package lila.puzzle

import lila.db.dsl._
import Puzzle.{ BSONFields => F, Source }

private[puzzle] final class PuzzleFixApi(puzzleColl: Coll) {

  import Puzzle.puzzleBSONHandler

  val notImport = F.ipt $exists false
  val enabled = $or(
    F.voteRatio $gt AggregateVote.minRatio,
    F.voteNb $lt AggregateVote.minVotes
  )

  def find(source: Source, prevId: PuzzleId): Fu[Option[Puzzle]] =
    puzzleColl.find($doc(
      F.source -> source.id,
      F.id $gt prevId
    ) ++ notImport ++ enabled).sort($sort asc F.id).uno[Puzzle]

  def delete(id: PuzzleId): Funit =
    puzzleColl.remove($id(id)).void

}
