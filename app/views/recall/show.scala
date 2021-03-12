package views.html.recall

import lila.api.Context
import play.api.libs.json.{ JsObject, Json }
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import play.api.data.Form
import controllers.routes

object show {

  def apply(data: JsObject, pref: JsObject, home: Boolean = false)(implicit ctx: Context) = views.html.base.layout(
    title = "记谱",
    moreCss = frag(
      cssTag("recall")
    ),
    moreJs = frag(
      jsAt(s"compiled/lichess.recall${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""lichess=lichess||{};lichess.recall=${
        safeJsonValue(Json.obj(
          "userId" -> ctx.userId,
          "home" -> home,
          "data" -> data,
          "pref" -> pref
        ))
      }""")
    ),
    chessground = false,
    zoomable = true
  ) {
      main(cls := "recall")(
        st.aside(cls := "recall__side"),
        div(cls := "recall__board main-board")(chessgroundBoard),
        div(cls := "recall__tools"),
        div(cls := "recall__controls")
      )
    }

}
