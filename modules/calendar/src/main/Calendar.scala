package lila.calendar

import org.joda.time.DateTime
import ornicar.scalalib.Random
import lila.user.User

case class Calendar(
    _id: Calendar.ID,
    typ: String,
    user: User.ID,
    sdt: DateTime,
    edt: DateTime,
    content: String,
    onlySdt: Boolean,
    link: Option[String],
    icon: Option[String],
    bg: Option[String],
    createAt: DateTime
) {

  import Calendar._

  def id: Calendar.ID = _id
  def week: Int = sdt.getDayOfWeek
  def date: String = sdt.toString("M月d日")
  def st: String = sdt.toString("HH:mm")
  def et: String = edt.toString("HH:mm")
  def period: Period = Period.byRange(st)
  def tp: Typ = Typ(typ)

}

object Calendar {

  type ID = String

  sealed class Period(val id: String, val name: String, val min: String, val max: String, val sort: Int)
  object Period {
    case object Morning extends Period("morning", "上午", "00:00", "12:00", 1)
    case object Afternoon extends Period("afternoon", "下午", "12:00", "18:00", 2)
    case object Evening extends Period("evening", "晚上", "18:00", "24:00", 3)

    val all = List(Morning, Afternoon, Evening)
    val byId = all map { v => (v.id, v) } toMap
    def apply(id: String): Period = byId get id err s"Bad Period $id"
    def byRange(time: String): Period = all.find { period =>
      time >= period.min && time < period.max
    } err s"Bad Period $time"
  }

  sealed class Typ(val id: String, val name: String)
  object Typ {
    case object Appt extends Typ("appt", "预约")
    case object Course extends Typ("course", "课程")
    case object Contest extends Typ("contest", "比赛")

    val all = List(Appt, Course, Contest)
    val byId = all map { v => (v.id, v) } toMap
    def apply(id: String): Typ = byId.get(id) err s"Bad Typ $id"
  }

  def make(
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
  ) = Calendar(
    _id = id | Random.nextString(8),
    typ = typ,
    user = user,
    sdt = sdt,
    edt = edt,
    content = content,
    onlySdt = onlySdt,
    link = link,
    icon = icon,
    bg = bg,
    createAt = DateTime.now
  )

}
