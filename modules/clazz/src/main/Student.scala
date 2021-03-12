package lila.clazz

import lila.user.User
import org.joda.time.DateTime
import lila.clazz.Student.InviteStatus

case class Student(
    status: String,
    enabled: Boolean,
    createdAt: DateTime,
    acceptAt: Option[DateTime],
    refusedAt: Option[DateTime]
) {

  val expireDay: Int = 3

  def expired = createdAt.plusDays(expireDay).isBeforeNow

  def statusPretty = status match {
    case InviteStatus.Invited.id =>
      if (expired) InviteStatus.Expired
      else InviteStatus.Invited
    case InviteStatus.Joined.id => InviteStatus.Joined
    case InviteStatus.Refused.id => InviteStatus.Refused
  }

  def joined = status == InviteStatus.Joined.id

  def statusObject = InviteStatus(status)
}

object Student {

  def make = Student(
    status = InviteStatus.Invited.id,
    enabled = true,
    createdAt = DateTime.now(),
    acceptAt = None,
    refusedAt = None
  )

  type StudentMap = Map[User.ID, Student]

  sealed abstract class InviteStatus(val id: String, val name: String, val sort: Int)
  object InviteStatus {
    case object Invited extends InviteStatus("invited", "待同意", 1)
    case object Joined extends InviteStatus("joined", "已加入", 2)
    case object Expired extends InviteStatus("expired", "已过期", 3)
    case object Refused extends InviteStatus("refused", "已拒绝", 4)

    val all = List(Invited, Joined, Expired, Refused)

    def byId = all map { v => (v.id, v) } toMap

    def apply(id: String): InviteStatus = byId get id err s"can not apply InviteStatus $id"
  }

}

case class Students(students: Student.StudentMap) {

  def toList = students.toList

  def contains(userId: User.ID): Boolean = students contains userId
  def contains(user: User): Boolean = contains(user.id)

  def get = students.get _

  def ids = students.keys

}

object Students {

  val empty = Students(Map.empty)

}