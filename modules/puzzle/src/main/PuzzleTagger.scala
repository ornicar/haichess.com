package lila.puzzle

import lila.db.dsl.bsonArrayToListHandler
import lila.user.User
import org.joda.time.DateTime

case class PuzzleTagger(
    _id: String,
    puzzleId: PuzzleId,
    user: User.ID,
    tags: Option[List[String]],
    date: DateTime
) {

}

object PuzzleTagger {

  object BSONFields {
    val id = "_id"
    val puzzleId = "puzzleId"
    val user = "user"
    val tags = "tags"
    val date = "date"
  }

  def makeId(puzzleId: PuzzleId, user: User.ID): String = puzzleId + "@" + user

  def empty(puzzleId: PuzzleId) = new PuzzleTagger(
    makeId(puzzleId, null),
    puzzleId,
    null,
    None,
    new DateTime()
  ).some

  import reactivemongo.bson.Macros
  import lila.db.BSON.BSONJodaDateTimeHandler

  private implicit val arrayHandler = bsonArrayToListHandler[String]
  implicit val taggerBSONHandler = Macros.handler[PuzzleTagger]
}

