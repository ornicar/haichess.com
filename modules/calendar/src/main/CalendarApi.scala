package lila.calendar

import lila.db.dsl._
import lila.hub.actorApi.calendar.CalendarCreate
import org.joda.time.DateTime
import reactivemongo.bson.Macros
import lila.user.User

final class CalendarApi(coll: Coll, bus: lila.common.Bus) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  implicit val calendarHandler = Macros.handler[Calendar]

  def byId(id: Calendar.ID): Fu[Option[Calendar]] = coll.byId[Calendar](id)

  def week(offset: Int, user: User.ID): Fu[List[Calendar]] = {
    val now = DateTime.now
    val minDay = now.withDayOfWeek(1).plusWeeks(offset)
    val maxDay = now.withDayOfWeek(7).plusWeeks(offset)

    coll.find(
      $doc(
        "user" -> user,
        "sdt" -> ($gte(minDay.withTimeAtStartOfDay()) ++ $lte(maxDay.withTime(23, 59, 59, 999)))
      )
    ).sort($sort asc "sdt").list()
  }

  def day(offset: Int, user: User.ID): Fu[List[Calendar]] = {
    val date = DateTime.now.plusDays(offset)
    coll.find(
      $doc(
        "user" -> user,
        "sdt" -> ($gte(date.withTimeAtStartOfDay()) ++ $lte(date.withTime(23, 59, 59, 999)))
      )
    ).sort($sort asc "sdt").list()
  }

  def create(
    id: Option[Calendar.ID],
    typ: String,
    user: User.ID,
    sdt: DateTime,
    edt: DateTime,
    content: String,
    onlySdt: Boolean,
    link: Option[String],
    icon: Option[String],
    bg: Option[String]
  ): Funit = coll.insert(
    Calendar.make(id, typ, user, sdt, edt, content, onlySdt, link, icon, bg)
  ).void >>- bus.publish(lila.hub.actorApi.home.ReloadCalendar(List(user)), 'changeCalendar)

  def remove(id: Calendar.ID): Funit =
    coll.byId[Calendar](id) flatMap {
      case None => funit
      case Some(c) => coll.remove($id(id)).void >>- bus.publish(lila.hub.actorApi.home.ReloadCalendar(List(c.user)), 'changeCalendar)
    }

  def batchRemove(ids: List[String]): Funit =
    coll.find($inIds(ids)).list().flatMap { list =>
      coll.remove($inIds(ids) ++ $doc("sdt" $gte DateTime.now)).void >>- bus.publish(lila.hub.actorApi.home.ReloadCalendar(list.map(_.user)), 'changeCalendar)
    }

  def batchCreate(list: List[CalendarCreate]): Funit =
    coll.bulkInsert(
      documents = list.map(cc => Calendar.make(cc.id, cc.typ, cc.user, cc.sdt, cc.edt, cc.content, cc.onlySdt, cc.link, cc.icon, cc.bg))
        .map(calendarHandler.write).toStream,
      ordered = false
    ).void >>- bus.publish(lila.hub.actorApi.home.ReloadCalendar(list.map(_.user)), 'changeCalendar)

}
