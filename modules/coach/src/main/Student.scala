package lila.coach

import Student._
import lila.user.User
import org.joda.time.DateTime

case class Student(
    id: String,
    coachId: User.ID,
    studentId: User.ID,
    available: Boolean,
    status: Status,
    createAt: DateTime,
    approvedAt: Option[DateTime]
) {

}

object Student {

  def make(coachId: User.ID, studentId: User.ID) = Student(
    id = makeId(coachId, studentId),
    coachId = coachId,
    studentId = studentId,
    available = true,
    status = Status.Applying,
    createAt = DateTime.now,
    approvedAt = none
  )

  def makeId(coachId: User.ID, studentId: User.ID) = s"$coachId@$studentId"

  sealed abstract class Status(val id: String, val name: String)
  object Status {
    case object Applying extends Status("applying", "审核中")
    case object Approved extends Status("approved", "已加入")
    case object Decline extends Status("decline", "已拒绝")

    def all = List(Applying, Approved, Decline)
    def apply(id: String): Status = all.find(_.id == id) getOrElse Applying
  }

}

case class StudentWithUser(student: Student, user: User)