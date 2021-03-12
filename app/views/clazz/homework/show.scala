package views.html.clazz.homework

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.{ Clazz, Course, HomeworkStudent }
import lila.common.String.html.richText
import lila.user.User
import controllers.routes

object show {

  private val dataLastmove = attr("data-lastmove")
  private val moveTag = tag("move")
  private val indexTag = tag("index")

  def apply(homework: HomeworkStudent, clazz: Clazz, course: Course, coach: Option[User])(implicit ctx: Context) =
    views.html.base.layout(
      title = "课后练",
      moreJs = frag(
        jsTag("clazz.homework.show.js")
      ),
      moreCss = cssTag("homework")
    ) {
        main(cls := "box box-pad page-small homework-show", dataId := homework.id)(
          div(cls := "box__top")(
            h1("课后练"),
            div(cls := "box__top__actions")(
              "完成进度：",
              homework.finishRate
            )
          ),
          table(cls := "course-info")(
            tbody(
              tr(
                coach.map { c =>
                  td(
                    label("教练："),
                    a(href := routes.Coach.showById(c.id))(strong(c.realNameOrUsername))
                  )
                },
                td
              ),
              tr(
                td(
                  label("班级名称："),
                  a(href := routes.Clazz.detail(clazz.id))(strong(clazz.name))
                ),
                td(
                  label("上课时间："),
                  strong(course.courseFormatTime)
                )
              ),
              tr(
                td(
                  label("课节："),
                  strong(course.index)
                ),
                td(
                  label("截止时间："),
                  strong(homework.deadlineAt.toString("yyyy-MM-dd HH:mm"))
                )
              )
            )
          ),
          div(cls := "info")(
            div(cls := "part split")(
              div(cls := "half")(
                h3(cls := "part-title")("课节总结"),
                div(
                  homework.summary.map(richText(_))
                )
              ),
              div(cls := "half")(
                h3(cls := "part-title")("预习内容"),
                div(
                  homework.prepare.map(richText(_))
                )
              )
            ),
            div(cls := "part common")(
              h3(cls := "part-title")("训练目标"),
              table(cls := "slist")(
                thead(
                  tr(
                    th("练习项目"),
                    th("本次目标"),
                    th("当前"),
                    th("状态")
                  )
                ),
                tbody(
                  homework.common.map { com =>
                    val item = com.item
                    tr(
                      td(item.name),
                      td(com.targetFormat),
                      td(com.currentResult | 0),
                      td(
                        if (com.isComplete) span(cls := "complete")("已完成") else
                          a(cls := "uncomplete", target := "_blank", href := com.link)("未完成")
                      )
                    )
                  }
                )
              )
            ),
            div(cls := "part practice")(
              h3(cls := "part-title")("练习"),
              div(cls := "practice-part")(
                homework.practice.map { practice =>
                  frag(
                    div(cls := "puzzles")(
                      h3(cls := "practice-title")("战术题"),
                      div(cls := "puzzle-list")(
                        practice.puzzles.map { p =>
                          val puzzle = p.puzzle
                          div(cls := "puzzle")(
                            a(
                              cls := "mini-board cg-wrap parse-fen is2d",
                              dataColor := puzzle.color.name,
                              dataFen := puzzle.fen,
                              dataLastmove := puzzle.lastMove,
                              target := "_blank",
                              href := routes.Puzzle.homeworkPuzzle(homework.id, puzzle.id)
                            )(cgWrapContent),
                            div(cls := "result")(
                              if (p.isTry) {
                                a(cls := List("complete" -> p.isComplete, "error" -> !p.isComplete), target := "_blank", href := routes.Puzzle.homeworkPuzzle(homework.id, puzzle.id))(if (p.isComplete) "正确" else "错误")
                              } else a(cls := "uncomplete", target := "_blank", href := routes.Puzzle.homeworkPuzzle(homework.id, puzzle.id))("做题")
                            )
                          )
                        }
                      )
                    ),
                    div(cls := "replayGames")(
                      h3(cls := "practice-title")("打谱"),
                      table(
                        tbody(
                          practice.replayGames.map { r =>
                            val replayGame = r.replayGame
                            tr(
                              td(cls := "td-board")(
                                a(
                                  cls := "mini-board cg-wrap parse-fen is2d",
                                  dataColor := "white",
                                  dataFen := replayGame.root,
                                  target := "_blank",
                                  href := routes.Homework.solveReplayGame(homework.id, replayGame.studyIdFromLink, replayGame.chapterIdFromLink)
                                )(cgWrapContent)
                              ),
                              td(
                                a(cls := List("complete" -> r.result.??(_.win), "uncomplete" -> !r.result.??(_.win)), target := "_blank",
                                  href := routes.Homework.solveReplayGame(homework.id, replayGame.studyIdFromLink, replayGame.chapterIdFromLink))(
                                    strong(replayGame.name, if (r.result.??(_.win)) "（已完成）" else "（未完成）")
                                  ),
                                div(cls := "moves")(
                                  replayGame.moves.map { move =>
                                    moveTag(
                                      indexTag(move.index, "."),
                                      move.white.map { w =>
                                        span(dataFen := w.fen)(w.san)
                                      } getOrElse span(cls := "disabled")("..."),
                                      move.black.map { b =>
                                        span(dataFen := b.fen)(b.san)
                                      } getOrElse span(cls := "disabled")
                                    )
                                  }
                                )
                              )
                            )
                          }
                        )
                      )
                    ),
                    div(cls := "recallGames")(
                      h3(cls := "practice-title")("记谱"),
                      table(
                        tbody(
                          practice.recallGames.map { r =>
                            val recallGame = r.recallGame
                            val link = s"${routes.Recall.showOfMate(recallGame.pgn)}&homeworkId=${recallGame.hashMD5}&color=${recallGame.color.??(_.name)}&turns=${recallGame.turns.??(_.toString)}&title=${recallGame.title | ""}"
                            tr(
                              td(cls := "td-board")(
                                a(
                                  cls := "mini-board cg-wrap parse-fen is2d",
                                  dataColor := recallGame.color.fold("white")(_.name),
                                  dataFen := recallGame.root,
                                  target := "_blank",
                                  href := link
                                )(cgWrapContent)
                              ),
                              td(
                                a(cls := List("complete" -> r.result.??(_.win), "uncomplete" -> !r.result.??(_.win)), target := "_blank", href := link)(
                                  strong(
                                    span("棋色：", recallGame.color.map { c => if (c.name == "white") "白方" else "黑方" } | "双方"),
                                    nbsp, nbsp,
                                    span("回合数：", r.result.fold(0)(_.turns), "/", recallGame.turns.map(_.toString) | "所有"),
                                    if (r.result.??(_.win)) "（已完成）" else "（未完成）"
                                  )
                                ),
                                div(cls := "pgn")(
                                  lila.common.String.html.richText(recallGame.pgn)
                                )
                              )
                            )
                          }
                        )
                      )
                    ),
                    div(cls := "fromPositions")(
                      h3(cls := "practice-title")("指定起始位置对局"),
                      table(
                        tbody(
                          practice.fromPositions.map { f =>
                            val fromPosition = f.fromPosition
                            val result = f.result
                            val link = s"${routes.Lobby.home.toString}?fen=${fromPosition.fen}&limit=${fromPosition.clock.limitSeconds}&increment=${fromPosition.clock.incrementSeconds}#friend"
                            tr(
                              td(cls := "td-board")(
                                a(
                                  cls := "mini-board cg-wrap parse-fen is2d",
                                  dataColor := "white",
                                  dataFen := fromPosition.fen,
                                  target := "_blank",
                                  href := link
                                )(cgWrapContent)
                              ),
                              td(
                                div(
                                  fromPosition.clock.show,
                                  nbsp, nbsp,
                                  label("完成进度："),
                                  strong(
                                    a(cls := List("complete" -> f.isComplete, "uncomplete" -> !f.isComplete), target := "_blank", href := link)(result.fold(0)(_.size)),
                                    span(s"/${fromPosition.num}")
                                  ),
                                  !f.isComplete option a(cls := "uncomplete", target := "_blank", href := link)(h2("未完成"))
                                ),
                                result.map { result =>
                                  ul(cls := "games")(
                                    result.map { g =>
                                      li(
                                        a(target := "_blank", href := routes.Round.watcher(g.gameId, "white"))(
                                          span(g.white),
                                          nbsp,
                                          iconTag("U"),
                                          nbsp,
                                          span(g.black)
                                        )
                                      )
                                    }
                                  )
                                }
                              )
                            )
                          }
                        )
                      )
                    )
                  )
                }
              )
            )
          )
        )
      }

}
