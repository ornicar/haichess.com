package lila.clazz

import lila.clazz.Clazz.CoachID
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import scala.collection.mutable.ListBuffer

case class WeekClazz(
    dateStart: DateTime,
    dateEnd: DateTime,
    times: Int,
    weekCourse: List[WeekCourse]
) {

  private val datePattern = "yyyy-MM-dd"
  private val dateFormatter = DateTimeFormat forPattern datePattern
  private val dateTimeFormatter = DateTimeFormat forPattern s"$datePattern HH:mm"

  def dateBegin = toCourseFromWeek(null, null).head.date

  def timeBegin = weekCourse.head.timeBegin

  def dateTimeBegin = dateTimeFormatter.parseDateTime(dateBegin.toString(datePattern) + " " + timeBegin)

  def getDateEnd = toCourseFromWeek(null, null).last.date

  def nextWeekCourse(course: Course): Course = {
    toCourseFromWeek(id = course.clazz, coachID = course.coach, dateStart = course.date)
      .find(_.dateTime.isAfter(course.dateTime)).get
  }

  def toCourseFromWeek(id: Clazz.ID, coachID: CoachID, startIndex: Int = 1, dateStart: DateTime = dateStart): List[Course] = {
    val courseList = ListBuffer[Course]()
    var cal = dateStart.withTimeAtStartOfDay()
    while (courseList.length < times) {
      weekCourse.filter(_.week == cal.getDayOfWeek) foreach { c =>
        courseList.append(
          Course(
            _id = Course.genId,
            date = cal,
            timeBegin = c.timeBegin,
            timeEnd = c.timeEnd,
            week = c.week,
            clazz = id,
            coach = coachID,
            index = startIndex + courseList.size,
            stopped = false,
            enabled = true,
            homework = false,
            createdAt = DateTime.now(),
            updatedAt = DateTime.now(),
            createdBy = coachID,
            updatedBy = coachID
          )
        )
      }
      cal = cal.plusDays(1)
    }

    courseList.toList.sortWith { (thisCourse, thatCourse) =>
      thisCourse.dateTime.isBefore(thatCourse.dateTime)
    }
  }

  def valid = validTimeOrdered && validTimeConfusion

  // 所有时间段保持顺序
  def validTimeOrdered = {
    val tempDate = "1970-01-01"
    weekCourse map { c =>
      dateTimeFormatter.parseDateTime(tempDate + " " + c.timeEnd) isAfter dateTimeFormatter.parseDateTime(tempDate + " " + c.timeBegin)
    } forall (_ == true)
  }

  // 所有时间段不能交叉
  def validTimeConfusion = {
    val tempDate = "1970-01-01"
    if (weekCourse.length > 1) {
      val map: Map[Int, Boolean] = weekCourse.groupBy(_.week) mapValues { list =>
        if (list.length > 1) {
          list map { thisCourse =>
            list map { thatCourse =>
              if (!thisCourse.eq(thatCourse)) {
                val thisDateTimeBegin = dateTimeFormatter.parseDateTime(tempDate + " " + thisCourse.timeBegin).getMillis
                val thisDateTimeEnd = dateTimeFormatter.parseDateTime(tempDate + " " + thisCourse.timeEnd).getMillis
                val thatDateTimeBegin = dateTimeFormatter.parseDateTime(tempDate + " " + thatCourse.timeBegin).getMillis
                val thatDateTimeEnd = dateTimeFormatter.parseDateTime(tempDate + " " + thatCourse.timeEnd).getMillis
                if (thisDateTimeEnd < thatDateTimeBegin || thatDateTimeEnd < thisDateTimeBegin) {
                  true
                } else {
                  false
                }
              } else {
                true
              }
            } forall (_ == true)
          } forall (_ == true)
        } else {
          true
        }
      }
      map.values.forall(_ == true)
    } else {
      true
    }
  }

  def validFirstDay = weekCourse.headOption.??(_.week == dateStart.getDayOfWeek)

}

case class WeekCourse(
    week: Int,
    timeBegin: String,
    timeEnd: String
) {

  override def toString: String = s"$weekFormat $timeBegin ~ $timeEnd"

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
}
