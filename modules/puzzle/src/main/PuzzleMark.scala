package lila.puzzle

import lila.db.dsl.bsonArrayToListHandler

case class PuzzleMark(
    id: Int,
    source: String,
    rating: Int,
    phase: Option[String],
    strength: Option[List[String]],
    moveFor: Option[List[String]],
    usePiece: Option[List[String]],
    subject: Option[List[String]],
    chessGame: Option[List[String]],
    comprehensive: Option[List[String]],
    tag: Option[List[String]]
) {

}

object PuzzleMark {
  import reactivemongo.bson.Macros
  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val arrayHandler = bsonArrayToListHandler[String]
  implicit val markBSONHandler = Macros.handler[PuzzleMark]
}
