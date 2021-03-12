package controllers

import lila.app.Env
import lila.app._
import lila.api.Context
import play.api.mvc.Result
import org.joda.time.DateTime
import lila.clazz.{ UpdateData, Course => CourseModel }
import views._

object Course extends LilaController {

  private val env = Env.clazz
  private val form = env.courseForm

  def timetable(week: Int) = Secure(_.Coach) { implicit ctx => me =>
    val firstDay = DateTime.now.withDayOfWeek(1).plusWeeks(week)
    val lastDay = DateTime.now.withDayOfWeek(7).plusWeeks(week)
    for {
      courseList <- env.courseApi.weekCourse(firstDay, lastDay, me)
      clazzList <- env.api.byCourseList(courseList)
    } yield {
      val courseMap: Map[(String, String), List[CourseModel.WithClazz]] = courseList.filter(c => clazzList.exists(_.id == c.clazz)).sortBy(_.timeBegin) map { c =>
        CourseModel.WithClazz(c, clazzList.find(c.clazz == _._id).get)
      } groupBy { c =>
        val d = c.course.date.toString("M月d日")
        val t =
          if (c.course.timeBegin < "12:00") "上午"
          else if (c.course.timeBegin >= "12:00" && c.course.timeBegin < "18:00") "下午"
          else if (c.course.timeBegin >= "18:00") "晚上"
          else "-"
        (d, t)
      }
      Ok(html.clazz.course(firstDay, lastDay, courseMap, week))
    }
  }

  def updateModal(id: String, week: Int) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(env.courseApi.byId(id)) { course =>
      Owner(course) {
        Ok(
          html.clazz.modal.course.update(
            course,
            form.updateForm fill UpdateData(course.date, course.timeBegin, course.timeEnd),
            week
          )
        ).fuccess
      }
    }
  }

  def update(id: String, week: Int) = SecureBody(_.Coach) { implicit ctx => me =>
    OptionFuResult(env.courseApi.byId(id)) { course =>
      Owner(course) {
        implicit def req = ctx.body
        form.updateForm.bindFromRequest.fold(
          err => BadRequest(errorsAsJson(err)).fuccess,
          data => env.courseApi.update(course, data) inject jsonOkResult
        )
      }
    }
  }

  def stopModal(id: String, week: Int) = Secure(_.Coach) { implicit ctx => me =>
    Ok(html.clazz.modal.course.stop(id, week)).fuccess
  }

  def stop(id: String, week: Int) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(env.courseApi.byId(id)) { course =>
      Owner(course) {
        env.courseApi.stop(course) inject {
          Redirect(routes.Course.timetable(week))
        }
      }
    }
  }

  def postponeModal(id: String, week: Int) = Secure(_.Coach) { implicit ctx => me =>
    Ok(html.clazz.modal.course.postpone(id, week)).fuccess
  }

  def postpone(id: String, week: Int) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(env.courseApi.byId(id)) { course =>
      Owner(course) {
        env.api.byId(course.clazz) flatMap {
          _.fold(notFound(ctx)) { clazz =>
            env.courseApi.postpone(course, clazz) flatMap { lastCourseTime =>
              env.api.updateWeekCourseLastDate(clazz._id, lastCourseTime) inject {
                Redirect(routes.Course.timetable(week))
              }
            }
          }
        }
      }
    }
  }

  private def Owner(course: CourseModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.??(me => course.isCreator(me.id))) f
    else ForbiddenResult

  private def ForbiddenResult(implicit ctx: Context) = Forbidden(views.html.site.message.authFailed).fuccess

}
