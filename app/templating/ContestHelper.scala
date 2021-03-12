package lila.app
package templating

import controllers.routes
import lila.app.ui.ScalatagsTemplate._
import lila.contest.Env.{ current => contestEnv }
import lila.contest.Contest

trait ContestHelper { self: I18nHelper with DateHelper with UserHelper =>

  def contestLink(contestId: String): Frag = a(
    dataIcon := "赛",
    cls := "text",
    href := routes.Contest.show(contestId)
  )(contestIdToName(contestId))

  def contestLink(contestId: String, contestName: String): Frag = a(
    dataIcon := "赛",
    cls := "text",
    href := routes.Contest.show(contestId)
  )(contestName)

  def contestIdToName(id: String) = contestEnv.cached name id getOrElse "比赛"

  def contestIconChar(c: Contest): String = c.perfType.fold('g')(_.iconChar).toString

}
