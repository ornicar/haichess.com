package views.html.challenge

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge.Status

import controllers.routes

object mine {

  def apply(c: lila.challenge.Challenge, json: play.api.libs.json.JsObject, error: Option[String])(implicit ctx: Context) = {

    val cancelForm =
      postForm(action := routes.Challenge.cancel(c.id), cls := "cancel xhr")(
        submitButton(cls := "button button-red text", dataIcon := "L")(trans.cancel())
      )

    views.html.base.layout(
      title = challengeTitle(c),
      openGraph = challengeOpenGraph(c).some,
      moreJs = bits.js(c, json, true),
      moreCss = cssTag("challenge.page")
    ) {
        val challengeLink = s"$netBaseUrl${routes.Round.watcher(c.id, "white")}"
        main(cls := "page-small challenge-page box box-pad")(
          c.status match {
            case Status.Created | Status.Offline => div(id := "ping-challenge")(
              h1(trans.challengeToPlay()),
              bits.details(c),
              c.destUserId.map { destId =>
                div(cls := "waiting")(
                  userIdLink(destId.some, cssClass = "large target".some),
                  !c.appt option spinner,
                  p("等待对手"),
                  c.appt option p("您可以", nbsp, a(href := s"${routes.Lobby.home()}#friend")("继续约棋"), nbsp, "或", nbsp, a(href := routes.Appt.form(c.id))("修改约棋时间"))
                )
              } getOrElse div(cls := "invite")(
                !c.appt option div(
                  h2(cls := "ninja-title", trans.toInviteSomeoneToPlayGiveThisUrl(), ": "), br,
                  p(cls := "challenge-id-form")(
                    input(
                      id := "challenge-id",
                      cls := "copyable autoselect",
                      spellcheck := "false",
                      readonly,
                      value := challengeLink,
                      size := challengeLink.size
                    ),
                    button(title := "复制URL", cls := "copy button", dataRel := "challenge-id", dataIcon := "\"")
                  ),
                  p(trans.theFirstPersonToComeOnThisUrlWillPlayWithYou())
                ),
                c.appt option div(
                  h2(cls := "ninja-title", "选择一位好友"), br,
                  ctx.me.map { user =>
                    a(cls := "button", style := "display:block;", href := routes.Relation.following(user.username, 1, c.id.some))("选择一位好友")
                  }
                ),
                ctx.isAuth option div(
                  h2(cls := "ninja-title", "或邀请一位棋手"), br,
                  postForm(cls := "user-invite", action := routes.Challenge.toFriend(c.id))(
                    input(name := "username", cls := "friend-autocomplete", placeholder := trans.search.txt()),
                    error.map { badTag(_) }
                  )
                )
              ),
              c.notableInitialFen.map { fen =>
                frag(
                  br,
                  div(cls := "board-preview", views.html.game.bits.miniBoard(fen, color = c.finalColor))
                )
              },
              cancelForm
            )
            case Status.Declined => div(cls := "follow-up")(
              h1("拒绝挑战"),
              bits.details(c),
              a(cls := "button button-fat", href := routes.Lobby.home())(trans.newOpponent())
            )
            case Status.Accepted => div(cls := "follow-up")(
              h1("接受挑战"),
              bits.details(c),
              a(id := "challenge-redirect", href := routes.Round.watcher(c.id, "white"), cls := "button-fat")(
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
}
