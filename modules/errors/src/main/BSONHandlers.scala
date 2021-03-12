package lila.errors

import chess.opening.{ Ecopening, EcopeningDB, FullOpening }
import reactivemongo.bson._
import lila.errors.GameErrors.Phase
import lila.errors.GameErrors.Judgement

object BSONHandlers {

  import lila.db.BSON.BSONJodaDateTimeHandler

  implicit val ColorBSONHandler = new BSONHandler[BSONBoolean, chess.Color] {
    def read(b: BSONBoolean) = chess.Color(b.value)
    def write(c: chess.Color) = BSONBoolean(c.white)
  }

  implicit val PhaseBSONHandler = new BSONHandler[BSONInteger, Phase] {
    def read(b: BSONInteger) = Phase(b.value)
    def write(p: Phase) = BSONInteger(p.id)
  }

  implicit val JudgementBSONHandler = new BSONHandler[BSONString, Judgement] {
    def read(b: BSONString) = Judgement(b.value)
    def write(j: Judgement) = BSONString(j.id)
  }

  /*  implicit val FullOpeningBSONHandler = new BSONHandler[BSONString, FullOpening] {
    def read(b: BSONString) = {
      val arr = b.value.split("    ")
      new FullOpening(arr(0), arr(1), arr(2))
    }
    def write(o: FullOpening) = BSONString(s"${o.eco}    ${o.name}    ${o.fen}")
  }*/

  implicit val EcopeningBSONHandler = new BSONHandler[BSONString, Ecopening] {
    def read(b: BSONString) = EcopeningDB.allByEco get b.value.split("    ")(0) err s"Invalid ECO ${b.value}"
    def write(e: Ecopening) = BSONString(s"${e.eco}    ${e.family}    ${e.name}")
  }

  implicit val PuzzleErrorsBSONHandler = Macros.handler[PuzzleErrors]

  implicit val GameErrorsBSONHandler = Macros.handler[GameErrors]

}
