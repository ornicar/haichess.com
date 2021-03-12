package lila.coach

import Coach.User
import lila.db.dsl._
import reactivemongo.bson._
import lila.coach.Certify._
import lila.db.BSON.BSONJodaDateTimeHandler

private[coach] object BsonHandlers {

  implicit val CoachIdBSONHandler = stringAnyValHandler[Coach.Id](_.value, Coach.Id.apply)
  implicit val CoachAvailableBSONHandler = booleanAnyValHandler[Coach.Available](_.value, Coach.Available.apply)
  implicit val CoachProfileRichTextBSONHandler = stringAnyValHandler[CoachProfile.RichText](_.value, CoachProfile.RichText.apply)
  implicit val CoachProfileBSONHandler = Macros.handler[CoachProfile]

  implicit val StatusBSONHandler = new BSONHandler[BSONString, Status] {
    def read(b: BSONString): Status = Status(b.value)
    def write(c: Status) = BSONString(c.id)
  }
  implicit val CertifyBSONHandler = Macros.handler[Certify]

  implicit val CoachUserBSONHandler = Macros.handler[User]

  implicit val CoachBSONHandler = Macros.handler[Coach]

  implicit val StudentStatusBSONHandler = new BSONHandler[BSONString, Student.Status] {
    def read(b: BSONString): Student.Status = Student.Status(b.value)
    def write(c: Student.Status) = BSONString(c.id)
  }

  import lila.db.BSON
  implicit val StudentBSONHandler = new BSON[Student] {
    def reads(r: BSON.Reader) = Student(
      id = r str "_id",
      coachId = r str "coachId",
      studentId = r str "studentId",
      available = r bool "available",
      status = r.get[Student.Status]("status"),
      createAt = r date "createAt",
      approvedAt = r dateO "approvedAt"
    )

    def writes(w: BSON.Writer, r: Student) = $doc(
      "_id" -> r.id,
      "coachId" -> r.coachId,
      "studentId" -> r.studentId,
      "available" -> r.available,
      "status" -> r.status.id,
      "createAt" -> r.createAt,
      "approvedAt" -> r.approvedAt
    )
  }

}
