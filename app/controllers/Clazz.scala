package controllers

import lila.app.Env
import lila.app._
import lila.clazz.ClazzForm.CreateData
import lila.user.UserRepo
import lila.clazz.{ Clazz => ClazzModel }
import lila.api.Context
import play.api.mvc.{ Result }
import views._

object Clazz extends LilaController {

  private def env = Env.clazz
  private val form = env.form
  private val homeworkApi = env.homeworkApi
  private val stuHomeworkApi = env.homeworkStudentApi

  def current = Auth { implicit ctx => me =>
    env.api.current(me) map { list =>
      Ok(html.clazz.list.current(list))
    }
  }

  def history = Auth { implicit ctx => me =>
    env.api.history(me) map { list =>
      Ok(html.clazz.list.history(list))
    }
  }

  def createForm = Secure(_.Coach) { implicit ctx => me =>
    teamList(me).map { teams =>
      Ok(html.clazz.form.create(form.create(me, none) fill CreateData.default, teams))
    }
  }

  def create = SecureBody(_.Coach) { implicit ctx => me =>
    implicit def req = ctx.body
    form.create(me, none).bindFromRequest.fold(
      failure => teamList(me).map { teams =>
        Ok(html.clazz.form.create(failure, teams))
      },
      data => {
        val clazz = data.toClazz(me)
        env.api.create(clazz) map { _ =>
          Redirect(routes.Clazz.detail(clazz._id))
        }
      }
    )
  }

  def editForm(id: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(env.api.byId(id)) { clazz =>
      Owner(clazz) {
        teamList(me).map { teams =>
          Ok(html.clazz.form.update(form.create(me, id.some) fill CreateData.byClazz(clazz), teams, clazz))
        }
      }
    }
  }

  def update(id: String) = SecureBody(_.Coach) { implicit ctx => me =>
    OptionFuResult(env.api.byId(id)) { clazz =>
      Owner(clazz) {
        if (clazz.editable) {
          implicit def req = ctx.body
          form.create(me, id.some).bindFromRequest.fold(
            failure => teamList(me).map { teams =>
              Ok(html.clazz.form.update(failure, teams, clazz))
            },
            data => {
              val updater = data.withUpdate(clazz)
              val courseList = updater.toCourse(id, clazz.coach)
              env.api.update(updater) >> env.courseApi.bulkUpdate(id, courseList) map { _ =>
                Redirect(routes.Clazz.detail(id))
              }
            }
          )
        } else ForbiddenResult
      }
    }
  }

  def stop(id: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(env.api.byId(id)) { clazz =>
      Owner(clazz) {
        env.api.stop(id) >> env.courseApi.stopByClazz(id) inject {
          Redirect(routes.Clazz.current)
        }
      }
    }
  }

  def delete(id: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(env.api.byId(id)) { clazz =>
      Owner(clazz) {
        if (clazz.deleteAble) {
          env.api.delete(clazz) inject {
            Redirect(routes.Clazz.current)
          }
        } else ForbiddenResult
      }
    }
  }

  def detail(id: String, error: Option[String] = None) = Auth { implicit ctx => me =>
    OptionFuOk(env.api.byIdWithCoach(id)) { clazzWithCoach =>
      val ids: List[String] = clazzWithCoach.clazz.studentList.map(_._1)
      for {
        users <- UserRepo.byIds(ids.toSet)
        teamOption <- Env.team.api.team(clazzWithCoach.clazz.teamOrDefault)
        courseHomework <- homeworkApi.courseHomeworks(id, me.id)
      } yield html.clazz.detail(clazzWithCoach, users, teamOption, courseHomework, error)
    }
  }

  private[controllers] def Owner(clazz: ClazzModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.??(me => clazz.isCreator(me.id))) f
    else ForbiddenResult

  private def ForbiddenResult(implicit ctx: Context) = Forbidden(views.html.site.message.authFailed).fuccess

  private def teamList(me: lila.user.User): Fu[List[(String, String)]] =
    Env.team.api.mine(me).map(_.filter(t => t.team.enabled && t.team.certified && t.member.isOwnerOrCoach).map(t => t.team.id -> t.team.name))

}
