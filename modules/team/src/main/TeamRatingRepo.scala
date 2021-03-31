package lila.team

import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.common.paginator.Paginator
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import lila.user.User

import scala.math.BigDecimal.RoundingMode

object TeamRatingRepo {

  private val coll = Env.current.colls.rating
  import BSONHandlers._

  def byId(id: String): Fu[Option[TeamRating]] = coll.byId[TeamRating](id)

  def findByUser(userId: String): Fu[List[TeamRating]] =
    coll.find(userQuery(userId)).sort($doc("createAt" -> -1)).list[TeamRating]()

  def findByContest(contestId: String): Fu[Map[User.ID, Double]] =
    coll.find($doc("metaData.contestId" -> contestId)).sort($doc("createAt" -> -1)).list[TeamRating]().map { list =>
      list.groupBy(_.userId).map {
        case (userId, list) => {
          userId -> {
            val diff = list.foldLeft(0.0d) {
              case (num, tr) => num + tr.diff
            }
            BigDecimal(diff).setScale(1, RoundingMode.DOWN).doubleValue()
          }
        }
      }
    }

  def historyData(userId: String): Fu[JsArray] = findByUser(userId).map { list =>
    JsArray(
      list.groupBy { tr =>
        tr.createAt.getYear + "/" + tr.createAt.getMonthOfYear + "/" + tr.createAt.getDayOfMonth
      }.map {
        case (date, list) => date -> {
          val last = list.maxBy(_.createAt)
          (last.rating + last.diff).toInt
        }
      }.toSeq.sortBy(_._1).map(d => Json.obj("date" -> d._1, "rating" -> d._2))
    )
  }

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
