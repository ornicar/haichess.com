package controllers

import lila.api.Context
import lila.app.Env
import lila.app._
import lila.recall.{ DataForm, Recall => RecallModel }
import play.api.mvc.Result
import views._

object Recall extends LilaController {

  private def env = Env.recall

  def home = Auth { implicit ctx => me =>
    env.api.history(me.id) flatMap { list =>
      env.jsonView(RecallModel.makeSyntheticRecall, list).map { data =>
        views.html.recall.show(
          data = data,
          pref = env.jsonView.pref(ctx.pref),
          home = true
        )
      }
    }
  }

  def show(id: String) = Auth { implicit ctx => me =>
    OptionFuOk(env.api.byId(id)) { recall =>
      env.api.history(me.id) flatMap { list =>
        env.jsonView(recall, list).map { data =>
          views.html.recall.show(
            data = data,
            pref = env.jsonView.pref(ctx.pref),
            home = false
          )
        }
      }
    }
  }

  def showOfMate(pgn: String) = Auth { implicit ctx => me =>
    env.api.history(me.id) flatMap { list =>
      val turns = get("turns").map(_.toInt)
      val color = get("color")
      val title = get("title")
      val homeworkId = get("homeworkId")
      env.jsonView(RecallModel.makeTemporaryRecall(turns, color, title), list, pgn.some).map { data =>
        Ok(
          views.html.recall.show(
            data = data.add("homeworkId" -> homeworkId),
            pref = env.jsonView.pref(ctx.pref),
            home = false
          )
        )
      }
    }
  }

  def page(page: Int) = Auth { implicit ctx => me =>
    env.api.page(me.id, page) map { pager =>
      Ok(views.html.recall.list(pager))
    }
  }

  def createForm = Auth { implicit ctx => me =>
    Ok(views.html.recall.modal.createForm(env.form.create(me))).fuccess
  }

  def create = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    env.form.create(me).bindFromRequest.fold(
      jsonFormError,
      data => env.api.create(data, me.id).map { pr =>
        Ok(jsonOkBody.add("id" -> pr.id.some))
      }
    ).map(_ as JSON)
  }

  def recallPgn = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    env.form.create(me).bindFromRequest.fold(
      jsonFormError,
      data => env.api.gamePgn(data).map { result =>
        Ok(
          jsonOkBody
            .add("fen" -> result._2.some)
            .add("pgn" -> result._1.some)
        )
      }
    ).map(_ as JSON)
  }

  def editForm(id: String, goTo: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.api.byId(id)) { recall =>
      Ok(views.html.recall.modal.editForm(recall, goTo, env.form.editOf(recall))).fuccess
    }
  }

  def update(id: String, goTo: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(env.api.byId(id)) { recall =>
      Owner(recall) {
        implicit val req = ctx.body
        env.form.edit.bindFromRequest.fold(
          jsonFormError,
          data => env.api.update(recall, data) inject Redirect(goTo)
        )
      }
    }
  }

  def delete(id: String, goTo: String) = Auth { implicit ctx => me =>
    OptionFuResult(env.api.byId(id)) { recall =>
      Owner(recall) {
        env.api.delete(recall) inject Redirect(goTo)
      }
    }
  }

  def finish(id: Option[String], homeworkId: Option[String]) = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    DataForm.finish.bindFromRequest.fold(
      jsonFormError,
      data => data match {
        case (win, turns) => {
          env.api.finish(id, homeworkId, win, turns, me.id).map { _ =>
            jsonOkResult
          }
        }
      }
    )
  }

  private def Owner(recall: RecallModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (ctx.me.??(me => recall.isCreator(me.id))) f
    else ForbiddenResult
  }

  private def ForbiddenResult(implicit ctx: Context) = Forbidden(views.html.site.message.authFailed).fuccess

}
