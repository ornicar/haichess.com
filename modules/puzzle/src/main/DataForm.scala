package lila.puzzle

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val round = Form(mapping(
    "win" -> number,
    "seconds" -> number,
    "homeworkId" -> optional(nonEmptyText),
    "lines" -> optional(list(mapping(
      "san" -> nonEmptyText,
      "uci" -> nonEmptyText,
      "fen" -> nonEmptyText
    )(ResultNode.apply)(ResultNode.unapply))),
    "timeout" -> optional(boolean)
  )(RoundData.apply)(RoundData.unapply))

  val vote = Form(single(
    "vote" -> number
  ))

  val like = Form(single(
    "like" -> boolean
  ))

  val tag = Form(single(
    "tags" -> text(minLength = 0, maxLength = 200)
  ))

  case class RoundData(
      win: Int,
      seconds: Int,
      homeworkId: Option[String],
      lines: Option[List[ResultNode]],
      timeout: Option[Boolean]
  ) {
    def linesWithEmpty = lines | List.empty[ResultNode]
  }

}
