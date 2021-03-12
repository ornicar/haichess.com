package views.html.clazz.homework

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object notFound {

  def apply()(implicit ctx: Context) =
    views.html.base.layout(
      title = "课后练",
      moreCss = cssTag("homework")
    ) {
        main(cls := "box box-pad page-small notFound")(
          h1("没有找到"),
          br,
          div(cls := "message")(
            "本节课没有生成课后练，您可以看看其它课节"
          ),
          br, br, br, br,
          a(cls := "button button-flat", href := routes.Home.home)("返回")
        )
      }

}
