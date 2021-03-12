package views.html.game

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object importFeedback {

  def apply(source: String, success: Int)(implicit ctx: Context) = views.html.base.layout(
    title = "导入成功"
  ) {
    val url = source match {
      case "puzzle" => routes.Resource.puzzleImported(1)
      case _ => routes.Resource.gameImported(1)
    }
    main(cls := "importer page-small box box-pad")(
      h1("导入完成"),
      div(
        h3("成功导入 ", strong(success), " 个资源，接下来您想："),
        br,
        br,
        div(
          a(cls := "button", href := routes.Importer.importGame)("继续导入"),
          span("            "),
          a(cls := "button button-empty", href := url)("查看资源")
        )
      )
    )
  }
}
