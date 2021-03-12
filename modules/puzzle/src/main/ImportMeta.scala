package lila.puzzle

import lila.db.dsl.bsonArrayToListHandler
import org.joda.time.DateTime

case class ImportMeta(
    pgn: String,
    tags: Option[List[String]],
    hasLastMove: Boolean,
    fenAfterMove: Option[String],
    userId: Option[String],
    date: Option[String]
)

object ImportMeta {

  def make(
    pgn: String,
    tags: Option[List[String]],
    hasLastMove: Boolean,
    fenAfterMove: Option[String] = None,
    userId: Option[String] = None,
    date: Option[String] = None
  ) = ImportMeta(
    pgn = pgn,
    tags = tags,
    hasLastMove = hasLastMove,
    fenAfterMove = fenAfterMove,
    userId = userId,
    date = date
  ).some

  import reactivemongo.bson.Macros
  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val tagsHandler = bsonArrayToListHandler[String]
  implicit val importBSONHandler = Macros.handler[ImportMeta]

}