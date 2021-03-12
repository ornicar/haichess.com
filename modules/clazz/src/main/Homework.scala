package lila.clazz

import lila.user.User
import org.joda.time.DateTime
import Homework._

case class Homework(
    _id: ID,
    clazzId: String,
    courseId: String,
    index: Int,
    deadlineAt: Option[DateTime],
    summary: Option[String],
    prepare: Option[String],
    common: Option[HomeworkCommon],
    practice: Option[HomeworkPractice],
    students: StudentSettings,
    status: Status,
    createdAt: DateTime,
    updatedAt: DateTime,
    createdBy: User.ID,
    updatedBy: User.ID
) {

  def id = _id

  def isCreated = status == Status.Created
  def isPublished = status == Status.Published

  def available = deadlineAt.??(_.isAfter(DateTime.now))

  def deadline = deadlineAt.map(_.toString("yyyy-MM-dd HH:mm"))

  def practiceWithEmpty = practice | HomeworkPractice.empty

  def hasContent = common.??(_.hasContent) || practice.??(_.hasContent)

  def itemIds = common.??(_.items.keySet.mkString(","))

  def isEmpty = !hasContent

  def isCreator(userId: User.ID) = userId == createdBy

  def belongTo(userId: User.ID) = students.contains(userId) || isCreator(userId)

}

object Homework {

  type ID = String

  def empty(
    clazzId: String,
    courseId: String,
    index: Int,
    user: User.ID
  ) = make(
    clazzId = clazzId,
    courseId = courseId,
    index = index,
    deadlineAt = None,
    summary = None,
    prepare = None,
    common = None,
    practice = None,
    students = StudentSettings(settings = Map.empty[String, StudentSetting]),
    user = user
  )

  def make(
    clazzId: String,
    courseId: String,
    index: Int,
    deadlineAt: Option[DateTime],
    summary: Option[String],
    prepare: Option[String],
    common: Option[HomeworkCommon],
    practice: Option[HomeworkPractice],
    students: StudentSettings,
    user: User.ID
  ) = Homework(
    _id = makeId(clazzId, courseId, index),
    clazzId = clazzId,
    courseId = courseId,
    index = index,
    deadlineAt = deadlineAt,
    summary = summary,
    prepare = prepare,
    common = common,
    practice = practice,
    students = students,
    status = Status.Created,
    createdAt = DateTime.now,
    updatedAt = DateTime.now,
    createdBy = user,
    updatedBy = user
  )

  def makeId(clazzId: String, courseId: String, index: Int) = s"$clazzId@$courseId@$index"

  sealed abstract class Status(val id: Int, val name: String) extends Ordered[Status] {
    def compare(other: Status) = Integer.compare(id, other.id)
    def is(s: Status): Boolean = this == s
    def is(f: Status.type => Status): Boolean = is(f(Status))
  }
  object Status {
    case object Created extends Status(10, "筹备中")
    case object Published extends Status(20, "已发布")

    val all = List(Created, Published)
    val byId = all map { v => (v.id, v) } toMap
    def apply(id: Int): Status = byId get id err s"Bad Status $id"
  }

}

case class StudentSettings(settings: Map[User.ID, StudentSetting]) {

  def contains(userId: User.ID): Boolean = settings contains userId

  def get(userId: User.ID): Option[StudentSetting] = settings.get(userId)

  def comment(userId: User.ID): Option[String] = get(userId) match {
    case None => none
    case Some(setting) => setting.comment
  }

  def common(userId: User.ID): Option[HomeworkCommon] = get(userId) match {
    case None => none
    case Some(setting) => setting.common
  }

}
case class StudentSetting(comment: Option[String], common: Option[HomeworkCommon])
case class HomeworkWithCourse(homework: Homework, course: Course)
case class CourseWithHomework(course: Course, homework: Option[Homework], stuHomework: Option[HomeworkStudent])

