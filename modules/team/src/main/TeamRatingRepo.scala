package lila.team

import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.common.paginator.Paginator

object TeamRatingRepo {

  private val coll = Env.current.colls.rating
  import BSONHandlers._

  def byId(id: String): Fu[Option[TeamRating]] = coll.byId[TeamRating](id)

  def findByUser(userId: String): Fu[List[TeamRating]] =
    coll.find(userQuery(userId)).sort($doc("createAt" -> -1)).list[TeamRating]()

  def insert(rating: TeamRating): Funit = coll.insert(rating).void

  def page(page: Int, userId: String): Fu[Paginator[TeamRating]] = {
    val adapter = new Adapter[TeamRating](
      collection = coll,
      selector = userQuery(userId),
      projection = $empty,
      sort = $doc("createAt" -> -1)
    )
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = lila.common.MaxPerPage(30)
    )
  }

  def userQuery(userId: String) = $doc("userId" -> userId)

}
