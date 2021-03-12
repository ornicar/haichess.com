package controllers

import lila.app.Env
import lila.app._
import lila.clazz.Student.InviteStatus
import lila.user.UserRepo
import play.api.data._
import play.api.data.Forms._
import lila.clazz.{ Student => StudentModel }
import views._

object Student extends LilaController {

  private val env = Env.clazz

  def invite(clazzId: String) = SecureBody(_.Coach) { implicit ctx => me =>
    implicit def req = ctx.body
    Form(single(
      "username" -> lila.user.DataForm.historicalUsernameField
    )).bindFromRequest.fold(
      err => Redirect(routes.Clazz.detail(clazzId, errorsAsJson(err).toString().some)).fuccess,
      username =>
        if (me.username == username) {
          Redirect(routes.Clazz.detail(clazzId, "不可以邀请自己".some)).fuccess
        } else {
          UserRepo named username flatMap {
            case None => Redirect(routes.Clazz.detail(clazzId, "邀请学员不存在".some)).fuccess
            case Some(u) => env.api.byId(clazzId) flatMap {
              case None => Redirect(routes.Clazz.detail(clazzId, "班级不存在".some)).fuccess
              case Some(clazz) => env.studentApi.byId(clazzId, u.id) flatMap {
                case None => env.studentApi.addStudent(clazz, u.id, StudentModel.make) inject Redirect(routes.Clazz.detail(clazzId))
                case Some(s) => s.statusPretty match {
                  case InviteStatus.Invited => Redirect(routes.Clazz.detail(clazzId, "请勿重复邀请".some)).fuccess
                  case InviteStatus.Joined => Redirect(routes.Clazz.detail(clazzId, "学员已经加入".some)).fuccess
                  case InviteStatus.Refused => env.studentApi.invitedAgain(clazz, u.id) inject Redirect(routes.Clazz.detail(clazzId))
                  case InviteStatus.Expired => env.studentApi.invitedAgain(clazz, u.id) inject Redirect(routes.Clazz.detail(clazzId))
                }
              }
            }
          }
        }
    )
  }

  def remove(clazzId: String, userId: String) = Secure(_.Coach) { implicit ctx => me =>
    OptionFuResult(env.api.byId(clazzId)) { clazz =>
      Clazz.Owner(clazz) {
        env.studentApi.removeStudent(clazz, userId) inject {
          Redirect(routes.Clazz.detail(clazzId))
        }
      }
    }
  }

  def acceptForm(clazzId: String, error: Option[String]) = Auth { implicit ctx => me =>
    OptionFuOk(env.api.byIdWithCoach(clazzId)) { clazzWithCoach =>
      Env.team.api.team(clazzWithCoach.clazz.teamOrDefault) map { team =>
        html.clazz.accept(clazzWithCoach, team, error)
      }
    }
  }

  def accept(clazzId: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.api.byId(clazzId)) { clazz =>
      env.studentApi.byId(clazzId, me.id) flatMap {
        case None => Redirect(routes.Student.acceptForm(clazzId, "没有邀请信息".some)).fuccess
        case Some(s) => s.statusPretty match {
          case InviteStatus.Invited => env.studentApi.accept(clazz, me) inject {
            Redirect(routes.Clazz.detail(clazzId))
          }
          case InviteStatus.Joined => Redirect(routes.Student.acceptForm(clazzId, "请勿重复操作".some)).fuccess
          case InviteStatus.Expired => Redirect(routes.Student.acceptForm(clazzId, "邀请已经过期".some)).fuccess
          case InviteStatus.Refused => Redirect(routes.Student.acceptForm(clazzId, "邀请已经拒绝".some)).fuccess
        }
      }
    }
  }

  def refused(clazzId: String) = Auth { implicit ctx => me =>
    env.studentApi.byId(clazzId, me.id) flatMap {
      case None => Redirect(routes.Student.acceptForm(clazzId, "没有邀请信息".some)).fuccess
      case Some(s) => s.statusPretty match {
        case InviteStatus.Invited => env.studentApi.refused(clazzId, me) inject {
          Redirect(routes.Student.acceptForm(clazzId))
        }
        case InviteStatus.Joined => Redirect(routes.Student.acceptForm(clazzId, "学员已经加入".some)).fuccess
        case InviteStatus.Expired => Redirect(routes.Student.acceptForm(clazzId, "邀请已经过期".some)).fuccess
        case InviteStatus.Refused => Redirect(routes.Student.acceptForm(clazzId, "邀请已经拒绝".some)).fuccess
      }
    }
  }
}
