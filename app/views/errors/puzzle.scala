package views.html.errors

import lila.api.Context
import play.api.data.Form
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.errors.{ DataForm, PuzzleErrors }
import controllers.routes

object puzzle {

  def apply(form: Form[_], pager: Paginator[PuzzleErrors])(implicit ctx: Context) = bits.layout(
    title = "错题库-战术题",
    active = "puzzle",
    form = form,
    pager = pager,
    call = routes.Errors.puzzle(1)
  ) {
      st.form(
        cls := "search_form",
        action := s"${routes.Errors.puzzle(1)}#results",
        method := "GET"
      )(
          table(
            tr(
              th(label("难度范围")),
              td(
                div(cls := "half")("从 ", form3.input(form("ratingMin"), "number")),
                div(cls := "half")("到 ", form3.input(form("ratingMax"), "number"))
              )
            ),
            tr(
              th(label("答案步数")),
              td(
                div(cls := "half")("从 ", form3.input(form("depthMin"), "number")),
                div(cls := "half")("到 ", form3.input(form("depthMax"), "number"))
              )
            ),
            tr(
              th(label("棋色")),
              td(
                form3.tagsWithKv(form, "color", DataForm.colorChoices)
              )
            ),
            tr(
              th,
              td(cls := "action")(
                submitButton(cls := "button")("搜索")
              )
            )
          ),
          table(
            tr(
              td(cls := "action")(
                pager.nbResults > 0 option bits.actions(List("delete"))
              )
            )
          )
        )
    }

}
