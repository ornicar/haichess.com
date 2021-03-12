package lila.errors

import play.api.data._
import play.api.data.Forms._
import lila.common.Form._

object DataForm {

  val puzzle = Form(mapping(
    "ratingMin" -> optional(number(min = 600, max = 2800)),
    "ratingMax" -> optional(number(min = 600, max = 2800)),
    "depthMin" -> optional(number(min = 1, max = 100)),
    "depthMax" -> optional(number(min = 1, max = 100)),
    "color" -> optional(list(stringIn(colorChoices))),
    "rating" -> optional(number(min = 600, max = 2800)),
    "time" -> optional(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)
  )(PuzzleQuery.apply)(PuzzleQuery.unapply))

  val game = Form(mapping(
    "gameAtMin" -> optional(ISODate.isoDate),
    "gameAtMax" -> optional(ISODate.isoDate),
    "color" -> optional(list(stringIn(colorChoices))),
    "opponent" -> optional(lila.user.DataForm.historicalUsernameField),
    "phase" -> optional(stringIn(phaseChoices)),
    "judgement" -> optional(stringIn(judgementChoices)),
    "eco" -> optional(text)
  )(GameQuery.apply)(GameQuery.unapply))

  def colorChoices = List("white" -> "白棋", "black" -> "黑棋")
  def phaseChoices: List[(String, String)] = GameErrors.Phase.all.map(p => p.id.toString -> p.name)
  def judgementChoices: List[(String, String)] = GameErrors.Judgement.all.map(p => p.id -> p.name)

}

