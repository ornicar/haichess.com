package lila.coach

import lila.db.dsl._
import reactivemongo.api._
import lila.common.paginator.Paginator
import lila.db.paginator.Adapter
import lila.user.{ User, UserRepo }

final class CoachPager(coll: Coll) {

  val maxPerPage = lila.common.MaxPerPage(15)

  import BsonHandlers._

  def apply(page: Int, status: Certify.Status): Fu[Paginator[Coach.WithUser]] = {
    val adapter = new Adapter[Coach](
      collection = coll,
      selector = $doc("certify.status" -> status.id),
      projection = $empty,
      sort = $sort desc "certify.applyAt"
    ) mapFutureList withUsers
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }

  private def withUsers(coaches: Seq[Coach]): Fu[Seq[Coach.WithUser]] =
    UserRepo.withColl {
      _.optionsByOrderedIds[User, User.ID](coaches.map(_.id.value), ReadPreference.secondaryPreferred)(_.id)
    } map { users =>
      coaches zip users collect {
        case (coach, Some(user)) => Coach.WithUser(coach, user)
      }
    }
}

object CoachPager {

}
