package controllers

import lila.app.Env
import lila.app._
import org.joda.time.DateTime
import views._

object Calendar extends LilaController {

  def api = Env.calendar.api
  def jsonView = Env.calendar.jsonView

  def remove(id: String) = Auth { implicit ctx => me =>
    api.remove(id) map { _ =>
      Ok("Ok")
    } map (_ as JSON)
  }

  def week(offset: Int) = Auth { implicit ctx => me =>
    api.week(offset, me.id) map { calendars =>
      Ok(jsonView.week(calendars, days(offset)))
    } map (_ as JSON)
  }

  def day(offset: Int) = Auth { implicit ctx => me =>
    api.day(offset, me.id) map { calendars =>
      Ok(jsonView.day(calendars, DateTime.now.plusDays(offset)))
    } map (_ as JSON)
  }

  private def days(week: Int): List[String] = {
    val minDay = DateTime.now.withDayOfWeek(1).plusWeeks(week)
    (0 to 6).map { i =>
      minDay.plusDays(i).toString("M月d日")
    }.toList
  }

}
