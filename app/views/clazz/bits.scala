package views.html.clazz

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.Clazz.ClazzWithCoach
import lila.team.Team
import lila.clazz.Clazz
import controllers.routes

object bits {

  def clazzInfo(clazzWithCoach: ClazzWithCoach, team: Option[Team])(implicit ctx: Context) = {
    val clazz = clazzWithCoach.clazz
    div(cls := "basic")(
      div(cls := "header")(
        div(cls := "head", style := "background-color:" + clazz.color),
        div(cls := "name")(
          h2(clazz.name),
          team.map { t =>
            div(
              a(href := routes.Team.show(t.id))(t.name)
            )
          }
        )
      ),
      div(cls := "info")(
        table(
          tr(
            th("教练"),
            td(
              a(href := routes.Coach.showById(clazz.coach))(clazzWithCoach.coach.realNameOrUsername)
            )
          ),
          tr(
            th("学员数量"),
            td(clazz.studentCount)
          ),
          tr(
            th("课节数"),
            td(
              span(showCourseTime(clazz))
            )
          ),
          clazz.weekClazz.map { wc =>
            tr(
              th("开始日期"),
              td(wc.dateStart.toString("M月d日"))
            )
          },
          tr(
            th("上课时间"),
            td(
              showCourseList(clazz)
            )
          )
        )
      )
    )
  }

  def showCourseTime(clazz: Clazz) =
    clazz.clazzType match {
      case Clazz.ClazzType.Week => clazz.weekClazz.fold(0)(_.times)
      case Clazz.ClazzType.Train => clazz.trainClazz.fold(0)(_.times)
    }

  def showCourseList(clazz: Clazz) =
    clazz.clazzType match {
      case Clazz.ClazzType.Week => {
        clazz.weekClazz.map { wc =>
          ul(cls := "course")(
            wc.weekCourse.map { c =>
              li(c.toString)
            }
          )
        }
      }
      case Clazz.ClazzType.Train => {
        clazz.trainClazz.map { tc =>
          ul(cls := "course")(
            tc.trainCourse.map { c =>
              li(c.toString)
            }
          )
        }
      }
    }

}
