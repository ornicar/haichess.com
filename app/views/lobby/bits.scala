package views.html.lobby

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  val lobbyApp = div(cls := "lobby__app")(
    div(cls := "tabs-horiz")(span(nbsp)),
    div(cls := "lobby__app__content")
  )

  def underboards(
    tours: List[lila.tournament.Tournament],
    simuls: List[lila.simul.Simul],
    leaderboard: List[lila.user.User.LightPerf],
    tournamentWinners: List[lila.tournament.Winner],
    puzzle: Option[lila.puzzle.DailyPuzzle]
  )(implicit ctx: Context) = frag(
    div(cls := "lobby__leaderboard lobby__box")(
      div(cls := "lobby__box__top")(
        h2(cls := "title text", dataIcon := "C")(trans.leaderboard()),
        a(cls := "more", href := routes.User.list)(trans.more(), " »")
      ),
      div(cls := "lobby__box__content")(
        table(tbody(
          leaderboard map { l =>
            tr(
              td(lightUserLink(l.user)),
              lila.rating.PerfType(l.perfKey) map { pt =>
                td(cls := "text", dataIcon := pt.iconChar)(l.rating)
              },
              td(ratingProgress(l.progress))
            )
          }
        ))
      )
    ),
    div(cls := "lobby__winners lobby__box")(
      frag(
        div(cls := "lobby__box__top")(
          h2(cls := "title text", dataIcon := "-")("每日一题 " /*, "(", p.color.fold(trans.whitePlays, trans.blackPlays)(), ")"*/ ),
          a(cls := "more", href := routes.Puzzle.home)(trans.more(), " »")
        ),
        div(cls := "lobby__box__content")(
          puzzle.map { p =>
            raw(p.html)
          }
        )
      )

    /*      div(cls := "lobby__box__top")(
        h2(cls := "title text", dataIcon := "g")(trans.tournamentWinners()),
        a(cls := "more", href := routes.Tournament.leaderboard)(trans.more(), " »")
      ),
      div(cls := "lobby__box__content")(
        table(tbody(
          tournamentWinners take 10 map { w =>
            tr(
              td(userIdLink(w.userId.some)),
              td(a(title := w.tourName, href := routes.Tournament.show(w.tourId))(scheduledTournamentNameShortHtml(w.tourName)))
            )
          }
        ))
      )*/
    )
  /*
    ,
      div(cls := "lobby__tournaments lobby__box")(
      div(cls := "lobby__box__top")(
        h2(cls := "title text", dataIcon := "g")(trans.openTournaments()),
        a(cls := "more", href := routes.Tournament.home())(trans.more(), " »")
      ),
      div(id := "enterable_tournaments", cls := "enterable_list lobby__box__content")(
        views.html.tournament.bits.enterable(tours)
      )
    ),
    div(cls := "lobby__simuls lobby__box")(
      div(cls := "lobby__box__top")(
        h2(cls := "title text", dataIcon := "f")(trans.simultaneousExhibitions()),
        a(cls := "more", href := routes.Simul.home())(trans.more(), " »")
      ),
      div(id := "enterable_simuls", cls := "enterable_list lobby__box__content")(
        views.html.simul.bits.allCreated(simuls)
      )
    )
    */
  )

  def lastPosts(posts: List[lila.blog.MiniPost])(implicit ctx: Context): Option[Frag] = posts.nonEmpty option
    div(cls := "lobby__blog lobby__box")(
      div(cls := "lobby__box__top")(
        h2(cls := "title text", dataIcon := "6")(trans.latestUpdates()),
        a(cls := "more", href := routes.Blog.index())(trans.more(), " »")
      ),
      div(cls := "lobby__box__content")(
        posts map { post =>
          a(cls := "post", href := routes.Blog.show(post.id, post.slug))(
            img(src := post.image),
            span(cls := "text")(
              strong(post.title),
              span(post.shortlede)
            ),
            semanticDate(post.date)
          )
        }
      )
    )

  def playbanInfo(ban: lila.playban.TempBan)(implicit ctx: Context) = nopeInfo(
    h1("对不起 :("),
    p("您已被禁赛 ", (ban.remainingSeconds < 3600) ?? "一小时 "),
    p("您将在 ", strong(secondsFromNow(ban.remainingSeconds)), " 恢复比赛资格。"),
    h2("禁赛原因："),
    p(
      "我们致力于为所有用户提供一个公平、愉快的竞赛环境", br,
      "我们尽量保证每位棋手准守国际象棋的礼仪和规范", br,
      "如果一个可疑的违规行为被检测到，将显示这条信息。"
    ),
    h2("如何避免发生类似情况？"),
    ul(
      li("认真对待每场开始的比赛"),
      li("尽量争取获得比赛胜利（或至少平局）"),
      li("如果您认为已经无法挽回局面，应该直接认输而不是等棋钟走完，被判超时负")
    ),
    p(
      "我们对给您造成的不便致歉，", br,
      "并且祝您在haichess.com有愉快的比赛体验。", br,
      "感谢阅读！"
    )
  )

  def currentGameInfo(current: lila.app.mashup.Preload.CurrentGame)(implicit ctx: Context) = nopeInfo(
    h1("稍等！"),
    p("您和 ", strong(current.opponent), " 有一盘对局正在进行中"),
    br, br,
    a(cls := "text button button-fat", dataIcon := "G", href := routes.Round.player(current.pov.fullId))("继续比赛"),
    br, br,
    "or",
    br, br,
    postForm(action := routes.Round.resign(current.pov.fullId))(
      button(cls := "text button button-red", dataIcon := "L")(
        if (current.pov.game.abortable) "终止对局" else "认输"
      )
    ),
    br,
    p("当前对局结束前，您不能开始新的比赛。")
  )

  def nopeInfo(content: Modifier*) = frag(
    div(cls := "lobby__app"),
    div(cls := "lobby__nope")(
      st.section(cls := "lobby__app__content")(content)
    )
  )

  def spotlight(e: lila.event.Event)(implicit ctx: Context) = a(
    href := (if (e.isNow) e.url else routes.Event.show(e.id).url),
    cls := List(
      s"tour-spotlight event-spotlight id_${e.id}" -> true,
      "invert" -> e.isNowOrSoon
    )
  )(
      i(cls := "img", dataIcon := ""),
      span(cls := "content")(
        span(cls := "name")(e.title),
        span(cls := "headline")(e.headline),
        span(cls := "more")(
          if (e.isNow) trans.eventInProgress() else momentFromNow(e.startsAt)
        )
      )
    )
}
