package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.app.mashup.TeamInfo
import controllers.routes

object tour {

  def widget(tours: List[TeamInfo.AnyTour])(implicit ctx: Context) =
    table(cls := "slist")(
      tbody(
        tours map { t =>
          tr(cls := List("enterable" -> t.isEnterable, "soon" -> t.isNowOrSoon))(
            td(cls := "icon")(iconTag(t.any.fold(tournamentIconChar, contestIconChar))),
            td(cls := "header")(
              t.any.fold(
                t =>
                  a(href := routes.Tournament.show(t.id))(
                    span(cls := "name")(t.name),
                    span(cls := "setup")(
                      t.clock.show,
                      " • ",
                      if (t.variant.exotic) t.variant.name else t.perfType.map(_.name),
                      !t.position.initial option frag(" • ", trans.thematic()),
                      " • ",
                      t.mode.fold(trans.casualTournament, trans.ratedTournament)()
                    )
                  ),
                c =>
                  a(href := routes.Contest.show(c.id))(
                    span(cls := "name")(c.fullName),
                    span(cls := "setup")(
                      c.clock.show,
                      " • ",
                      if (c.variant.exotic) c.variant.name else c.perfType.map(_.name),
                      " • ",
                      (if (c.rated) trans.ratedTournament else trans.casualTournament)()
                    )
                  )
              )
            ),
            td(cls := "infos")(
              t.any.fold(
                t =>
                  frag(
                    t.durationString,
                    br,
                    momentFromNowOnce(t.startsAt)
                  ),
                s =>
                  frag(
                    s.actualNbRounds,
                    " 轮",
                    br,
                    momentFromNowOnce(s.startsAt)
                  )
              )
            ),
            td(cls := "text", dataIcon := "r")(t.nbPlayers.localize)
          )
        }
      )
    )
}
