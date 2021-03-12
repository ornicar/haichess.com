package lila.clazz

import lila.db.dsl._
import lila.notify.NotifyApi
import org.joda.time.DateTime
import lila.user.User

final class HomeworkApi(coll: Coll, stuApi: HomeworkStudentApi, courseApi: CourseApi, notifyApi: NotifyApi) {

  import BSONHandlers.homeworkHandler

  def byId(id: String): Fu[Option[Homework]] = coll.byId[Homework](id)

  def find(clazzId: Clazz.ID, courseId: Course.ID, index: Int): Fu[Option[Homework]] =
    byId(Homework.makeId(clazzId, courseId, index))

  def findOrCreate(clazzId: Clazz.ID, courseId: Course.ID, index: Int, user: User.ID): Fu[Homework] =
    find(clazzId, courseId, index) flatMap {
      case None => create(clazzId, courseId, index, user)
      case Some(h) => fuccess(h)
    }

  def findByClazz(clazzId: Clazz.ID): Fu[List[Homework]] =
    coll.find($doc("clazzId" -> clazzId)).list[Homework]()

  def courseHomeworks(clazzId: Clazz.ID, user: User.ID): Fu[List[CourseWithHomework]] = for {
    courses <- courseApi.clazzCourse(clazzId)
    homeworks <- findByClazz(clazzId)
    stuHomeworks <- stuApi.mineOfClazz(clazzId, user)
  } yield {
    courses.map { course =>
      CourseWithHomework(course, homeworks.find(_.courseId == course.id), stuHomeworks.find(_.courseId == course.id))
    }
  }

  def create(clazzId: Clazz.ID, courseId: Course.ID, index: Int, user: User.ID): Fu[Homework] = {
    val homework = Homework.empty(clazzId, courseId, index, user)
    coll.insert(homework).inject(homework)
  }

  def updateAndPublish(homework: Homework, clazz: Clazz, course: Course, data: HomeworkData, user: User.ID): Funit = {
    update(homework, course, data, user) flatMap { publish(_, clazz) }
  }

  def update(homework: Homework, course: Course, data: HomeworkData, user: User.ID): Fu[Homework] = {
    val h = data.toHomework(course, user)
    val newHomework = homework.copy(
      deadlineAt = h.deadlineAt,
      summary = h.summary,
      prepare = h.prepare,
      common = h.common,
      practice = h.practice,
      students = h.students,
      updatedAt = DateTime.now
    )
    update(newHomework) inject newHomework
  }

  def update(homework: Homework): Funit = coll.update($id(homework.id), homework).void

  def publish(homework: Homework, clazz: Clazz): Funit =
    coll.update(
      $id(homework.id),
      $set(
        "status" -> Homework.Status.Published.id
      )
    ).void >> stuApi.create(homework, clazz) >> courseApi.setHomework(homework.courseId)

  def deadlines: Fu[List[Homework]] =
    coll.find($doc("deadlineAt" -> ($lte(DateTime.now) ++ $gte(DateTime.now minusMinutes 1)))).list[Homework]()
}
