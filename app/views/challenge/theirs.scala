package views.html.challenge

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge.Status

import controllers.routes

object theirs {

  def apply(
    c: lila.challenge.Challenge,
    json: play.api.libs.json.JsObject,
    user: Option[lila.user.User]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      moreJs = bits.js(c, json, false),
      moreCss = cssTag("challenge.page")
    ) {
        main(cls := "page-small challenge-page challenge-theirs box box-pad")(
          c.status match {
            case Status.Created | Status.Offline => frag(
              h1(cls := "challenge-user")(user.fold[Frag]("Anonymous")(u =>
                frag(
                  userLink(u, cssClass = "large".some),
                  " (", u.perfs(c.perfType).glicko.display, ")"
                ))),
              bits.details(c),
              c.notableInitialFen.map { fen =>
                div(cls := "board-preview", views.html.game.bits.miniBoard(fen, color = !c.finalColor))
              },
              if (!c.mode.rated || ctx.isAuth) frag(
                (c.mode.rated && c.unlimited) option
                  badTag(trans.bewareTheGameIsRatedButHasNoClock()),
                if (c.appt) {
                  postForm(cls := "accept", action := routes.Appt.accept(c.id))(
                    submitButton(cls := "text button button-fat", dataIcon := "G")(trans.joinTheGame())
                  )
                } else {
                  postForm(cls := "accept", action := routes.Challenge.accept(c.id))(
                    submitButton(cls := "text button button-fat", dataIcon := "G")(trans.joinTheGame())
                  )
                }
              )
              else frag(
                hr,
                badTag(
                  p("这是一个积分对局"),
                  p(
                    "您必须 ",
                    a(cls := "button", href := s"${routes.Auth.login}?referrer=${routes.Round.watcher(c.id, "white")}")(trans.signIn()),
                    " 后加入。"
                  )
                )
              )
            )
            case Status.Declined => div(cls := "follow-up")(
              h1("拒绝挑战"),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent())
            )
            case Status.Accepted => div(cls := "follow-up")(
              h1("接受挑战"),
              bits.details(c),
              a(id := "challenge-redirect", href := routes.Round.watcher(c.id, "white"), cls := "button button-fat")(
                trans.joinTheGame()
              )
            )
            case Status.Canceled => div(cls := "follow-up")(
              h1("取消挑战"),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent())
            )
            case Status.Processed => c.appt option div(cls := "follow-up")(
              h1("修改预约时间"),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Appt.form(c.id))("修改预约时间")
            )
          }
        )
      }
}
