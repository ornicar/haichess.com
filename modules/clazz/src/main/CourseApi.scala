package lila.clazz

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference

final class CourseApi(coll: Coll, clazzColl: Coll, bus: lila.common.Bus) {

  import BSONHandlers.clazzHandler
  import BSONHandlers.courseHandler

  def bulkInsert(courseList: List[Course]): Funit = coll.bulkInsert(
    documents = courseList.map(courseHandler.write).toStream,
    ordered = false
  ).void

  def bulkUpdate(id: String, courseList: List[Course]): Funit =
    coll.remove($doc("clazz" -> id)) >> bulkInsert(courseList)

  def byId(id: String): Fu[Option[Course]] = coll.byId[Course](id)

  def findByCourseIndex(clazzId: String, index: Int): Fu[Option[Course]] =
    coll.find(
      $doc(
        "clazz" -> clazzId,
        "index" -> index,
        "stopped" -> false,
        "enabled" -> true
      )
    ).uno[Course]

  def clazzCourse(clazzId: Clazz.ID, withStopped: Boolean = false): Fu[List[Course]] =
    coll.find(
      $doc(
        "clazz" -> clazzId,
        "stopped" -> withStopped,
        "enabled" -> true
      )
    ).list[Course]()

  def weekCourse(firstDay: DateTime, lastDay: DateTime, user: User): Fu[List[Course]] = {
    coll.find($doc(
      "coach" -> user.id,
      "enabled" -> true,
      "date" -> ($gte(firstDay.withTimeAtStartOfDay()) ++ $lte(lastDay.withTime(23, 59, 59, 999)))
    )).sort($sort asc "date").list[Course]()
  }

  def courseFromSecondary(ids: Seq[String]): Fu[List[Course]] =
    coll.byOrderedIds[Course, String](
      ids,
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def update(c: Course, data: UpdateData): Funit =
    coll.update(
      $id(c._id),
      $set(
        "date" -> data.date,
        "timeBegin" -> data.timeBegin,
        "timeEnd" -> data.timeEnd,
        "week" -> data.date.getDayOfWeek,
        "updatedAt" -> DateTime.now()
      )
    ).void >> updatePublish(c.copy(
        date = data.date,
        timeBegin = data.timeBegin,
        timeEnd = data.timeEnd,
        week = data.date.getDayOfWeek
      ))

  def stop(course: Course): Funit =
    coll.update($id(course.id), $set("stopped" -> true)).void >> stopPublish(course)

  def stopByClazz(clazzId: String): Funit =
    coll.update(
      $doc("clazz" -> clazzId),
      $set("stopped" -> true),
      multi = true
    ).void >> stopByClazzPublish(clazzId)

  def deleteByClazz(clazzId: String): Funit =
    coll.remove(
      $doc("clazz" -> clazzId)
    ).void

  // 只有周定时的能推迟
  def postpone(course: Course, clazz: Clazz): Fu[DateTime] = {
    beforeList(course) flatMap { courseList =>
      val sortedCourseList = courseList.sortWith { (thisCourse, thatCourse) =>
        thisCourse.dateTime.isBefore(thatCourse.dateTime)
      }

      sortedCourseList.map { course =>
        val virtualCourse = courseList.find(_.dateTime.isAfter(course.dateTime))
          .getOrElse(clazz.weekClazz.map(_.nextWeekCourse(course)) err s"cannot find next week course of ${course.toString}")
        coll.update(
          $id(course.id),
          $set(
            "date" -> virtualCourse.date,
            "timeBegin" -> virtualCourse.timeBegin,
            "timeEnd" -> virtualCourse.timeEnd,
            "updatedAt" -> DateTime.now()
          )
        ) inject virtualCourse.copy(index = course.index)
      }.sequenceFu.flatMap { virtualCourseList =>
        fuccess(postponePublish(clazz, sortedCourseList, virtualCourseList)) inject sortedCourseList.last.date
      }
    }
  }

  private def beforeList(course: Course): Fu[List[Course]] =
    coll.find($doc(
      "clazz" -> course.clazz,
      "index" -> $gte(course.index),
      "enabled" -> true
    )).sort($sort asc "index").list[Course]()

  private def stopByClazzPublish(clazzId: Clazz.ID): Funit =
    clazzColl.byId[Clazz](clazzId) flatMap {
      case None => funit
      case Some(c) => {
        clazzCourse(clazzId, true).map { list =>
          val ids = list.foldLeft(List.empty[String]) { (all, course) =>
            {
              all ++ c.studentsId.map { userId => s"$clazzId@$userId@${course.index}" }
            }
          }
          bus.publish(lila.hub.actorApi.calendar.CalendarsRemove(ids), 'calendarRemoveBus)
        }
      }
    }

  private def updatePublish(course: Course): Funit =
    clazzColl.byId[Clazz](course.clazz) map {
      case None =>
      case Some(c) => {
        val ids = c.studentsId.map { userId => s"${course.clazz}@$userId@${course.index}" }
        bus.publish(lila.hub.actorApi.calendar.CalendarsRemove(ids), 'calendarRemoveBus)

        // -------------------------------------------------------------------------------
        val calendars = c.studentsId.map { userId => makeCalendar(c, course, userId) }
        bus.publish(lila.hub.actorApi.calendar.CalendarsCreate(calendars), 'calendarCreateBus)
      }
    }

  private def stopPublish(course: Course): Funit =
    clazzColl.byId[Clazz](course.clazz) map {
      case None =>
      case Some(c) => {
        val ids = c.studentsId.map { userId => s"${course.clazz}@$userId@${course.index}" }
        bus.publish(lila.hub.actorApi.calendar.CalendarsRemove(ids), 'calendarRemoveBus)
      }
    }

  private def postponePublish(clazz: Clazz, oldCourseList: List[Course], newCourseList: List[Course]) = {
    val ids = oldCourseList.foldLeft(List.empty[String]) { (all, course) =>
      {
        all ++ clazz.studentsId.map { userId => s"${clazz.id}@$userId@${course.index}" }
      }
    }
    bus.publish(lila.hub.actorApi.calendar.CalendarsRemove(ids), 'calendarRemoveBus)

    // -------------------------------------------------------------------------------
    val calendars = newCourseList.foldLeft(List.empty[lila.hub.actorApi.calendar.CalendarCreate]) { (all, course) =>
      {
        all ++ clazz.studentsId.map { userId =>
          makeCalendar(clazz, course, userId)
        }
      }
    }
    bus.publish(lila.hub.actorApi.calendar.CalendarsCreate(calendars), 'calendarCreateBus)
  }

  private def makeCalendar(clazz: Clazz, course: Course, userId: User.ID) =
    lila.hub.actorApi.calendar.CalendarCreate(
      id = s"${clazz.id}@${userId}@${course.index}".some,
      typ = "course",
      user = userId,
      sdt = course.dateTime,
      edt = course.dateEndTime,
      content = s"${clazz.name} 第${course.index}节",
      onlySdt = false,
      link = s"/homework/show2?clazzId=${clazz.id}&courseId=${course.id}".some,
      icon = "课".some,
      bg = "#507803".some
    )

  def setHomework(id: String): Funit =
    coll.update(
      $id(id),
      $set("homework" -> true)
    ).void
}
