package lila.coach

import lila.coach.Student.Status
import lila.db.dsl._
import lila.notify.{ Notification, NotifyApi }
import lila.notify.Notification.Notifies
import lila.user.{ User, UserRepo }
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference

final class StudentApi(coll: Coll, notifyApi: NotifyApi) {

  import BsonHandlers.StudentBSONHandler

  def byId(id: String): Fu[Option[Student]] = coll.byId[Student](id)

  def byIds(coachId: User.ID, studentId: User.ID): Fu[Option[Student]] =
    coll.byId[Student](Student.makeId(coachId, studentId))

  def mineCoach(user: Option[User]): Fu[Set[String]] = user.?? { u =>
    coll.distinct[String, Set](
      "coachId",
      $doc(
        "studentId" -> u.id,
        "available" -> true,
        "status" -> Student.Status.Approved.id
      ).some
    )
  }

  def mineCertifyCoach(user: Option[User]): Fu[List[User]] = user.?? { u =>
    for {
      coachs <- coll.distinct[String, Set](
        "coachId",
        $doc(
          "studentId" -> u.id,
          "available" -> true,
          "status" -> Student.Status.Approved.id
        ).some
      )
      users <- UserRepo.byOrderedIds(coachs.toSeq, ReadPreference.secondaryPreferred)
    } yield users.filter(_.isCoach)
  }

  def mineStudents(user: Option[User]): Fu[Set[String]] = user.?? { u =>
    coll.distinct[String, Set](
      "studentId",
      $doc(
        "coachId" -> u.id,
        "available" -> true,
        "status" -> Student.Status.Approved.id
      ).some
    )
  }

  def applyingList(coachId: String): Fu[List[Student]] =
    coll.find(
      $doc(
        "coachId" -> coachId,
        "available" -> true,
        "status" -> Student.Status.Applying.id
      )
    ).sort($doc("createAt" -> -1)).list()

  def approvedList(coachId: String, q: String): Fu[List[StudentWithUser]] =
    coll.find(
      $doc(
        "coachId" -> coachId,
        "available" -> true,
        "status" -> Student.Status.Approved.id
      ) ++ q.trim.nonEmpty.?? {
          $doc("studentId" $regex (q.trim, "i"))
        }
    ).sort($doc("studentId" -> 1)).list() flatMap withUsers

  private def withUsers(students: Seq[Student]): Fu[List[StudentWithUser]] =
    UserRepo.withColl {
      _.byOrderedIds[User, User.ID](students.map(_.studentId))(_.id)
    } map { users =>
      students zip users collect {
        case (student, user) => StudentWithUser(student, user)
      } toList
    }

  def addOrReAdd(coachId: User.ID, studentId: User.ID): Funit = {
    val id = Student.makeId(coachId, studentId)
    byId(id) flatMap {
      case None => add(coachId, studentId) >>- applyNotify(coachId: User.ID, studentId: User.ID)
      case Some(s) => (!s.available || s.status == Student.Status.Decline).?? {
        coll.update(
          $id(id),
          $set(
            "available" -> true,
            "status" -> Student.Status.Applying.id,
            "createAt" -> DateTime.now
          )
        ).void >>- applyNotify(coachId: User.ID, studentId: User.ID)
      }
    }
  }

  def add(coachId: User.ID, studentId: User.ID): Funit =
    coll.insert(
      Student.make(coachId, studentId)
    ).void

  def join(coachId: String, studentId: String): Funit = {
    val id = Student.makeId(coachId, studentId)
    coll.update(
      $id(id),
      Student(
        id = id,
        coachId = coachId,
        studentId = studentId,
        available = true,
        status = Status.Approved,
        createAt = DateTime.now,
        approvedAt = DateTime.now.some
      ),
      upsert = true
    ).void
  }

  def approve(id: String, coachId: User.ID, studentId: User.ID): Funit =
    coll.update(
      $id(id),
      $set(
        "status" -> Student.Status.Approved.id,
        "approvedAt" -> DateTime.now
      )
    ).void >>- approveNotify(coachId, studentId)

  def decline(id: String): Funit =
    coll.update(
      $id(id),
      $set("status" -> Student.Status.Decline.id)
    ).void

  def remove(id: String): Funit =
    coll.update(
      $id(id),
      $set("available" -> false)
    ).void

  private def applyNotify(coachId: User.ID, studentId: User.ID): Funit = {
    notifyApi.addNotification(Notification.make(
      Notifies(coachId),
      lila.notify.GenericLink(
        url = "/coach/student/list/applying",
        title = "学员申请".some,
        text = s"$studentId 申请成为学员".some,
        icon = "教"
      )
    ))
  }

  private def approveNotify(coachId: User.ID, studentId: User.ID): Funit = {
    notifyApi.addNotification(Notification.make(
      Notifies(studentId),
      lila.notify.GenericLink(
        url = s"/coach/$coachId",
        title = "学员申请通过".some,
        text = s"您已经成为${coachId}的学员".some,
        icon = "教"
      )
    ))
  }

}
