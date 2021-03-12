package lila.calendar

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    hub: lila.hub.Env,
    db: lila.db.Env
) {

  val CollectionCalendar = config getString "collection.calendar"

  private lazy val calendarColl = db(CollectionCalendar)

  lazy val api = new CalendarApi(calendarColl, hub.bus)

  lazy val jsonView = new JsonView()

  system.lilaBus.subscribeFuns(
    'calendarCreateBus -> {
      case lila.hub.actorApi.calendar.CalendarCreate(id, typ, user, sdt, edt, content, onlySdt, link, icon, bg) => api.create(id, typ, user, sdt, edt, content, onlySdt, link, icon, bg)
      case lila.hub.actorApi.calendar.CalendarsCreate(list) => api.batchCreate(list)
    },
    'calendarRemoveBus -> {
      case lila.hub.actorApi.calendar.CalendarRemove(id) => api.remove(id)
      case lila.hub.actorApi.calendar.CalendarsRemove(ids) => api.batchRemove(ids)
    }
  )
}

object Env {

  lazy val current: Env = "calendar" boot new Env(
    config = lila.common.PlayApp loadConfig "calendar",
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current
  )
}
