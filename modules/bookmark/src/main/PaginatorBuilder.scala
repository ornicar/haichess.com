package lila.bookmark

import lila.common.paginator._
import lila.db.dsl._
import lila.game.GameRepo
import lila.user.User

private[bookmark] final class PaginatorBuilder(
    coll: Coll,
    maxPerPage: lila.common.MaxPerPage
) {

  def byUser(user: User, page: Int, tags: Option[List[String]] = None): Fu[Paginator[Bookmark]] =
    paginator(new UserAdapter(user, tags), page)

  private def paginator(adapter: AdapterLike[Bookmark], page: Int): Fu[Paginator[Bookmark]] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final class UserAdapter(user: User, tags: Option[List[String]] = None) extends AdapterLike[Bookmark] {

    def nbResults: Fu[Int] = coll countSel selector

    def slice(offset: Int, length: Int): Fu[Seq[Bookmark]] = for {
      gameIds ← coll.find(selector, $doc("g" -> true))
        .sort(sorting)
        .skip(offset)
        .cursor[Bdoc]()
        .gather[List](length) map { _ flatMap { _.getAs[String]("g") } }
      games ← GameRepo gamesFromSecondary gameIds
    } yield games map { g => Bookmark(g, user) }

    private def selector = {
      var s = $doc("u" -> user.id)
      tags foreach { tg =>
        s = s ++ $doc("t" -> $in(tg: _*))
      }
      s
    }
    private def sorting = $sort desc "d"
  }
}
