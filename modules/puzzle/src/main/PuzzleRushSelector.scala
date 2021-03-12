package lila.puzzle

import lila.db.dsl._
import lila.user.User
import Puzzle.{ BSONFields => P }
import scala.util.Random
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import reactivemongo.bson.BSONDocument
import scala.concurrent.duration._

private[puzzle] final class PuzzleRushSelector(puzzleColl: Coll, puzzleRoundApi: PuzzleRoundApi) {

  private val cache: Cache[PuzzleRush.ID, Puzzle] =
    Scaffeine().expireAfterWrite(10 minutes).build[PuzzleRush.ID, Puzzle]

  import Puzzle.puzzleBSONHandler

  val projection = $doc(P.taggers -> 0, P.likers -> 0)
  val notImport = P.ipt $exists false
  val enabled = $or(
    P.voteRatio $gt AggregateVote.minRatio,
    P.voteNb $lt AggregateVote.minVotes
  )
  val depth = $doc("depth" $lt 6)
  val retry = $doc("retry" -> false)
  val defaultFilter = enabled ++ retry ++ notImport
  val notGenerate = $doc("idHistory.source" $nin List("checkmate1", "checkmate2", "checkmate3"))

  val firstSkipMax = 1000
  val maxRating = 2600
  val minRatingStep = 40
  val maxRatingStep = 60
  val maxDepth = 5

  private def mergeMaxRating(query: CustomCondition) = query.ratingMax | maxRating

  private def mergeFirstSkipMax(query: CustomCondition) = if (query.isNone) firstSkipMax else 10

  private def mergeMinRatingStep(query: CustomCondition) = if (query.isNone) minRatingStep else 8

  private def mergeMaxRatingStep(query: CustomCondition) = if (query.isNone) maxRatingStep else 12

  private def mergeRatingInterval(query: CustomCondition) = if (query.isNone) 10 else 5

  case object NoPuzzlesAvailableException extends lila.base.LilaException {
    val message = "No puzzles available"
  }

  def apply(user: User, rushId: PuzzleRush.ID, condition: CustomCondition): Fu[Puzzle] = {
    if (!condition.isNone && condition.isRandom) {
      puzzleRoundApi.rushRounds(rushId) flatMap { list =>
        random(condition, list.map(_.puzzleId))
      }
    } else {
      puzzleRoundApi.rushLastRound(rushId) flatMap {
        case None => selectFirst(condition)
        case Some(r) => {
          if (r.puzzleRating >= mergeMaxRating(condition)) {
            selectFirst(condition)
          } else {
            selectNext(r, condition)
          }
        }
      }
    }
  } flattenWith NoPuzzlesAvailableException addEffect { cache.put(rushId, _) }

  def last(user: User, rushId: PuzzleRush.ID, condition: CustomCondition): Fu[Puzzle] = {
    cache.getIfPresent(rushId) match {
      case Some(p) => fuccess(p)
      case None => apply(user, rushId, condition)
    }
  }

  private def selectFirst(condition: CustomCondition): Fu[Option[Puzzle]] = {
    val $filter = filter(condition)
    //println("----------------------------first--------------------------------")
    //println(BSONDocument.pretty($filter))
    puzzleColl.countSel($filter) flatMap { count =>
      val skip = math.min(mergeFirstSkipMax(condition), math.max(count, 1))
      puzzleColl.find($filter, projection)
        .sort($sort asc P.rating)
        .skip(Random nextInt skip)
        .uno[Puzzle]
    }
  }

  private def selectNext(prev: PuzzleRound, condition: CustomCondition, retry: Int = 0): Fu[Option[Puzzle]] = {
    val nextMinRatingStep = Random.nextInt(mergeMaxRatingStep(condition) - mergeMinRatingStep(condition)) + mergeMinRatingStep(condition)
    val minRating = math.min(prev.puzzleRating + nextMinRatingStep, mergeMaxRating(condition))
    val maxRating = math.min(minRating + mergeRatingInterval(condition) + 5 * retry, mergeMaxRating(condition))
    val $filter = filter(condition, minRating.some, maxRating.some)
    //println("----------------------------next--------------------------------")
    //println(BSONDocument.pretty($filter))
    puzzleColl.countSel($filter) flatMap { count =>
      val skp = math.min(100, math.max(count, 1))
      puzzleColl.find($filter, projection)
        .sort($sort asc P.rating)
        .skip(Random nextInt skp)
        .uno[Puzzle]
    }
  } flatMap {
    // 没有查到分为两种情况
    case None => {
      // 1.下次查询的分数 > 最大分数 => 循环到第一条
      if (prev.puzzleRating + mergeMaxRatingStep(condition) + mergeRatingInterval(condition) + 5 * retry > mergeMaxRating(condition)) {
        selectFirst(condition)
      } else {
        // 2.在当前分数段内查询不到数据 => 扩大分数段
        selectNext(prev, condition, retry + 1)
      }
    }
    case res => fuccess(res)
  }

  private def random(condition: CustomCondition, ids: List[PuzzleId]) = {
    val $filter = filter(condition) ++ $doc(P.id $nin ids)
    //    println("----------------------------next--------------------------------")
    //    println(BSONDocument.pretty($filter))
    puzzleColl.countSel($filter) flatMap { count =>
      val skp = math.max(count, 1)
      puzzleColl.find($filter, projection)
        .skip(Random nextInt skp)
        .uno[Puzzle]
    }
  }

  private def filter(query: CustomCondition, r1: Option[Int] = None, r2: Option[Int] = None) = {
    var $filter = defaultFilter
    if ((query.ratingMin.isDefined || query.ratingMax.isDefined) || (r1.isDefined || r2.isDefined)) {
      var ratingRange = $empty
      if (query.ratingMin.isDefined || r1.isDefined) {
        val r = math.max(r1 | 0, query.ratingMin | 0)
        ratingRange = ratingRange ++ $gte(r)
      }

      if (query.ratingMax.isDefined || r2.isDefined) {
        val r = math.min(r2 | 3000, query.ratingMax | 3000)
        ratingRange = ratingRange ++ $lte(r)
      }
      $filter = $filter ++ $doc(P.rating -> ratingRange)
    } else {
      $filter = $filter ++ $doc(P.rating $lte maxRating)
    }

    if (query.stepsMin.isDefined || query.stepsMax.isDefined) {
      var stepsRange = $empty
      query.stepsMin foreach { stepsMin =>
        stepsRange = stepsRange ++ $gte(stepsMin)
      }
      query.stepsMax foreach { stepsMax =>
        stepsRange = stepsRange ++ $lte(stepsMax)
      }
      $filter = $filter ++ $doc(P.depth -> stepsRange)
    } else {
      $filter = $filter ++ $doc(P.depth $lte maxDepth)
    }

    query.color foreach { c =>
      $filter = $filter ++ $doc(P.white -> (c == "White"))
    }

    query.phase foreach { p =>
      $filter = $filter ++ $doc(s"${P.mark}.phase" -> p)
    }
    $filter
  }

}
