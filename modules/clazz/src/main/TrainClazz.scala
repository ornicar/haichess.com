package lila.clazz

import lila.clazz.Clazz.{ CoachID, ID }
import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, Period, PeriodType }

import scala.collection.mutable.ListBuffer

case class TrainClazz(
    dateStart: DateTime,
    dateEnd: DateTime,
    times: Int,
    trainCourse: List[TrainCourse]
) {

  private val datePattern = "yyyy-MM-dd"
  private val dateFormatter = DateTimeFormat forPattern datePattern
  private val dateTimeFormatter = DateTimeFormat forPattern s"$datePattern HH:mm"

  def timeBegin = trainCourse.sortBy(_.dateStart).head.timeBegin

  def dateTimeBegin = dateTimeFormatter.parseDateTime(trainCourse.sortBy(_.dateStart).head.dateStart.toString(datePattern) + " " + timeBegin)

  def valid = validTimeOrdered && validTimeConfusion

  def validTimeOrdered =
    trainCourse.map { tc =>
      !tc.dateEnd.isBefore(tc.dateStart) && tc.timeEnd.compareTo(tc.timeBegin) > 0
    } forall (_ == true)

  def validTimeConfusion = {
    val list = toCourseFromTrain("", "")
    list map { thisCourse =>
      list map { thatCourse =>
        if (!thisCourse.eq(thatCourse)) {
          val thisDateTimeBegin = thisCourse.dateTime.getMillis
          val thisDateTimeEnd = thisCourse.dateEndTime.getMillis
          val thatDateTimeBegin = thatCourse.dateTime.getMillis
          val thatDateTimeEnd = thatCourse.dateEndTime.getMillis
          if (thisDateTimeBegin < thatDateTimeEnd && thatDateTimeBegin < thisDateTimeEnd) false
          else true
        } else true
      } forall (_ == true)
    } forall (_ == true)
  }

  def toCourseFromTrain(id: Clazz.ID, coachID: CoachID): List[Course] = {
    val courseList = ListBuffer[Course]()
    var index = 1
    trainCourse.foreach { c =>
      val sdate = c.dateStart.withTimeAtStartOfDay
      val edate = c.dateEnd.withTimeAtStartOfDay
      val days = new Period(sdate.getMillis, edate.getMillis, PeriodType.days).getDays
      (0 to days) foreach { day =>
        val d = sdate.plusDays(day)
        courseList.append(
          Course(
            _id = Course.genId,
            date = d,
            timeBegin = c.timeBegin,
            timeEnd = c.timeEnd,
            week = d.getDayOfWeek,
            clazz = id,
            coach = coachID,
            index = index,
            stopped = false,
            enabled = true,
            homework = false,
            createdAt = DateTime.now(),
            updatedAt = DateTime.now(),
            createdBy = coachID,
            updatedBy = coachID
          )
        )
        index = index + 1
      }
    }

    courseList.toList.sortWith { (thisCourse, thatCourse) =>
      thisCourse.dateTime.isBefore(thatCourse.dateTime)
    }
  }

}

case class TrainCourse(
    dateStart: DateTime,
    dateEnd: DateTime,
    timeBegin: String,
    timeEnd: String
) {

  override def toString: String = s"${dateStart.toString("yyyy/MM/dd")} ~ ${dateEnd.toString("yyyy/MM/dd")} $timeBegin~$timeEnd"

}

