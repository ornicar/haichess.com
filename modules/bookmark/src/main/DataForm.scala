package lila.bookmark

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val tag = Form(single(
    "tags" -> text(minLength = 1, maxLength = 200)
  ))

}
