package lila.puzzle

import scala.util.Random
import lila.db.dsl._
import lila.user.User
import Puzzle.{ BSONFields => F }
import lila.hub.actorApi.puzzle.{ NextPuzzle, NextThemePuzzle }
import reactivemongo.bson.BSONDocument

private[puzzle] final class Selector(
    puzzleColl: Coll,
    api: PuzzleApi,
    puzzleIdMin: Int,
    bus: lila.common.Bus
) {

  val notImport = F.ipt $exists false

  val projection = $doc(F.taggers -> 0, F.likers -> 0)

  import Selector._

  def byId(id: PuzzleId): Fu[Option[Puzzle]] = {
    puzzleColl.byId[Puzzle](id)
  }

  def nextPuzzle(me: Option[User]): Fu[Puzzle] = {
    lila.mon.puzzle.selector.count()
    me match {
      // anon
      case None => puzzleColl // this query precisely matches a mongodb partial index
        .find($doc( /*F.voteNb $gte 50*/ ) ++ notImport, projection)
        .sort($sort desc F.voteRatio)
        .skip(Random nextInt anonSkipMax)
        .uno[Puzzle]
      // user
      case Some(user) => api.head find user flatMap {
        // new player
        case None => api.puzzle find puzzleIdMin flatMap { puzzleOption =>
          puzzleOption ?? { p => api.head.addNew(user, p.id) } inject puzzleOption
        }
        // current puzzle
        case Some(PuzzleHead(_, Some(current), _)) => api.puzzle find current
        // find new based on last
        case Some(PuzzleHead(_, _, last)) => newPuzzleForUser(user, last) flatMap {
          // user played all puzzles. Reset rounds and start anew.
          case None => api.puzzle.cachedLastId.get flatMap { maxId =>
            (last > maxId - 1000) ?? {
              api.round.reset(user) >> api.puzzle.find(puzzleIdMin)
            }
          }
          case Some(found) => fuccess(found.some)
        } flatMap { puzzleOption =>
          puzzleOption ?? { p => api.head.addNew(user, p.id) } inject puzzleOption
        }
      }
    }
  }.mon(_.puzzle.selector.time) flattenWith NoPuzzlesAvailableException addEffect { puzzle =>
    if (puzzle.vote.sum < -1000) {
      logger.info(s"Select #${puzzle.id} vote.sum: ${puzzle.vote.sum} for ${me.fold("Anon")(_.username)} (${me.fold("?")(_.perfs.puzzle.intRating.toString)})")
    } else {
      lila.mon.puzzle.selector.vote(puzzle.vote.sum)
    }
    me.?? { u =>
      bus.publish(NextPuzzle(puzzle.id, u.id), 'nextPuzzle)
    }
  }

  def nextThemePuzzle(user: User, themeSearchCondition: BSONDocument, queryString: String): Fu[Option[Puzzle]] = {
    puzzleColl.find(themeSearchCondition, projection)
      .sort($sort asc F.id)
      .uno[Puzzle].map { po =>
        po.?? { p =>
          bus.publish(NextThemePuzzle(p.id, user.id, queryString), 'nextThemePuzzle)
        }
        po
      }
  }

  def nextCapsulePuzzle(lastPlayed: Option[PuzzleId], ids: List[PuzzleId]): Fu[Option[Puzzle]] = {
    if (ids.isEmpty) fuccess(none)
    else {
      val sortedIds = ids.sortWith((id1, id2) => id1 < id2)
      lastPlayed.fold(sortedIds.headOption) { lp =>
        sortedIds.find(_ > lp)
      } match {
        case None => fuccess(none)
        case Some(nextPuzzleId) => puzzleColl.byId[Puzzle](nextPuzzleId)
      }
    }
  }

  def currentHomeworkPuzzle(id: PuzzleId): Fu[Option[Puzzle]] = {
    puzzleColl.byId[Puzzle](id)
  }

  def nextHomeworkPuzzle(lastPlayed: PuzzleId, mappingArr: Array[(PuzzleId, Boolean)]): Fu[Option[Puzzle]] = {
    if (mappingArr.forall(_._2)) fuccess(none)
    else {
      val idArr = mappingArr.map(_._1)
      val lastIndex = idArr.indexOf(lastPlayed)
      if (lastIndex < 0) {
        fufail(s"can not find puzzle of $lastPlayed")
      } else {

        def nextPuzzleIndex(lastIndex: Int): Int = {
          var nextIndex = lastIndex + 1
          if (nextIndex > idArr.length - 1) {
            nextIndex = 0
          }

          if (mappingArr(nextIndex)._2) {
            nextPuzzleIndex(nextIndex)
          } else nextIndex
        }

        val npi = nextPuzzleIndex(lastIndex)
        puzzleColl.byId[Puzzle](idArr(npi))
      }
    }
  }

  private def newPuzzleForUser(user: User, lastPlayed: PuzzleId): Fu[Option[Puzzle]] = {
    val rating = user.perfs.puzzle.intRating atMost 2300 atLeast 600
    val step = toleranceStepFor(rating, user.perfs.puzzle.nb)
    tryRange(
      rating = rating,
      tolerance = step,
      step = step,
      idRange = Range(lastPlayed, lastPlayed + 200)
    )
  }

  private def tryRange(
    rating: Int,
    tolerance: Int,
    step: Int,
    idRange: Range
  ): Fu[Option[Puzzle]] = puzzleColl.find(rangeSelector(
    rating = rating,
    tolerance = tolerance,
    idRange = idRange
  ) ++ notImport).sort($sort asc F.id).uno[Puzzle] flatMap {
    case None if (tolerance + step) <= toleranceMax =>
      tryRange(rating, tolerance + step, step, Range(idRange.min, idRange.max + 100))
    case res => fuccess(res)
  }

}

private final object Selector {

  case object NoPuzzlesAvailableException extends lila.base.LilaException {
    val message = "No puzzles available"
  }

  val toleranceMax = 1000

  val anonSkipMax = 50

  val balancer = 50

  def toleranceStepFor(rating: Int, nbPuzzles: Int) = {
    math.abs(1500 - rating) match {
      case d if d >= 500 => 300
      case d if d >= 300 => 250
      case d => 200
    }
  } * {
    // increase rating tolerance for puzzle blitzers,
    // so they get more puzzles to play
    if (nbPuzzles > 10000) 2
    else if (nbPuzzles > 5000) 3 / 2
    else 1
  }

  def rangeSelector(rating: Int, tolerance: Int, idRange: Range) = $doc(
    F.id $gt idRange.min $lt idRange.max,
    F.rating $gt (rating - tolerance) $lt (rating + tolerance - balancer),
    $or(
      F.voteRatio $gt AggregateVote.minRatio,
      F.voteNb $lt AggregateVote.minVotes
    )
  )
}
