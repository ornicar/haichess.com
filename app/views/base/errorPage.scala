package views.html
package base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object errorPage {

  def apply(ex: Throwable)(implicit ctx: Context) = layout(
    title = "内部服务器错误"
  ) {
    main(cls := "page-small box box-pad")(
      h1("这页出了点问题"),
      p(
        "如果问题仍然存在, 请 ",
        a(href := s"${routes.Page.contact}#help-error-page")("反馈问题"),
        "."
      ),
      code(ex.getMessage)
    )
  }
}
