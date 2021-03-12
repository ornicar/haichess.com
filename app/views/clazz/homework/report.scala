package views.html.clazz.homework

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.{ Clazz, Course, Homework, HomeworkReport, HomeworkStudent }
import play.api.libs.json._
import lila.user.User
import controllers.routes

object report {

  private val dataLastmove = attr("data-lastmove")
  private val moveTag = tag("move")
  private val indexTag = tag("index")
  private val dataAttr = attr("data-attr")
  private val dataXAxis = attr("data-xaxis")
  private val dataSeries = attr("data-series")
  private val dataAvailable = attr("data-available")
  private val dataUpdateAt = attr("data-updateat")

  def apply(homework: Homework, homeworkReport: Option[HomeworkReport], users: List[User], clazz: Clazz, course: Course)(implicit ctx: Context) =
    views.html.base.layout(
      title = "课后练报告",
      moreJs = frag(
        echartsTag,
        echartsThemeTag,
        jsTag("clazz.homework.report.js")
      ),
      moreCss = cssTag("homework")
    ) {
        main(cls := "box box-pad page-small homework-report", dataUpdateAt := homeworkReport.map(_.updateAt.getMillis) | 0, dataAvailable := homework.available, dataId := homework.id)(
          div(cls := "box__top")(
            h1("课后练报告"),
            div(cls := "uptime")(
              span("更新时间："),
              homeworkReport.map { hr =>
                span(hr.updateAt.toString("yyyy-MM-dd HH:mm"))
              } | span("未更新"),
              nbsp, nbsp,
              homework.available option a(href := routes.Homework.refreshReport(homework.id))("更新")
            )
          ),
          table(cls := "course-info")(
            tbody(
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
                  strong(homework.deadlineAt.map(_.toString("yyyy-MM-dd HH:mm")))
                )
              )
            )
          ),
          div(cls := "report")(
            div(cls := "part common")(
              h3(cls := "part-title")("公共目标"),
              table(cls := "slist")(
                thead(
                  tr(
                    th("学员"),
                    th("姓名"),
                    homework.common.map { common =>
                      common.itemList.map { item =>
                        th(item.name)
                      }
                    },
                    th
                  )
                ),
                tbody(
                  homeworkReport.map { hr =>
                    hr.common.map {
                      case (u, list) => tr(
                        td(u),
                        td(users.find(_.id == u).map(_.profileOrDefault.nonEmptyRealName | "-")),
                        list.map { com =>
                          td(
                            span(cls := List("complete" -> com.isComplete, "uncomplete" -> !com.isComplete))(com.currentResult | 0),
                            "/",
                            com.num
                          )
                        },
                        td(
                          a(target := "_blank", href := routes.Homework.show(HomeworkStudent.makeId(homework.id, u)), title := "查看详情")("详情")
                        )
                      )
                    }.toList
                  }
                )
              )
            ),
            div(cls := "part practice")(
              h3(cls := "part-title")("练习"),
              div(cls := "practice-part")(
                h3(cls := "practice-title")("战术题"),
                homeworkReport.map { hr =>
                  val xAxis = JsArray(hr.practice.puzzles.zipWithIndex.map {
                    case (_, i) => JsNumber((i + 1))
                  })

                  val series = JsArray(
                    List(
                      Json.obj(
                        "name" -> "完成率",
                        "type" -> "line",
                        "data" -> JsArray(hr.practice.puzzles.map { pz =>
                          JsNumber(pz.report.completeRate)
                        })
                      ),
                      Json.obj(
                        "name" -> "正确率",
                        "type" -> "line",
                        "data" -> JsArray(hr.practice.puzzles.map { pz =>
                          JsNumber(pz.report.rightRate)
                        })
                      ),
                      Json.obj(
                        "name" -> "首次正确率",
                        "type" -> "line",
                        "data" -> JsArray(hr.practice.puzzles.map { pz =>
                          JsNumber(pz.report.firstMoveRightRate)
                        })
                      )
                    )
                  )
                  div(cls := "puzzle-all-chart", dataXAxis := xAxis.toString, dataSeries := series.toString)
                },
                ul(cls := "puzzles")(
                  homeworkReport.map { hr =>
                    hr.practice.puzzles.zipWithIndex.map {
                      case (pz, i) => {
                        val puzzle = pz.puzzle
                        val report = pz.report
                        li(cls := List("none" -> (i > 0)))(
                          table(
                            tr(
                              td(cls := "td-chart")(
                                div(cls := "puzzle-chart", dataSeries := s"""[${report.completeRate}, ${report.rightRate}, ${report.firstMoveRightRate}]""")
                              ),
                              td(cls := "td-board")(
                                a(
                                  cls := "mini-board cg-wrap parse-fen is2d",
                                  dataColor := puzzle.color.name,
                                  dataFen := puzzle.fen,
                                  dataLastmove := puzzle.lastMove,
                                  target := "_blank",
                                  href := routes.Puzzle.show(puzzle.id)
                                )(cgWrapContent),
                                div(cls := "controls")(
                                  button(cls := List("button button-empty prev" -> true, "disabled" -> (i == 0)))("上一题"),
                                  span("第", strong(i + 1), "题"),
                                  button(cls := List("button button-empty next" -> true, "disabled" -> (i == hr.practice.puzzles.size - 1)))("下一题")
                                )
                              ),
                              td(
                                div(cls := "first-move")(
                                  label("第一步正确走法"),
                                  ul(cls := "right")(
                                    report.rightMoveDistribute.map { mn =>
                                      li(mn.move, nbsp, nbsp, mn.num)
                                    }
                                  )
                                ),
                                div(cls := "first-move")(
                                  label("第一步其他走法"),
                                  ul(cls := "wrong")(
                                    report.wrongMoveDistribute.take(6).map { mn =>
                                      li(mn.move, nbsp, nbsp, mn.num)
                                    }
                                  )
                                )
                              )
                            )
                          )
                        )
                      }
                    }
                  }
                )
              ),
              div(cls := "practice-part")(
                h3(cls := "practice-title")("打谱"),
                div(cls := "replayGames")(
                  table(
                    tbody(
                      homeworkReport.map { hr =>
                        hr.practice.replayGames.map { rg =>
                          val replayGame = rg.replayGame
                          val report = rg.report
                          tr(
                            td(cls := "td-chart")(
                              div(cls := "replayGame-chart", dataSeries := s"""[{"name":"完成","value":${report.complete}}, {"name":"未完成","value":${hr.num - report.complete}}]""")
                            ),
                            td(cls := "td-board")(
                              a(
                                cls := "mini-board cg-wrap parse-fen is2d",
                                dataColor := "white",
                                dataFen := replayGame.root,
                                target := "_blank",
                                href := replayGame.chapterLink
                              )(cgWrapContent)
                            ),
                            td(
                              div(
                                a(href := replayGame.chapterLink, target := "_blank")(
                                  strong(replayGame.name)
                                )
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
                      }
                    )
                  )
                )
              ),
              div(cls := "practice-part")(
                h3(cls := "practice-title")("记谱"),
                div(cls := "recallGames")(
                  table(
                    tbody(
                      homeworkReport.map { hr =>
                        hr.practice.recallGames.map { rg =>
                          val recallGame = rg.recallGame
                          val report = rg.report
                          tr(
                            td(cls := "td-chart")(
                              div(cls := "recallGame-chart", dataXAxis := JsArray(report.map(r => JsNumber(r.turns))).toString, dataSeries := JsArray(report.map(r => JsNumber(r.num))).toString)
                            ),
                            td(cls := "td-board")(
                              span(
                                cls := "mini-board cg-wrap parse-fen is2d",
                                dataColor := "white",
                                dataFen := recallGame.root,
                                target := "_blank"
                              )(cgWrapContent)
                            ),
                            td(
                              div(
                                div(
                                  span("棋色：", recallGame.color.map { c => if (c.name == "white") "白方" else "黑方" } | "双方"),
                                  nbsp, nbsp,
                                  span("回合数：", recallGame.turns.map(_.toString) | "所有")
                                ),
                                div(cls := "pgn")(
                                  lila.common.String.html.richText(recallGame.pgn)
                                )
                              )
                            )
                          )
                        }
                      }
                    )
                  )
                )
              ),
              div(cls := "practice-part")(
                h3(cls := "practice-title")("指定起始位置对局"),
                div(cls := "fromPositions")(
                  table(
                    tbody(
                      homeworkReport.map { hr =>
                        hr.practice.fromPositions.map { fp =>
                          val fromPosition = fp.fromPosition
                          val report = fp.report
                          tr(
                            td(cls := "td-chart")(
                              div(cls := "fromPosition-chart", dataXAxis := JsArray(report.map(r => JsNumber(r.rounds))).toString, dataSeries := JsArray(report.map(r => JsNumber(r.num))).toString)
                            ),
                            td(cls := "td-board")(
                              span(
                                cls := "mini-board cg-wrap parse-fen is2d",
                                dataColor := "white",
                                dataFen := fromPosition.fen,
                                target := "_blank"
                              )(cgWrapContent)
                            ),
                            td(
                              span("对局数：", fromPosition.num, "，时钟：", fromPosition.clock.show)
                            )
                          )
                        }
                      }
                    )
                  )
                )
              )
            )
          )
        )
      }

}
