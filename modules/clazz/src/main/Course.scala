package lila.clazz

import lila.user.User
import org.joda.time.DateTime
import ornicar.scalalib.Random
import org.joda.time.format.DateTimeFormat

case class Course(
    _id: Course.ID,
    date: DateTime,
    timeBegin: String,
    timeEnd: String,
    week: Int,
    clazz: String,
    coach: String,
    index: Int,
    stopped: Boolean,
    enabled: Boolean,
    homework: Boolean,
    createdAt: DateTime,
    updatedAt: DateTime,
    createdBy: User.ID,
    updatedBy: User.ID
) {

  val dateTimeFormatter = DateTimeFormat forPattern "yyyy-MM-dd HH:mm"

  val dateTime = dateTimeFormatter.parseDateTime(date.toString("yyyy-MM-dd") + " " + timeBegin)

  val dateEndTime = dateTimeFormatter.parseDateTime(date.toString("yyyy-MM-dd") + " " + timeEnd)

  def id = _id

  def editable = dateTime.isAfterNow

  def isCreator(user: String) = user == createdBy

  def weekFormat =
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

  def courseFormatTime = s"${date.toString("yyyy年MM月dd")}（$weekFormat）$timeBegin"

  override def toString: String = s"clazz: $clazz, course: $id"
}

object Course {

  type ID = String

  def genId = Random nextString 8

  case class WithClazz(course: Course, clazz: Clazz)
}
