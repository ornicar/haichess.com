package controllers

import lila.api.Context
import lila.app.{ Env, _ }
import lila.clazz.{ Clazz => ClazzModel, Homework => HomeworkModel }
import lila.user.UserRepo
import play.api.mvc.Result
import views._

object Homework extends LilaController {

  private val env = Env.clazz
  private val api = env.api
  private val forms = env.homeworkForm
  private val courseApi = env.courseApi
  private val homeworkApi = env.homeworkApi
  private val stuHomeworkApi = env.homeworkStudentApi
  private val homeworkSolve = env.homeworkSolve
  private val homeworkReport = env.homeworkReport
  private val recallFrom = Env.recall.form

  def createForm(clazzId: String, courseId: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(for {
      clazzOption <- api.byId(clazzId)
      courseOption <- courseApi.byId(courseId)
    } yield (clazzOption |@| courseOption).tupled) {
      case (clazz, course) => {
        Owner(clazz) {
          for {
            homework <- homeworkApi.findOrCreate(clazzId, course.id, course.index, me.id)
            nextCourse <- courseApi.findByCourseIndex(clazzId, course.index + 1)
          } yield Ok(html.clazz.homework.form(forms.createOf(homework), homework, clazz, course, nextCourse))
        }
      }
    }
  }

  /*def update(id: String) = SecureBody(_.Coach) { implicit ctx => me =>
    OptionFuResult(for {
      homeworkOption <- homeworkApi.byId(id)
      clazzOption <- homeworkOption.??(h => api.byId(h.clazzId))
      courseOption <- homeworkOption.??(h => courseApi.byId(h.courseId))
    } yield (homeworkOption |@| clazzOption |@| courseOption).tupled) {
      case (homework, clazz, course) => {
        Owner(clazz) {
          if (homework.isPublished) ForbiddenResult
          else {
            implicit def req = ctx.body
            forms.create.bindFromRequest.fold(
              fail => courseApi.findByCourseIndex(homework.clazzId, course.index + 1) map { nextCourse =>
                Ok(html.clazz.homework.form(fail, homework, clazz, course, nextCourse))
              },
              data => homeworkApi.update(homework, course, data, me.id) inject
                Redirect(routes.Homework.createForm(homework.clazzId, homework.courseId))
            )
          }
        }
      }
    }
  }
*/

  def updateOrPublish(id: String) = SecureBody(_.Coach) { implicit ctx => me =>
    OptionFuResult(for {
      homeworkOption <- homeworkApi.byId(id)
      clazzOption <- homeworkOption.??(h => api.byId(h.clazzId))
      courseOption <- homeworkOption.??(h => courseApi.byId(h.courseId))
    } yield (homeworkOption |@| clazzOption |@| courseOption).tupled) {
      case (homework, clazz, course) => {
        Owner(clazz) {
          if (homework.isPublished) ForbiddenResult
          else {
            implicit def req = ctx.body
            forms.create.bindFromRequest.fold(
              fail => courseApi.findByCourseIndex(homework.clazzId, course.index + 1) map { nextCourse =>
                Ok(html.clazz.homework.form(fail, homework, clazz, course, nextCourse))
              },
              data => {
                val method = data.method
                if (method == "update") {
                  homeworkApi.update(homework, course, data, me.id)
                } else if (method == "publish") {
                  homeworkApi.updateAndPublish(homework, clazz, course, data, me.id)
                } else funit
              } inject Redirect(routes.Homework.createForm(homework.clazzId, homework.courseId))
            )
          }
        }
      }
    }
  }

  def show(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(for {
      stuHomeworkOption <- stuHomeworkApi.byId(id)
      clazzOption <- stuHomeworkOption.??(h => api.byId(h.clazzId))
      courseOption <- stuHomeworkOption.??(h => courseApi.byId(h.courseId))
    } yield (stuHomeworkOption |@| clazzOption |@| courseOption).tupled) {
      case (stuHomework, clazz, course) => {
        if (stuHomework.isCreator(me.id) || stuHomework.belongTo(me.id)) {
          UserRepo.byId(clazz.coach) map { coach =>
            Ok(html.clazz.homework.show(stuHomework, clazz, course, coach))
          }
        } else ForbiddenResult
      }
    }
  }

  def show2(clazzId: String, courseId: String) = Auth { implicit ctx => me =>
    (for {
      stuHomeworkOption <- stuHomeworkApi.byId2(clazzId, courseId, me.id)
      clazzOption <- stuHomeworkOption.??(h => api.byId(h.clazzId))
      courseOption <- stuHomeworkOption.??(h => courseApi.byId(h.courseId))
    } yield (stuHomeworkOption |@| clazzOption |@| courseOption).tupled).flatMap {
      case None => notFoundHomework(ctx)
      case Some((stuHomework, clazz, course)) => {
        if (stuHomework.isCreator(me.id) || stuHomework.belongTo(me.id)) {
          UserRepo.byId(clazz.coach) map { coach =>
            Ok(html.clazz.homework.show(stuHomework, clazz, course, coach))
          }
        } else ForbiddenResult
      }
    }
  }

  def solveReplayGame(id: String, studyId: String, chapterId: String) = Auth { implicit ctx => me =>
    OptionFuResult(stuHomeworkApi.byId(id)) { homework =>
      if (ctx.me.??(me => homework.belongTo(me.id))) {
        homeworkSolve.handleReplayGame(me.id, id, studyId, chapterId) inject Redirect(s"/study/$studyId/$chapterId")
      } else ForbiddenResult
    }
  }

  def itemModal(ids: String) = Auth { implicit ctx => me =>
    Ok(views.html.clazz.homework.modal.itemModal(ids.split(",").toList)).fuccess
  }

  def replayGameModal = Auth { implicit ctx => me =>
    Ok(views.html.clazz.homework.modal.replayGameModal).fuccess
  }

  def recallModal = Auth { implicit ctx => me =>
    Ok(views.html.clazz.homework.modal.recallForm(recallFrom.create(me))).fuccess
  }

  def report(id: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(for {
      homeworkOption <- homeworkApi.byId(id)
      clazzOption <- homeworkOption.??(h => api.byId(h.clazzId))
      courseOption <- homeworkOption.??(h => courseApi.byId(h.courseId))
    } yield (homeworkOption |@| clazzOption |@| courseOption).tupled) {
      case (homework, clazz, course) => {
        OwnerHomework(homework) {
          for {
            homeworkReportOption <- homeworkReport.byId(id)
            users <- homeworkReportOption.??(h => UserRepo.byIds(h.common.keySet))
          } yield Ok(html.clazz.homework.report(homework, homeworkReportOption, users, clazz, course))
        }
      }
    }
  }

  def refreshReport(id: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(for {
      homeworkOption <- homeworkApi.byId(id)
      clazzOption <- homeworkOption.??(h => api.byId(h.clazzId))
    } yield (homeworkOption |@| clazzOption).tupled) {
      case (homework, clazz) => {
        OwnerHomework(homework) {
          if (!homework.available) ForbiddenResult
          else {
            homeworkReport.refreshReport(homework, clazz) inject Redirect(routes.Homework.report(id))
          }
        }
      }
    }
  }

  private def notFoundHomework(implicit ctx: Context): Fu[Result] = Ok(html.clazz.homework.notFound()).fuccess

  private def BelongTo(homework: HomeworkModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.??(me => homework.isCreator(me.id))) f
    else ForbiddenResult

  private def OwnerHomework(homework: HomeworkModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.??(me => homework.isCreator(me.id))) f
    else ForbiddenResult

  private def Owner(clazz: ClazzModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.??(me => clazz.isCreator(me.id))) f
    else ForbiddenResult

  private def ForbiddenResult(implicit ctx: Context) = Forbidden(views.html.site.message.authFailed).fuccess

}
