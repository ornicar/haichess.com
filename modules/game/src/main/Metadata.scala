package lila.game

import java.security.MessageDigest
import lila.db.ByteArray
import lila.db.dsl.bsonArrayToListHandler

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    tournamentId: Option[String],
    contestId: Option[String],
    contestCanLateMinute: Option[Int],
    simulId: Option[String],
    analysed: Boolean,
    appt: Boolean,
    apptComplete: Boolean
) {

  def pgnDate = pgnImport flatMap (_.date)

  def pgnUser = pgnImport flatMap (_.user)

  def isEmpty = this == Metadata.empty
}

private[game] object Metadata {

  val empty = Metadata(None, None, None, None, None, None, false, false, false)
}

case class PgnImport(
    user: Option[String],
    date: Option[String],
    pgn: String,
    // hashed PGN for DB unicity
    h: Option[ByteArray],
    tags: Option[List[String]] = None
)

object PgnImport {

  def hash(pgn: String) = ByteArray {
    MessageDigest getInstance "MD5" digest
      pgn.lines.map(_.replace(" ", "")).filter(_.nonEmpty).mkString("\n").getBytes("UTF-8") take 12
  }

  def make(
    user: Option[String],
    date: Option[String],
    pgn: String,
    tags: Option[List[String]] = None
  ) = PgnImport(
    user = user,
    date = date,
    pgn = pgn,
    h = hash(pgn).some,
    tags
  )

  import reactivemongo.bson.Macros
  import ByteArray.ByteArrayBSONHandler
  private implicit val tagsHandler = bsonArrayToListHandler[String]
  implicit val pgnImportBSONHandler = Macros.handler[PgnImport]
}
