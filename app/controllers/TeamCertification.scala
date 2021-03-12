package controllers

import lila.app._
import views._
import lila.team.Certification

object TeamCertification extends LilaController {

  private def forms = Env.team.forms
  private def api = Env.team.api
  private def certificationApi = Env.team.certificationApi

  def certification(id: String) = Auth { implicit ctx => me =>
    OptionOk(api team id) { team =>
      html.team.forms.certification(team, forms.certificationOf(me, team))
    }
  }

  def certificationSend(id: String) = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    val form = forms.certificationForm(me).bindFromRequest
    OptionFuResult(api team id) { team =>
      team.certification match {
        case Some(c) => c.status match {
          case Certification.Status.Rejected => form.fold(
            err => BadRequest(html.team.forms.certification(team, err)).fuccess,
            data => certificationApi.certificationSend(team, data) inject Redirect(routes.TeamCertification.certification(team.id))
          )
          case Certification.Status.Applying => Forbidden("Can not apply certification status").fuccess
          case Certification.Status.Approved => Forbidden("Can not apply certification status").fuccess
        }
        case _ => form.fold(
          err => BadRequest(html.team.forms.certification(team, err)).fuccess,
          data => certificationApi.certificationSend(team, data) inject Redirect(routes.TeamCertification.certification(team.id))
        )
      }
    }
  }

  def modList(page: Int, s: String) = Secure(_.ManageTeam) { implicit ctx => me =>
    val status = Certification.Status(s)
    certificationApi.modPage(page, status) map { pager =>
      Ok(html.team.mod.list(pager, status))
    }
  }

  def modDetail(id: String) = Secure(_.ManageTeam) { implicit ctx => me =>
    OptionResult(api team id) { team =>
      Ok(html.team.mod.detail(team, forms.certificationProcessForm))
    }
  }

  def processCertification(id: String) = SecureBody(_.ManageTeam) { implicit ctx => me =>
    OptionFuResult(api team id) { team =>
      if (!team.certification.??(_.status.applying)) {
        Forbidden("Can not apply certification status").fuccess
      } else {
        implicit val req = ctx.body
        forms.certificationProcessForm.bindFromRequest.fold(
          fail => BadRequest(html.team.mod.detail(team, fail)).fuccess,
          {
            case (process, comments) =>
              certificationApi.processCertification(team, process == "approve", comments) >>
                Env.mod.logApi.teamCertificationProcess(me.id, team.name, team.description, process == "approve") inject
                Redirect(routes.TeamCertification.modList(1, Certification.Status.Approved.id))
          }
        )
      }
    }
  }

}
