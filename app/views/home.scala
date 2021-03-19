package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import play.api.libs.json.{ JsObject, Json }
import lila.game.Pov
import lila.contest.Contest
import lila.puzzle.PuzzleRush
import lila.study.Study
import lila.user.User
import org.joda.time.DateTime
import controllers.routes

object home {

  private val dataTab = attr("data-tab")

  def apply(
    data: JsObject,
    feat: Option[lila.game.Game],
    leader: List[lila.user.User.LightPerf],
    rushs: List[(PuzzleRush.Mode, List[(User.ID, Int)])],
    contests: List[(Contest, User.ID)],
    studys: List[Study],
    puzzle: Option[lila.puzzle.DailyPuzzle],
    lastThemePuzzleId: Int
  )(implicit ctx: Context) = {
    val rating = ctx.me.map(_.perfs.puzzle.intRating)
    val minRating = "600" /*rating.fold("") { r => Math.max(600, (r - 200)).toString }*/
    val maxRating = "2800" /*rating.fold("") { r => Math.min(2800, (r + 200)).toString }*/
    views.html.base.layout(
      title = "Home",
      moreCss = cssTag("home"),
      moreJs = frag(
        unsliderTag,
        jsAt(s"compiled/lichess.home${isProd ?? (".min")}.js"),
        embedJsUnsafe(s"""lichess=lichess||{};lichess.home=${
          safeJsonValue(Json.obj(
            "data" -> data,
            "userId" -> ctx.userId
          ))
        }""")
      )
    ) {
        main(cls := "home")(
          ctx.isAnon option div(cls := "banner")(
            ul(cls := "items")(
              List("1", "2", "3", "4").map { image =>
                li(img(src := staticUrl(s"images/banner/$image.jpg")))
              }
            )
          ),
          ctx.isAuth option div(cls := "home-header-warp")(
            div(cls := "home-header")(
              div(cls := "home-header-content")
            )
          ),
          div(cls := "home-body")(
            div(cls := "components")(
              div(cls := "home-box home__daily")(
                div(cls := "box-header")(
                  span(cls := "title text", dataIcon := "-")("每日一题"),
                  a(cls := "more", href := routes.Puzzle.home)("更多", " »")
                ),
                div(cls := "box-body")(
                  puzzle.map { p =>
                    raw(p.html)
                  }
                )
              ),
              div(cls := "home-box home__rush")(
                div(cls := "box-header")(
                  span(cls := "title text", dataIcon := "冲")("战术冲刺 ", DateTime.now.toString("yyyy年MM月")),
                  a(cls := "more", href := routes.PuzzleRush.show)("更多", " »")
                ),
                div(cls := "box-body")(
                  div(cls := "tabs")(
                    div(cls := "header")(
                      div(dataTab := "threeMinutes", cls := "active")(span("3分钟")),
                      div(dataTab := "fiveMinutes")(span("5分钟")),
                      div(dataTab := "survival")(span("生存"))
                    ),
                    div(cls := "panels")(
                      rushs.zipWithIndex.map {
                        case ((m, list), i) => {
                          div(cls := List(m.id -> true, "active" -> (i == 0)))(
                            table(
                              tbody(
                                list.zipWithIndex.map {
                                  case (rush, j) => {
                                    tr(
                                      td(rank(j)),
                                      td(userIdLink(rush._1.some)),
                                      td(cls := "text")(rush._2)
                                    )
                                  }
                                }
                              )
                            )
                          )
                        }
                      }
                    ),
                    a(cls := "btn-rush", href := s"${routes.PuzzleRush.show}?mode=threeMinutes&auto=true")("去挑战")
                  )
                )
              ),
              div(cls := "home-box home__theme")(
                div(cls := "box-header")(
                  span(cls := "title text", dataIcon := "题")("主题战术"),
                  a(cls := "more", href := routes.Puzzle.themePuzzleHome)("更多", " »")
                ),
                div(cls := "box-body")(
                  div(cls := "container")(
                    ul(
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=QianZi&ratingMin=$minRating&ratingMax=$maxRating")("牵制")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=ZhuoShuang&ratingMin=$minRating&ratingMax=$maxRating")("捉双")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=ShuangChongGongJi&ratingMin=$minRating&ratingMax=$maxRating")("双重攻击"))
                    ),
                    ul(
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=ShanJi&ratingMin=$minRating&ratingMax=$maxRating")("闪击")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=ChuanJi&ratingMin=$minRating&ratingMax=$maxRating")("串击"))
                    ),
                    ul(
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=ShuangJiang&ratingMin=$minRating&ratingMax=$maxRating")("双将")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=YinRu&ratingMin=$minRating&ratingMax=$maxRating")("引入")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=YinLi&ratingMin=$minRating&ratingMax=$maxRating")("引离"))
                    ),
                    ul(
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=TouShi&ratingMin=$minRating&ratingMax=$maxRating")("透视")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=LanJie&ratingMin=$minRating&ratingMax=$maxRating")("拦截"))
                    ),
                    ul(
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=TengNuo&ratingMin=$minRating&ratingMax=$maxRating")("腾挪")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=DuSe&ratingMin=$minRating&ratingMax=$maxRating")("堵塞")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=QuGan&ratingMin=$minRating&ratingMax=$maxRating")("驱赶"))
                    ),
                    ul(
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=GuoMen&ratingMin=$minRating&ratingMax=$maxRating")("过门")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=XiaoChuBaoHu&ratingMin=$minRating&ratingMax=$maxRating")("消除保护"))
                    ),
                    ul(
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=BiZouLieZhao&ratingMin=$minRating&ratingMax=$maxRating")("逼走劣着")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=GuoDuZhao&ratingMin=$minRating&ratingMax=$maxRating")("过渡着")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=WeiKun&ratingMin=$minRating&ratingMax=$maxRating")("围困"))
                    ),
                    ul(
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=FangYu&ratingMin=$minRating&ratingMax=$maxRating")("防御")),
                      li(a(href := s"${routes.Puzzle.themePuzzle(lastThemePuzzleId, false, true)}&subject[0]=MenSha&ratingMin=$minRating&ratingMax=$maxRating")("闷杀"))
                    )
                  )
                )
              ),
              div(cls := "home-box home__leaderboard")(
                div(cls := "box-header")(
                  span(cls := "title text", dataIcon := "C")("排行榜"),
                  a(cls := "more", href := routes.User.list)("更多", " »")
                ),
                div(cls := "box-body")(
                  div(cls := "content-wrap")(
                    table(
                      tbody(
                        leader map { l =>
                          tr(
                            lila.rating.PerfType(l.perfKey) map { pt =>
                              td(cls := "text", dataIcon := pt.iconChar, title := pt.name)
                            },
                            td(lightUserLink(l.user)),
                            lila.rating.PerfType(l.perfKey) map { pt =>
                              td(cls := "text")(l.rating)
                            },
                            td(ratingProgress(l.progress))
                          )
                        }
                      )
                    )
                  )
                )
              ),
              div(cls := "home-box home__tv")(
                div(cls := "box-header")(
                  span(cls := "title text", dataIcon := "1")("当前对局"),
                  a(cls := "more", href := routes.Tv.games.toString)("更多", " »")
                ),
                div(cls := "box-body")(
                  feat map { g =>
                    /*div(cls := "feat")(*/
                    gameFen(Pov first g, tv = !g.finished) /*,
                        views.html.game.bits.vstext(Pov first g)(ctx.some)*/
                    /*)*/
                  }
                )
              ),
              div(cls := "home-box home__start")(
                div(cls := "box-header")(
                  span(cls := "title text", dataIcon := "兵")("对战")
                ),
                div(cls := "box-body")(
                  gameButton(a(dataIcon := "厅", href := routes.Lobby.home)("大厅")),
                  gameButton(a(dataIcon := "屏", href := s"${routes.Lobby.home}#ai")("与电脑下棋")),
                  gameButton(a(dataIcon := "赛", href := routes.Contest.home)("比赛"))
                )
              ),
              div(cls := "home-box home__contest")(
                div(cls := "box-header")(
                  span(cls := "title text", dataIcon := "赛")("冠军榜"),
                  a(cls := "more", href := routes.Contest.home)("更多", " »")
                ),
                div(cls := "box-body")(
                  div(cls := "content-wrap")(
                    table(
                      tbody(
                        contests map {
                          case (c, u) => {
                            tr(
                              td(userIdLink(u.some)),
                              td(cls := "link")(
                                a(href := routes.Contest.show(c.id))(c.fullName)
                              ),
                              td(c.createdAt.toString("yyyy/MM/dd HH:mm"))
                            )
                          }
                        }
                      )
                    )
                  )
                )
              ),
              div(cls := "home-box home__study")(
                div(cls := "box-header")(
                  span(cls := "title text", dataIcon := "4")("研习"),
                  a(cls := "more", href := routes.Study.allDefault(1))("更多", " »")
                ),
                div(cls := "box-body")(
                  div(cls := "content-wrap")(
                    table(
                      tbody(
                        table(
                          tbody(
                            studys map { study =>
                              tr(
                                td(cls := "link")(
                                  a(href := routes.Study.show(study.id.value))(study.name)
                                ),
                                td(cls := "text")(study.likes.value)
                              )
                            }
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      }
  }

  private def rank(r: Int) = {
    if (r == 0) {
      i(cls := "one")("一")
    } else if (r == 1) {
      i(cls := "two")("二")
    } else if (r == 2) {
      i(cls := "three")("三")
    } else span((r + 1).toString)
  }

  private def gameButton(frag: Frag) = div(cls := "gbt")(
    span(cls := "rect-border lt"),
    span(cls := "rect-border lb"),
    span(cls := "rect-border rt"),
    span(cls := "rect-border rb"),
    frag
  )

}
