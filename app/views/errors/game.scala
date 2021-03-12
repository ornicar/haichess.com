package views.html.errors

import lila.api.Context
import play.api.data.Form
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.errors.{ DataForm, GameErrors }
import controllers.routes

object game {

  def apply(form: Form[_], pager: Paginator[GameErrors])(implicit ctx: Context) = bits.layout(
    title = "错题库-对局",
    active = "game",
    form = form,
    pager = pager,
    call = routes.Errors.game(1)
  ) {
      st.form(
        cls := "search_form",
        action := s"${routes.Errors.game(1)}#results",
        method := "GET"
      )(
          table(
            tr(
              th(label("对局时间（从）")),
              td(form3.input(form("gameAtMin"), klass = "flatpickr")),
              th(label("对局时间（到）")),
              td(form3.input(form("gameAtMax"), klass = "flatpickr"))
            ),
            tr(
              th(label("错误程度")),
              td(form3.select(form("judgement"), DataForm.judgementChoices, "".some)),
              th(label("阶段")),
              td(form3.select(form("phase"), DataForm.phaseChoices, "".some))
            ),
            tr(
              th(label("对手账号")),
              td(form3.input(form("opponent"))),
              th(label("ECO")),
              td(form3.input(form("eco")))
            ),
            tr(
              th(label("棋色")),
              td(colspan := 3)(
                form3.tagsWithKv(form, "color", DataForm.colorChoices)
              )
            ),
            tr(
              th,
              td(colspan := 3, cls := "action")(
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
