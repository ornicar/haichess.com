package lila.contest

import BSONHandlers._
import lila.common.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import reactivemongo.bson.BSONDocument
import lila.user.User

class ContestPager {

  def all(page: Int, status: Option[Contest.Status], text: String) = paginator(
    page,
    text,
    ContestRepo.allSelect ++ status ?? ContestRepo.statusSelect,
    ContestRepo.startAsc
  )

  def belong(page: Int, status: Option[Contest.Status], me: User, text: String) =
    PlayerRepo.getByUserId(me.id) flatMap { list =>
      paginator(
        page,
        text,
        ContestRepo.idsSelect(list.map(_.contestId)) ++ ContestRepo.belongSelect ++ status ?? ContestRepo.statusSelect,
        ContestRepo.startDesc
      )
    }

  def owner(page: Int, status: Option[Contest.Status], me: User, text: String) = paginator(
    page,
    text,
    ContestRepo.createBySelect(me.id) ++ ContestRepo.ownerSelect ++ status ?? ContestRepo.statusSelect,
    ContestRepo.startDesc
  )

  def finish(page: Int, status: Option[Contest.Status], text: String) = paginator(
    page,
    text,
    ContestRepo.finishedOrCancelSelect ++ status ?? ContestRepo.statusSelect,
    ContestRepo.startDesc
  )

  private def paginator(page: Int, text: String, $selector: BSONDocument, $order: BSONDocument) = {
    val textOption = if (text.trim.isEmpty) none else text.trim.some
    Paginator[Contest](adapter = new Adapter[Contest](
      collection = ContestRepo.coll,
      selector = $selector ++ textOption ?? (t => $doc("name" $regex (t, "i"))),
      projection = $empty,
      sort = $order
    ), currentPage = page, maxPerPage = MaxPerPage(16))
  }

}
