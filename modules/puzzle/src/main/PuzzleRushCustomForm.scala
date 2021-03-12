package lila.puzzle

import play.api.data._
import play.api.data.Forms._
import lila.common.Form._

object PuzzleRushCustomForm {

  def phase = List("Opening" -> "开局", "MiddleGame" -> "中局", "EndingGame" -> "残局")

  def color = List("White" -> "白方", "Black" -> "黑方")

  def selector = List("round" -> "循环", "random" -> "随机")

  def customForm = Form(mapping(
    "ratingMin" -> optional(number(min = 600, max = 2800)),
    "ratingMax" -> optional(number(min = 600, max = 2800)),
    "stepsMin" -> optional(number(min = 1, max = 10)),
    "stepsMax" -> optional(number(min = 1, max = 10)),
    "phase" -> optional(stringIn(phase)),
    "color" -> optional(stringIn(color)),
    "selector" -> optional(stringIn(selector)),
    "minutes" -> number(min = 1, max = 30),
    "limit" -> number(min = 1, max = 20)
  )(CustomCondition.apply)(CustomCondition.unapply))

}

case class CustomCondition(
    ratingMin: Option[Int] = None,
    ratingMax: Option[Int] = None,
    stepsMin: Option[Int] = None,
    stepsMax: Option[Int] = None,
    phase: Option[String] = None,
    color: Option[String] = None,
    selector: Option[String] = None,
    minutes: Int = 3,
    limit: Int = 3
) {

  def isNone =
    ratingMin.isEmpty && ratingMax.isEmpty && stepsMin.isEmpty && stepsMax.isEmpty && phase.isEmpty && color.isEmpty && selector.isEmpty

  def isRandom = (selector | "round") == "random"
}

