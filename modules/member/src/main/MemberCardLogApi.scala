package lila.member

import lila.db.dsl._
import lila.user.User
import ornicar.scalalib.Random
import org.joda.time.DateTime
import lila.common.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.paginator.Adapter
import lila.member.MemberCard.CardStatus

case class MemberCardLogApi(coll: Coll) {

  import BSONHandlers.MemberCardLogBSONHandler

  def byId(id: User.ID): Fu[Option[MemberCardLog]] = coll.byId[MemberCardLog](id)

  def setLog(
    userId: User.ID,
    card: MemberCard,
    typ: MemberCardLog.Type,
    note: Option[String]
  ): Funit = coll.insert(
    MemberCardLog(
      _id = Random nextString 8,
      cardId = card.id,
      level = card.level,
      days = card.days,
      validDays = card.validDays,
      status = card.status,
      expireAt = card.expireAt,
      typ = typ,
      oldUserId = card.userId,
      newUserId = userId,
      note = note,
      createAt = DateTime.now
    )
  ).void

  def hasSignupGiven(userId: User.ID): Fu[Boolean] = coll.exists(
    $doc("status" -> MemberCardLog.Type.SignupGiven.id, "newUserId" -> userId)
  )

  def setStatus(cardId: String, status: CardStatus): Funit =
    coll.update(
      $doc("cardId" -> cardId),
      $set("status" -> status.id),
      multi = true
    ).void

  def minePage(userId: User.ID, page: Int, search: MemberCardLogSearch): Fu[Paginator[MemberCardLog]] = {
    var selector = $doc(
      "oldUserId" -> userId
    ) ++ search.username.??(u => $doc("newUserId" -> u.toLowerCase)) ++
      search.level.??(l => $doc("level" -> l)) ++
      search.status.??(s => $doc("status" -> s))

    if (search.dateMin.isDefined || search.dateMax.isDefined) {
      var dateRange = $doc()
      search.dateMin foreach { dateMin =>
        dateRange = dateRange ++ $gte(dateMin.withTimeAtStartOfDay())
      }
      search.dateMax foreach { dateMax =>
        dateRange = dateRange ++ $lte(dateMax.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999))
      }
      selector = selector ++ $doc("createAt" -> dateRange)
    }

    val adapter = new Adapter[MemberCardLog](
      collection = coll,
      selector = selector,
      projection = $empty,
      sort = $sort desc "createAt"
    )
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

}
