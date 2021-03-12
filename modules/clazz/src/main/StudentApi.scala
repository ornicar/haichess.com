package lila.clazz

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime
import lila.notify.{ InvitedToClazz, Notification, NotifyApi }
import lila.team.TeamApi

final class StudentApi(coll: Coll, courseApi: CourseApi, teamApi: TeamApi, notifyApi: NotifyApi, bus: lila.common.Bus) {

  import BSONHandlers.StudentBSONHandler
  import BSONHandlers.StudentsBSONHandler

  def byId(clazzId: Clazz.ID, userId: User.ID): Fu[Option[Student]] =
    students(clazzId) map { studentsOption =>
      studentsOption.fold(none[Student]) { ss =>
        ss.students.get(userId)
      }
    }

  def students(clazzId: Clazz.ID): Fu[Option[Students]] =
    coll.primitiveOne[Students]($id(clazzId), s"students")

  def addStudent(clazz: Clazz, userId: User.ID, student: Student): Funit =
    coll.update(
      $id(clazz._id),
      $set(s"students.$userId" -> student)
    ) >> sendInviteNotify(clazz, userId)

  def invitedAgain(clazz: Clazz, userId: User.ID): Funit =
    coll.update(
      $id(clazz._id),
      $set(
        s"students.$userId.status" -> Student.InviteStatus.Invited.id,
        s"students.$userId.createdAt" -> DateTime.now
      )
    ) >> sendInviteNotify(clazz, userId)

  def removeStudent(clazz: Clazz, userId: User.ID): Funit =
    coll.update(
      $id(clazz.id),
      $unset(s"students.$userId")
    ) >> clazz.team.?? { teamId =>
        teamApi.removeMemberClazz(userId, teamId, clazz.id)
      } >> removePublish(clazz.id, userId)

  def accept(clazz: Clazz, user: User): Funit =
    coll.update(
      $id(clazz.id),
      $set(
        s"students.${user.id}.status" -> Student.InviteStatus.Joined.id,
        s"students.${user.id}.acceptAt" -> DateTime.now()
      )
    ) >> clazz.team.?? { teamId =>
        teamApi.addMemberClazz(user.id, teamId, clazz.id)
      } >> acceptPublish(clazz, user)

  def refused(clazzId: String, user: User): Funit =
    coll.update(
      $id(clazzId),
      $set(
        s"students.${user.id}.status" -> Student.InviteStatus.Refused.id,
        s"students.${user.id}.refusedAt" -> DateTime.now()
      )
    ).void

  private def sendInviteNotify(clazz: Clazz, userId: User.ID): Funit = {
    val notificationContent = InvitedToClazz(
      InvitedToClazz.InvitedBy(clazz.coach),
      InvitedToClazz.ClazzName(clazz.name),
      InvitedToClazz.ClazzId(clazz._id)
    )
    notifyApi.addNotification(
      Notification.make(Notification.Notifies(userId), notificationContent)
    )
  }

  private def acceptPublish(clazz: Clazz, user: User): Funit = {
    bus.publish(lila.hub.actorApi.clazz.ClazzJoinAccept(clazz.id, clazz.name, clazz.coach, user.id), 'clazzJoinAccept)
    courseApi.clazzCourse(clazz.id).map { list =>
      val calendars = list.map { course =>
        makeCalendar(clazz, course, user)
      }
      bus.publish(lila.hub.actorApi.calendar.CalendarsCreate(calendars), 'calendarCreateBus)
    }
  }

  private def removePublish(clazzId: Clazz.ID, userId: User.ID): Funit =
    courseApi.clazzCourse(clazzId, true).map { list =>
      val ids = list.map { course => s"$clazzId@$userId@${course.index}" }
      bus.publish(lila.hub.actorApi.calendar.CalendarsRemove(ids), 'calendarRemoveBus)
    }

  private def makeCalendar(clazz: Clazz, course: Course, user: User) =
    lila.hub.actorApi.calendar.CalendarCreate(
      id = s"${clazz.id}@${user.id}@${course.index}".some,
      typ = "course",
      user = user.id,
      sdt = course.dateTime,
      edt = course.dateEndTime,
      content = s"${clazz.name} 第${course.index}节",
      onlySdt = false,
      link = s"/homework/show2?clazzId=${clazz.id}&courseId=${course.id}".some,
      icon = "课".some,
      bg = "#507803".some
    )

}
