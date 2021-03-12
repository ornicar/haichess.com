package lila.offlineContest

import lila.common.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User
import reactivemongo.bson.BSONDocument

class OffContestPager {

  import BSONHandlers.contestHandler

  def current(me: User, page: Int, status: Option[OffContest.Status], text: String) = paginator(
    me,
    page,
    text,
    OffContestRepo.currentSelect ++ status ?? OffContestRepo.statusSelect,
    OffContestRepo.startAsc
  )

  def history(me: User, page: Int, status: Option[OffContest.Status], text: String) = paginator(
    me,
    page,
    text,
    OffContestRepo.historySelect ++ status ?? OffContestRepo.statusSelect,
    OffContestRepo.startAsc
  )

  private def paginator(me: User, page: Int, text: String, $selector: BSONDocument, $order: BSONDocument): Fu[Paginator[OffContest]] = {
    val textOption = if (text.trim.isEmpty) none else text.trim.some
    Paginator[OffContest](adapter = new Adapter[OffContest](
      collection = OffContestRepo.coll,
      selector = $doc("createdBy" -> me.id) ++ $selector ++ textOption ?? (t => $doc("name" $regex (t, "i"))),
      projection = $empty,
      sort = $order
    ), currentPage = page, maxPerPage = MaxPerPage(16))
  }

}
