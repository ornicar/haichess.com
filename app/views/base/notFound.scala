package views.html
package base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object notFound {

  def apply()(implicit ctx: Context) = layout(
    title = "找不到页面",
    moreJs = prismicJs,
    moreCss = cssTag("not-found"),
    csp = isGranted(_.Prismic) option defaultCsp.withPrismic(true)
  ) {
      main(cls := "not-found page-small box box-pad")(
        header(
          h1("404"),
          div(
            strong("找不到页面！"),
            p(
              "返回 ",
              a(href := routes.Home.home)("主页"),
              span(cls := "or-play")(" 或者玩一会小游戏")
            )
          )
        ),
        div(cls := "game")(
          iframe(
            src := staticUrl(s"vendor/ChessPursuit/bin-release/index.html"),
            st.frameborder := 0,
            width := 400,
            height := 500
          ) /*,
          p(cls := "credits")(
            a(href := "https://github.com/Saturnyn/ChessPursuit")("ChessPursuit"),
            " courtesy of ",
            a(href := "https://github.com/Saturnyn")("Saturnyn")
          )*/
        )
      )
    }
}
