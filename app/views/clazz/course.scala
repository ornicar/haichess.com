package views.html.clazz

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.{ Clazz, Course }
import org.joda.time.{ DateTime, Period, PeriodType }
import controllers.routes

object course {

  def apply(firstDay: DateTime, lastDay: DateTime, courseMap: Map[(String, String), List[Course.WithClazz]], week: Int)(implicit ctx: Context) = {
    val dataClazz = attr("data-clazz")
    val timeRange = List("上午", "下午", "晚上")
    val dateRange: Seq[String] = (0 to new Period(firstDay.getMillis, lastDay.getMillis, PeriodType.days()).getDays) map { i =>
      firstDay.plusDays(i).toString("M月d日")
    }

    views.html.base.layout(
      title = "课表",
      moreJs = frag(
        jsTag("clazz.course.js"),
        flatpickrTag
      ),
      moreCss = cssTag("course")
    ) {
        main(cls := "box page-small timetable")(
          div(cls := "box__top")(
            div(cls := "btn-group")(
              a(cls := "button prev", href := routes.Course.timetable(week - 1))("上一周"),
              a(cls := "button next", href := routes.Course.timetable(week + 1))("下一周"),
              a(cls := "button today", href := routes.Course.timetable(0))("今天")
            ),
            div(cls := "btn-group action")(
              button(cls := "button button-empty action-update disabled", disabled, href := routes.Course.updateModal("#id#", week))("修改课时"),
              button(cls := "button button-empty action-stop disabled", disabled, href := routes.Course.stopModal("#id#", week))("停课一次"),
              button(cls := "button button-empty action-postpone disabled", disabled, href := routes.Course.postponeModal("#id#", week))("顺延")
            )
          ),
          table(cls := "course-list")(
            thead(
              tr(
                th(rowspan := 2),
                th("周一"),
                th("周二"),
                th("周三"),
                th("周四"),
                th("周五"),
                th("周六"),
                th("周日")
              ),
              tr(
                dateRange map { d =>
                  th(d)
                }
              )
            ),
            tbody(
              timeRange map { t =>
                tr(
                  td(t),
                  frag(
                    dateRange map { d =>
                      courseMap.get(d, t) map { list =>
                        td(
                          list map { courseWithClazz =>
                            courseInfo(courseWithClazz.clazz, courseWithClazz.course)
                          }
                        )
                      } getOrElse td
                    }
                  )
                )
              }
            )
          )
        )
      }
  }

  val dataClazz = attr("data-clazz")
  val dataIndex = attr("data-index")
  val dataHomework = attr("data-homework")
  private def courseInfo(clazz: Clazz, course: Course) =
    div(
      cls := List("course" -> true, "stopped" -> course.stopped, "editable" -> (course.editable && !course.stopped)),
      dataId := course.id,
      dataIndex := course.index,
      dataHomework := course.homework,
      dataClazz := clazz.id,
      dataType := clazz.clazzType.id,
      style := "color:" + clazz.color
    )(
        div(cls := "nowrap")(course.timeBegin, "-", course.timeEnd),
        div(cls := "nowrap ellipsis", title := clazz.name)(clazz.name),
        div(cls := "nowrap")("第" + course.index + "/" + clazz.times + "节")
      )

}
