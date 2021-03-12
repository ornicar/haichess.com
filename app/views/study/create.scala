package views.html.study

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.study.Study

import controllers.routes

object create {

  private def studyButton(s: Study.IdName) =
    submitButton(name := "as", value := s.id.value, cls := "submit button")(s.name.value)

  def apply(data: lila.study.DataForm.importGame.Data, owner: List[Study.IdName], contrib: List[Study.IdName])(implicit ctx: Context) =
    views.html.site.message(
      title = "研习",
      icon = Some("4"),
      back = true,
      moreCss = cssTag("study.create").some
    ) {
        div(cls := "study-create")(
          postForm(action := routes.Study.create)(
            input(tpe := "hidden", name := "gameId", value := data.gameId),
            input(tpe := "hidden", name := "orientation", value := data.orientationStr),
            input(tpe := "hidden", name := "fen", value := data.fenStr),
            input(tpe := "hidden", name := "pgn", value := data.pgnStr),
            input(tpe := "hidden", name := "variant", value := data.variantStr),
            p(
              submitButton(name := "as", value := "study", cls := "submit button large new text", dataIcon := "4")("创建研习")
            ),
            div(cls := "studies")(
              div(
                h2("我的研习"),
                owner map studyButton
              ),
              div(
                h2("我参与的研习"),
                contrib map studyButton
              )
            )
          )
        )
      }
}
