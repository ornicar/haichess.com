package lila.clazz

import lila.db.dsl._
import lila.user.User
import lila.notify.NotifyApi

final class HomeworkStudentApi(coll: Coll, clazzApi: ClazzApi, courseApi: CourseApi, notifyApi: NotifyApi, bus: lila.common.Bus) {

  import BSONHandlers.HomeworkStudentHandler

  def byId(id: String): Fu[Option[HomeworkStudent]] =
    coll.byId[HomeworkStudent](id)

  def byId2(clazzId: Clazz.ID, courseId: Course.ID, userId: User.ID): Fu[Option[HomeworkStudent]] =
    coll.uno[HomeworkStudent](
      $doc(
        "clazzId" -> clazzId,
        "courseId" -> courseId,
        "studentId" -> userId
      )
    )

  def findByCourse(clazzId: Clazz.ID, courseId: Course.ID): Fu[List[HomeworkStudent]] =
    coll.find(
      $doc(
        "clazzId" -> clazzId,
        "courseId" -> courseId
      )
    ).list[HomeworkStudent]()

  def info(id: String): Fu[Option[HomeworkStudentFullInfo]] =
    for {
      homeworkOption <- byId(id)
      clazzOption <- homeworkOption.??(h => clazzApi.byId(h.clazzId))
      courseOption <- homeworkOption.??(h => courseApi.byId(h.courseId))
    } yield (homeworkOption |@| clazzOption |@| courseOption).apply {
      case (homework, clazz, course) => HomeworkStudentFullInfo(homework, clazz, course)
    }

  def mineOfClazz(clazzId: Clazz.ID, userId: User.ID): Fu[List[HomeworkStudent]] =
    coll.find(
      $doc(
        "clazzId" -> clazzId,
        "studentId" -> userId
      )
    ).list[HomeworkStudent]()

  def create(homework: Homework, clazz: Clazz): Funit = {
    val homeworks = clazz.studentsId.map { studentId =>
      HomeworkStudent.byHomework(homework, studentId)
    }
    coll.bulkInsert(
      documents = homeworks.map(HomeworkStudentHandler.write).toStream,
      ordered = false
    ).void >>- bus.publish(StudentHomeworkCreate(homeworks), 'homeworkCreate)
  }

}
