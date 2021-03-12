package lila.team

import lila.db.dsl._
import lila.db.BSON
import reactivemongo.bson.{ BSONHandler, BSONString, BSONWriter, Macros, BSONDocument }

private object BSONHandlers {

  import lila.db.dsl.BSONJodaDateTimeHandler

  implicit val TagTypeBSONHandler = new BSONHandler[BSONString, Tag.Type] {
    def read(b: BSONString) = Tag.Type.byId get b.value err s"Invalid TagType. ${b.value}"
    def write(x: Tag.Type) = BSONString(x.id)
  }

  implicit val RoleBSONHandler = new BSONHandler[BSONString, Member.Role] {
    def read(b: BSONString) = Member.Role.byId get b.value err s"Invalid role ${b.value}"
    def write(x: Member.Role) = BSONString(x.id)
  }

  implicit val CertificationStatusBSONHandler = new BSONHandler[BSONString, Certification.Status] {
    def read(b: BSONString): Certification.Status = Certification.Status(b.value)
    def write(c: Certification.Status) = BSONString(c.id)
  }

  implicit val MemberTagBSONHandler = Macros.handler[MemberTag]
  implicit val MemberTagBSONWriter = new BSONWriter[MemberTag, Bdoc] {
    def write(x: MemberTag) = MemberTagBSONHandler write x
  }
  implicit val MemberTagsBSONHandler = new BSONHandler[Bdoc, MemberTags] {
    private val mapHandler = BSON.MapDocument.MapHandler[String, MemberTag]
    def read(b: Bdoc) = MemberTags(mapHandler read b map {
      case (field, tag) => field -> tag
    })
    def write(x: MemberTags) = BSONDocument(x.tagMap.mapValues(MemberTagBSONWriter.write))
  }

  implicit val EnvPictureHandler = lila.db.dsl.bsonArrayToListHandler[String]
  implicit val CertificationBSONHandler = Macros.handler[Certification]
  implicit val TeamBSONHandler = Macros.handler[Team]
  implicit val RequestBSONHandler = Macros.handler[Request]
  implicit val InviteBSONHandler = Macros.handler[Invite]
  implicit val MemberBSONHandler = Macros.handler[Member]
  implicit val TagBSONHandler = Macros.handler[Tag]
}
