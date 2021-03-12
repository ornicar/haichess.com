package lila.appt

import lila.db.BSON
import lila.db.dsl._
import reactivemongo.bson._
import chess.Clock.{ Config => ClockConfig }
import chess.Mode
import chess.variant.Variant

object BSONHandlers {

  import reactivemongo.bson.Macros
  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val apptRecordStatusBSONHandler = new BSONHandler[BSONString, ApptRecord.Status] {
    def read(b: BSONString): ApptRecord.Status = ApptRecord.Status(b.value)
    def write(s: ApptRecord.Status) = BSONString(s.id)
  }
  implicit val apptRecordHandler = Macros.handler[ApptRecord]
  implicit val apptRecordArrayHandler = lila.db.dsl.bsonArrayToListHandler[ApptRecord]
  private implicit val clockBSONHandler = new BSONHandler[BSONDocument, ClockConfig] {
    def read(doc: BSONDocument) = ClockConfig(
      doc.getAs[Int]("limit").get,
      doc.getAs[Int]("increment").get
    )
    def write(config: ClockConfig) = BSONDocument(
      "limit" -> config.limitSeconds,
      "increment" -> config.incrementSeconds
    )
  }

  implicit val ApptContestHandler = new BSON[ApptContest] {
    def reads(r: BSON.Reader) = ApptContest(
      id = r str "id",
      name = r str "name",
      logo = r strO "logo",
      roundNo = r int "roundNo",
      boardNo = r int "boardNo"
    )
    def writes(w: BSON.Writer, c: ApptContest) = $doc(
      "id" -> c.id,
      "name" -> c.name,
      "logo" -> c.logo,
      "roundNo" -> c.roundNo,
      "boardNo" -> c.boardNo
    )
  }

  implicit val ApptHandler = new BSON[Appt] {
    def reads(r: BSON.Reader) = Appt(
      id = r str "_id",
      position = r str "position",
      variant = r.intO("variant").fold[Variant](Variant.default)(Variant.orDefault),
      rated = r.bool("rated"),
      clock = r.getO[ClockConfig]("clock"),
      daysPerTurn = r intO "daysPerTurn",
      minDateTime = r date "minDateTime",
      maxDateTime = r date "maxDateTime",
      whitePlayerUid = r str "whitePlayerUid",
      blackPlayerUid = r str "blackPlayerUid",
      records = r.get[List[ApptRecord]]("records"),
      contest = r.getO[ApptContest]("contest"),
      confirmed = r int "confirmed",
      finalTime = r dateO "finalTime",
      canceled = r boolD "canceled",
      createBy = r strO "createBy",
      createAt = r date "createAt",
      updateAt = r date "updateAt"
    )

    def writes(w: BSON.Writer, a: Appt) = $doc(
      "_id" -> a.id,
      "position" -> a.position,
      "variant" -> a.variant.id,
      "rated" -> a.rated,
      "clock" -> a.clock.map(clockBSONHandler.write(_)),
      "daysPerTurn" -> a.daysPerTurn,
      "minDateTime" -> a.minDateTime,
      "maxDateTime" -> a.maxDateTime,
      "whitePlayerUid" -> a.whitePlayerUid,
      "blackPlayerUid" -> a.blackPlayerUid,
      "records" -> apptRecordArrayHandler.write(a.records),
      "contest" -> a.contest,
      "confirmed" -> a.confirmed,
      "finalTime" -> a.finalTime,
      "canceled" -> a.canceled,
      "createBy" -> a.createBy,
      "createAt" -> a.createAt,
      "updateAt" -> a.updateAt
    )
  }

}
