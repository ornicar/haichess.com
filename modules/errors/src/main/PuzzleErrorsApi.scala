package lila.errors

import lila.db.dsl._
import lila.user.User
import reactivemongo.bson._
import lila.common.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.paginator.Adapter
import lila.puzzle.{ Puzzle, PuzzleApi, PuzzleId }

final class PuzzleErrorsApi(coll: Coll, puzzleApi: PuzzleApi, bus: lila.common.Bus) {

  import BSONHandlers._

  def receive(res: Puzzle.UserResult): Funit =
    puzzleApi.puzzle.find(res.puzzleId) flatMap {
      case None => funit
      case Some(p) => {
        val error = PuzzleErrors.make(p, res.userId)
        coll.update(
          $id(error.id),
          error,
          upsert = true
        ).void
      }
    }

  def nextPuzzleErrors(puzzleId: PuzzleId, userId: User.ID, query: PuzzleQuery): Fu[Option[PuzzleErrors]] = {
    val condition = createQuery(userId, query) ++ $doc("puzzleId" $ne puzzleId)
    //println(BSONDocument.pretty(condition))
    coll.find(condition).sort(sort).uno[PuzzleErrors]
  }

  def removeByIds(ids: List[String]): Funit =
    coll.remove($inIds(ids)).void

  def removeById(id: Int, userId: User.ID): Funit =
    coll.remove($id(PuzzleErrors.makeId(userId, id))).void

  def page(page: Int, userId: User.ID, query: PuzzleQuery): Fu[Paginator[PuzzleErrors]] = {
    val condition = createQuery(userId, query)
    //println(BSONDocument.pretty(condition))

    Paginator(
      adapter = new Adapter(
        collection = coll,
        selector = condition,
        projection = $empty,
        sort = sort
      ),
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

  private def sort = $doc("rating" -> 1, "createAt" -> -1)

  private def createQuery(userId: User.ID, query: PuzzleQuery) = {
    var condition = $doc("createBy" -> userId)
    if (query.ratingMin.isDefined || query.ratingMax.isDefined) {
      var ratingRange = $doc()
      query.ratingMin foreach { ratingMin =>
        ratingRange = ratingRange ++ $gte(ratingMin)
      }
      query.ratingMax foreach { ratingMax =>
        ratingRange = ratingRange ++ $lte(ratingMax)
      }
      condition = condition ++ $doc("rating" -> ratingRange)
    }

    if (query.depthMin.isDefined || query.depthMax.isDefined) {
      var depthRange = $doc()
      query.depthMin foreach { stepsMin =>
        depthRange = depthRange ++ $gte(stepsMin)
      }
      query.depthMax foreach { stepsMax =>
        depthRange = depthRange ++ $lte(stepsMax)
      }
      condition = condition ++ $doc("depth" -> depthRange)
    }

    query.color foreach { tg =>
      val color = tg map { _.toLowerCase == "white" }
      condition = condition ++ $doc("color" -> $in(color: _*))
    }

    query.rating foreach { rating =>
      condition = condition ++ $doc("rating" -> $gte(rating))
    }

    /*    query.time foreach { time =>
      condition = condition ++ $doc("createAt" -> $lt(time.toLocalDateTime().toDate()))
    }*/
    condition
  }

}
