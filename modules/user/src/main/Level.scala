package lila.user

import lila.db.dsl.Barr
import org.joda.time.DateTime
import reactivemongo.bson.BSONHandler

case class Level(
    level: String,
    current: Int,
    name: Option[String] = None,
    time: Option[DateTime] = None,
    result: Option[String] = None
) {

  val label = FormSelect.Level.levelLabel(level)

}

object ChessAssociation {

}

case class Levels(lvs: List[Level]) {

  def +(lv: Level) = copy(lvs = (lvs ++ List(lv)))

  def current: Level = lvs.find(_.current == 1) match {
    case None => sys error "Invalid Level"
    case Some(l) => l
  }

}

object Levels {

  val empty = Levels(List.empty)

  import reactivemongo.bson.Macros
  import lila.db.BSON.BSONJodaDateTimeHandler
  implicit val levelBSONHandler = Macros.handler[Level]
  private[user] val levelsBSONHandler = new BSONHandler[Barr, Levels] {
    val levelArrayHandler = lila.db.dsl.bsonArrayToListHandler[Level]
    def read(b: Barr): Levels = Levels(levelArrayHandler.read(b))
    def write(levels: Levels): Barr = levelArrayHandler.write(levels.lvs)
  }
}