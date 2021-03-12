package views.html.search

import play.api.data.Form
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.gameSearch.{ Query, Sorting }
import lila.user.User
import controllers.routes
import play.mvc.Call

object user {

  def apply(u: User, form: Form[_], call: Call)(implicit ctx: Context) = {
    val commons = bits of form
    import commons._
    st.form(
      rel := "nofollow",
      cls := "search__form",
      action := call.url,
      method := "GET"
    )(dataReqs)(
        table(
          date,
          rating,
          turns,
          duration,
          clockTime,
          clockIncrement,
          source,
          perf,
          mode
        ),
        table(
          hasAi,
          aiLevel,
          tr(cls := "opponentName")(
            th(label(`for` := form3.id(form("players")("b")))("对手名称")),
            td(cls := "usernames")(
              st.input(tpe := "hidden", value := u.id, name := "players.a"),
              form3.input(form("players")("b"))(tpe := "text")
            )
          ),
          winner(hide = false),
          loser(hide = false),
          colors(hide = false),
          status,
          winnerColor,
          sort,
          analysed,
          tr(cls := "action")(
            th,
            td(button(cls := "button")(trans.search()))
          )
        )
      )
  }
}
