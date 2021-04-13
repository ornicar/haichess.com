package views.html.clazz.homework

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.{ Clazz, Course }
import org.joda.time.DateTime
import play.api.data.Form
import lila.clazz.Homework
import lila.clazz.HomeworkCommon
import lila.clazz.HomeworkCommon._
import lila.clazz.HomeworkPractice
import lila.clazz.HomeworkForm
import lila.app.ui.EmbedConfig
import controllers.routes

object form {

  private val dataLastmove = attr("data-lastmove")
  private val moveTag = tag("move")
  private val indexTag = tag("index")

  def apply(form: Form[_], homework: Homework, clazz: Clazz, course: Course, nextCourse: Option[Course])(implicit ctx: Context) =
    views.html.base.layout(
      title = "课后练",
      moreJs = frag(
        flatpickrTag,
        delayFlatpickrStart,
        jsTag("clazz.homework.form.js")
      ),
      moreCss = cssTag("homework")
    ) {
        main(cls := "box box-pad page-small homework-form")(
          div(cls := "box__top")(
            h1("课后练")
          ),
          table(cls := "course-info")(
            tbody(
              tr(
                td(
                  label("班级名称："),
                  a(href := routes.Clazz.detail(clazz.id))(strong(clazz.name))
                ),
                td(
                  label("课节："),
                  strong(course.index)
                ),
                td(
                  label("上课时间："),
                  strong(course.courseFormatTime)
                )
              )
            )
          ),
          postForm(cls := "form3", action := routes.Homework.updateOrPublish(homework.id))(
            form3.hidden("courseName", s"${clazz.name} 第${course.index}节"),
            form3.split(
              form3.group(form("deadlineAt"), raw("截止时间"))(f =>
                form3.input3(f, vl = (nextCourse.fold(course.dateTime.plusWeeks(1)) { c => c.dateTime }).toString("yyyy-MM-dd HH:mm").some, klass = "flatpickr")(
                  dataEnableTime := true,
                  datatime24h := true
                )(dataMinDate := DateTime.now.toString("yyyy-MM-dd HH:mm")))
            ),
            form3.split(
              form3.group(form("summary"), raw("课节总结"), klass = "form-half")(form3.textarea(_)(rows := 5)),
              form3.group(form("prepare"), raw("预习内容"), klass = "form-half")(form3.textarea(_)(rows := 5))
            ),
            div(cls := "part-name")("公共目标"),
            form3.group(form("common"), frag("项目", nbsp, homework.isCreated option a(cls := "btn-item-add modal-alert", href := routes.Homework.itemModal(homework.itemIds))("添加")))(f =>
              div(cls := "block items")(
                span(cls := "title")("公共目标"),
                table(
                  tbody(
                    (0 to HomeworkCommon.maxItem - 1).map { i =>
                      val itemField = f(s"[$i]")
                      itemField("item").value.map { itemId =>
                        val commonItem = HomeworkCommonItem.apply(itemId)
                        tr(
                          td(cls := "item-name")(commonItem.name),
                          td(cls := "bit")(!commonItem.number option strong("+")),
                          td(
                            form3.input(itemField("num"), typ = "number", klass = (if (itemField("num").hasErrors) "invalid" else "")),
                            form3.hidden(itemField("item"))
                          ),
                          td(
                            homework.isCreated option a(cls := "remove")("移除")
                          )
                        )
                      }
                    }
                  )
                )
              )),
            div(cls := "part-name")("练习"),
            form3.group(form("practice"), frag())(practiceField =>
              frag(
                form3.group(practiceField("capsules"), frag("战术题", nbsp, homework.isCreated option a(cls := "btn-capsule-add modal-alert", href := routes.Capsule.mineOfHomework)("添加")))(f =>
                  div(cls := "block capsules")(
                    span(cls := "title")("战术题"),
                    (0 to HomeworkPractice.maxCapsule - 1).map { i =>
                      val capsuleField = f(s"[$i]")
                      val puzzles = capsuleField("puzzles")
                      capsuleField("name").value.map { name =>
                        div(cls := "capsule")(
                          div(cls := "capsule-head")(
                            label(name),
                            homework.isCreated option a(cls := "remove")("移除"),
                            form3.hidden(capsuleField("id")),
                            form3.hidden(capsuleField("name"))
                          ),
                          div(cls := "capsule-puzzles")(
                            (0 to HomeworkPractice.maxPuzzles - 1).map { j =>
                              val puzzleField = puzzles(s"[$j]")
                              puzzleField("id").value.map { name =>
                                div(cls := "puzzle")(
                                  span(
                                    cls := "mini-board cg-wrap parse-fen is2d",
                                    dataColor := puzzleField("color").value,
                                    dataFen := puzzleField("fen").value,
                                    dataLastmove := puzzleField("lastMove").value
                                  )(cgWrapContent),
                                  form3.hidden(puzzleField("id")),
                                  form3.hidden(puzzleField("color")),
                                  form3.hidden(puzzleField("fen")),
                                  form3.hidden(puzzleField("lastMove")),
                                  form3.hidden(puzzleField("lines"))
                                )
                              }
                            }
                          )
                        )
                      }
                    }
                  )),
                form3.group(practiceField("replayGames"), frag("打谱", nbsp, homework.isCreated option a(cls := "btn-replay-add modal-alert", href := routes.Homework.replayGameModal)("添加")))(f =>
                  div(cls := "block replayGames")(
                    span(cls := "title")("打谱"),
                    table(
                      tbody(
                        (0 to HomeworkPractice.maxReplayGame - 1).map { i =>
                          val replayGameField = f(s"[$i]")
                          val movesField = replayGameField("moves")
                          replayGameField("name").value.map { name =>
                            tr(
                              td(cls := "td-board")(
                                span(
                                  cls := "mini-board cg-wrap parse-fen is2d",
                                  dataFen := replayGameField("root").value,
                                  dataColor := "white"
                                )(cgWrapContent)
                              ),
                              td(
                                a(href := replayGameField("chapterLink").value, target := "_blank")(
                                  strong(replayGameField("name").value)
                                ),
                                div(cls := "moves")(
                                  (0 to 500).map { j =>
                                    val moveField = movesField(s"[$j]")
                                    moveField("index").value.map { index =>
                                      moveTag(
                                        indexTag(index, "."),
                                        form3.hidden(moveField("index")),
                                        moveField("white.san").value.map { san =>
                                          span(dataFen := moveField("white.fen").value)(
                                            san,
                                            form3.hidden(moveField("white.san")),
                                            form3.hidden(moveField("white.uci")),
                                            form3.hidden(moveField("white.fen"))
                                          )
                                        } getOrElse span(cls := "disabled")("..."),
                                        moveField("black.san").value.map { san =>
                                          span(dataFen := moveField("black.fen").value)(
                                            san,
                                            form3.hidden(moveField("black.san")),
                                            form3.hidden(moveField("black.uci")),
                                            form3.hidden(moveField("black.fen"))
                                          )
                                        } getOrElse span(cls := "disabled")
                                      )
                                    }
                                  }
                                )
                              ),
                              td(
                                homework.isCreated option a(cls := "remove")("移除"),
                                form3.hidden(replayGameField("chapterLink")),
                                form3.hidden(replayGameField("name")),
                                form3.hidden(replayGameField("root"))
                              )
                            )
                          }
                        }
                      )
                    )
                  )),
                form3.group(practiceField("recallGames"), frag("记谱", nbsp, homework.isCreated option a(cls := "btn-recall-add modal-alert", href := routes.Homework.recallModal)("添加")))(f =>
                  div(cls := "block recallGames")(
                    span(cls := "title")("记谱"),
                    table(
                      tbody(
                        (0 to HomeworkPractice.maxRecallGame - 1).map { i =>
                          val recallGameField = f(s"[$i]")
                          recallGameField("pgn").value.map { pgn =>
                            tr(
                              td(cls := "td-board")(
                                span(
                                  cls := "mini-board cg-wrap parse-fen is2d",
                                  dataFen := recallGameField("root").value,
                                  dataColor := "white"
                                )(cgWrapContent)
                              ),
                              td(
                                //a(href := routes.Recall.showOfMate(pgn))("打开"),
                                div(cls := "meta")(
                                  span("棋色：", recallGameField("color").value.map { c => if (c == "white") "白方" else "黑方" } | "双方"),
                                  nbsp, nbsp,
                                  span("回合数：", recallGameField("turns").value | "所有")
                                ),
                                div(cls := "pgn")(
                                  strong(
                                    (recallGameField("pgn").value | "").split("\r\n\r\n")(1)
                                  )
                                )
                              ),
                              td(
                                homework.isCreated option a(cls := "remove")("移除"),
                                form3.textarea(recallGameField("pgn"), klass = "none")(),
                                form3.hidden(recallGameField("root")),
                                form3.hidden(recallGameField("turns")),
                                form3.hidden(recallGameField("color")),
                                form3.hidden(recallGameField("title"))
                              )
                            )
                          }
                        }
                      )
                    )
                  )),
                form3.group(practiceField("fromPositions"), frag("指定起始位置对局", nbsp, homework.isCreated option a(cls := "btn-position-add")("添加")))(f =>
                  div(cls := "block fromPositions")(
                    span(cls := "title")("起始位置"),
                    table(cls := "tb")(
                      tbody(
                        (0 to HomeworkPractice.maxFromPosition - 1).map { i =>
                          val fromPositionField = f(s"[$i]")
                          fromPositionField("fen").value.map { fen =>
                            tr(
                              td(cls := "td-board")(
                                span(
                                  cls := "mini-board cg-wrap parse-fen is2d",
                                  dataFen := fen,
                                  dataColor := "white"
                                )(cgWrapContent)
                              ),
                              td(
                                table(cls := "form")(
                                  tbody(
                                    tr(
                                      td("FEN"),
                                      td(form3.input(fromPositionField("fen"), klass = (if (fromPositionField("fen").hasErrors) "invalid" else "")))
                                    ),
                                    tr(
                                      td("初始时间"),
                                      td(form3.select(fromPositionField("clockTime"), HomeworkForm.clockTimeChoices, klass = (if (fromPositionField("clockTime").hasErrors) "invalid" else "")))
                                    ),
                                    tr(
                                      td("时间增量"),
                                      td(form3.select(fromPositionField("clockIncrement"), HomeworkForm.clockIncrementChoices, klass = (if (fromPositionField("clockIncrement").hasErrors) "invalid" else "")))
                                    ),
                                    tr(
                                      td("对局数"),
                                      td(form3.input(fromPositionField("num"), typ = "number", klass = (if (fromPositionField("num").hasErrors) "invalid" else "")))
                                    )
                                  )
                                )
                              ),
                              td(
                                homework.isCreated option a(cls := "remove")("移除")
                              )
                            )
                          }
                        }
                      )
                    )
                  ))
              )),
            globalError(form),
            form3.actions(
              a(cls := "goBack")("取消"),
              button(
                cls := List("button button-green confirm" -> true, "disabled" -> (homework.isPublished || course.dateTime.isAfterNow)),
                (homework.isPublished || course.dateTime.isAfterNow) option disabled,
                title := (if (course.dateTime.isAfterNow) "课程开始后您可以发布" else "发布后将不可修改，是否继续？"),
                name := "method", value := "publish"
              )(if (homework.isPublished) "已发布" else "保存并发布"),
              button(cls := List("button" -> true, "disabled" -> homework.isPublished), homework.isPublished option disabled, name := "method", value := "update")("保存")
            )
          )
        )
      }

}
