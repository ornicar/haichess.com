package lila.recall

import reactivemongo.bson.{ BSONBoolean, BSONHandler, Macros }
import lila.db.BSON.BSONJodaDateTimeHandler

object BSONHandlers {

  private implicit val colorBSONHandler = new BSONHandler[BSONBoolean, chess.Color] {
    def read(b: BSONBoolean) = chess.Color(b.value)
    def write(c: chess.Color) = BSONBoolean(c.white)
  }

  implicit val RecallHandler = Macros.handler[Recall]

}
