package lila.calendar

import org.joda.time.DateTime
import play.api.libs.json._

final class JsonView {

  def week(calendars: List[Calendar], days: List[String]): JsObject = Json.obj(
    "days" -> days,
    "weeks" -> List("周一", "周二", "周三", "周四", "周五", "周六", "周日"),
    "list" -> calendarsJson(calendars)
  )

  def day(calendars: List[Calendar], day: DateTime): JsObject = Json.obj(
    "days" -> List(day.toString("M月d日")),
    "weeks" -> List(weekFormat(day.getDayOfWeek)),
    "list" -> calendarsJson(calendars)
  )

  private def weekFormat(week: Int): String =
    "周".concat(
      week match {
        case 1 => "一"
        case 2 => "二"
        case 3 => "三"
        case 4 => "四"
        case 5 => "五"
        case 6 => "六"
        case 7 => "日"
      }
    )

  def calendarsJson(calendars: List[Calendar]): JsArray =
    JsArray(
      calendars.map(calendarJson _)
    )

  def calendarJson(calendar: Calendar): JsObject = Json.obj(
    "id" -> calendar.id,
    "typ" -> calendar.typ,
    "week" -> calendar.week,
    "date" -> calendar.date,
    "st" -> calendar.st,
    "et" -> calendar.et,
    "period" -> calendar.period.name,
    "content" -> calendar.content,
    "tag" -> calendar.tp.name,
    "onlySdt" -> calendar.onlySdt,
    "finished" -> calendar.sdt.isBeforeNow
  ).add("link" -> calendar.link)
    .add("icon" -> calendar.icon)
    .add("bg" -> calendar.bg)

}

