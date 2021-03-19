package lila.puzzle

import lila.common.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import Puzzle.{ BSONFields => F }
import lila.user.User
import lila.resource.DataForm.puzzle.{ ImportedData, LikedData, ThemeData }
import reactivemongo.bson.BSONDocument

private[puzzle] final class PuzzleResource(
    puzzleColl: Coll,
    api: PuzzleApi,
    bus: lila.common.Bus
) {

  val projection = $doc(F.taggers -> 0, F.likers -> 0)

  val enabled = $or(
    F.voteRatio $gt AggregateVote.minRatio,
    F.voteNb $lt AggregateVote.minVotes
  )

  def likedTags(userId: String): Fu[Set[String]] = api.tagger.tagsByUser(userId)

  def liked(page: Int, userId: String, query: LikedData): Fu[Paginator[Puzzle]] = {
    var condition = $doc("user" -> userId)
    query.tags.foreach { tg =>
      condition = condition ++ $doc("tags" -> $in(tg: _*))
    }

    api.tagger.puzzleIds(condition) flatMap { ids =>
      Paginator(
        adapter = new Adapter(
          collection = puzzleColl,
          selector = $inIds(ids) ++ enabled,
          projection = projection,
          sort = $doc(F.rating -> query.sortOrder)
        ),
        currentPage = page,
        maxPerPage = MaxPerPage(15)
      )
    }
  }

  def importedTags(userId: String): Fu[Set[String]] =
    puzzleColl.distinct[String, Set](s"${F.ipt}.tags", ($doc(s"${F.ipt}.userId" -> userId) ++ enabled).some)

  def imported(page: Int, userId: String, query: ImportedData): Fu[Paginator[Puzzle]] = {
    var condition = $doc(s"${F.ipt}.userId" -> userId) ++ enabled
    query.tags foreach { tg =>
      condition = condition ++ $doc(s"${F.ipt}.tags" -> $in(tg: _*))
    }

    Paginator(
      adapter = new Adapter(
        collection = puzzleColl,
        selector = condition,
        projection = projection,
        sort = $sort desc F.date
      ),
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

  /*
    可以按照如下语法优化
    db.puzzle.aggregate(
    { $group: { _id: "$mark.tag", count: { $sum: 1 }}},
    { $sort: { count: -1 } }
  )*/
  def themeTags(): Fu[Set[String]] =
    puzzleColl.distinct[String, Set](s"${F.mark}.tag", ($doc(F.mark $exists true, s"${F.mark}.tag" $exists true) ++ enabled).some)

  def theme(page: Int, userId: String, query: ThemeData): Fu[Paginator[Puzzle]] = {
    //val p = BSONDocument.pretty(condition)
    Paginator(
      adapter = new Adapter(
        collection = puzzleColl,
        selector = themeSearchCondition(query),
        projection = projection,
        sort = $doc(F.rating -> query.sortOrder)
      ),
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

  def themeSearchCondition(query: ThemeData, fromPuzzleId: Option[Int] = None) = {
    var condition = $doc(F.mark $exists true) ++ enabled

    if (query.ratingMin.isDefined || query.ratingMax.isDefined) {
      var ratingRange = $doc()
      query.ratingMin foreach { ratingMin =>
        ratingRange = ratingRange ++ $gte(ratingMin)
      }
      query.ratingMax foreach { ratingMax =>
        ratingRange = ratingRange ++ $lte(ratingMax)
      }
      condition = condition ++ $doc(F.rating -> ratingRange)
    }

    if (query.stepsMin.isDefined || query.stepsMax.isDefined) {
      var stepsRange = $doc()
      query.stepsMin foreach { stepsMin =>
        stepsRange = stepsRange ++ $gte(stepsMin)
      }
      query.stepsMax foreach { stepsMax =>
        stepsRange = stepsRange ++ $lte(stepsMax)
      }
      condition = condition ++ $doc(F.depth -> stepsRange)
    }

    query.pieceColor foreach { tg =>
      val color = tg map { _.toLowerCase == "white" }
      condition = condition ++ $doc(s"${F.white}" -> $in(color: _*))
    }

    query.tags foreach { tg =>
      condition = condition ++ $doc(s"${F.mark}.tag" -> $in(tg: _*))
    }

    query.phase foreach { tg =>
      condition = condition ++ $doc(s"${F.mark}.phase" -> $in(tg: _*))
    }

    query.moveFor foreach { tg =>
      condition = condition ++ $doc(s"${F.mark}.moveFor" -> $in(tg: _*))
    }

    query.subject foreach { tg =>
      condition = condition ++ $doc(s"${F.mark}.subject" -> $in(tg: _*))
    }

    query.strength foreach { tg =>
      condition = condition ++ $doc(s"${F.mark}.strength" -> $in(tg: _*))
    }

    query.chessGame foreach { tg =>
      condition = condition ++ $doc(s"${F.mark}.chessGame" -> $in(tg: _*))
    }

    query.comprehensive foreach { tg =>
      condition = condition ++ $doc(s"${F.mark}.comprehensive" -> $in(tg: _*))
    }

    fromPuzzleId foreach { minPuzzleId =>
      condition = condition ++ $doc(F.id -> $gt(minPuzzleId))
    }
    println(BSONDocument.pretty(condition))
    condition
  }

  def disableByIds(ids: List[PuzzleId], user: User): Funit =
    puzzleColl.update(
      $inIds(ids) ++ $doc(s"${F.ipt}.userId" -> user.id),
      $doc("$set" -> $doc(F.vote -> AggregateVote.disable)),
      multi = true
    ).void >>- bus.publish(lila.hub.actorApi.resource.PuzzleResourceRemove(ids), 'puzzleResourceRemove)

  def capsule(page: Int, ids: List[PuzzleId]): Fu[Paginator[Puzzle]] = {
    Paginator(
      adapter = new Adapter(
        collection = puzzleColl,
        selector = $inIds(ids) ++ enabled,
        projection = projection,
        sort = $doc(F.rating -> 1)
      ),
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

}
