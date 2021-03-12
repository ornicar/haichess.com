package controllers

import lila.api.Context
import lila.app.{ Env, _ }
import lila.common.Form.numberIn
import play.api.mvc.Result
import play.api.data.Form
import play.api.data.Forms.{ list, _ }
import lila.resource.{ Capsule => CapsuleModel }
import lila.resource.DataForm.capsule.enabledSelect
import play.api.libs.json.{ JsArray, Json }

object Capsule extends LilaController {

  def api = Env.resource.capsuleApi
  def puzzleApi = Env.puzzle.api
  def forms = Env.resource.forms

  def list = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    def searchForm = forms.capsule.capsuleSearchForm
    searchForm.bindFromRequest.fold(
      fail => Ok(views.html.resource.capsule.list(fail, List.empty[CapsuleModel], Set.empty[String])).fuccess,
      data => {
        for {
          tags <- api.tags(me.id)
          list <- api.list(me.id, data.enabled, data.name, data.tags)
        } yield {
          Ok(views.html.resource.capsule.list(searchForm fill data, list, tags))
        }
      }
    )
  }

  def mine = Auth { implicit ctx => me =>
    api.list(me.id, 1.some) map { list =>
      Ok(views.html.resource.puzzle.capsuleModal(list))
    }
  }

  def mineOfHomework = Auth { implicit ctx => me =>
    api.list(me.id, none) map { list =>
      Ok(views.html.clazz.homework.modal.capsuleModal(list))
    }
  }

  def infos = Auth { implicit ctx => me =>
    val idList = get("ids").??(_.split(",").toList).distinct take 10
    api.byIds(idList) flatMap { capsules =>
      val puzzleIds = capsules.foldLeft(Set.empty[Int]) {
        case (all, capsule) => all ++ capsule.puzzlesWithoutRemove
      }
      puzzleApi.puzzle.findMany3(puzzleIds.toList) map { puzzles =>
        Ok(
          JsArray(
            capsules.map { capsule =>
              Json.obj(
                "id" -> capsule.id,
                "name" -> capsule.name,
                "puzzles" -> JsArray(
                  puzzles.filter(p => capsule.puzzlesWithoutRemove.contains(p.id)).map { puzzle =>
                    Json.obj(
                      "id" -> puzzle.id,
                      "fen" -> puzzle.fenAfterInitialMove,
                      "color" -> puzzle.color.name,
                      "lastMove" -> puzzle.initialUci,
                      "lines" -> lila.puzzle.Line.toJson2(puzzle.lines).toString
                    )
                  }
                )
              )
            }
          )
        )
      }
    } map (_ as JSON)
  }

  def createForm = Auth { implicit ctx => me =>
    Ok(views.html.resource.capsule.form.create(forms.capsule.capsuleForm)).fuccess
  }

  def create = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    forms.capsule.capsuleForm.bindFromRequest.fold(
      fail => Ok(views.html.resource.capsule.form.create(fail)).fuccess,
      data => api.create(me.id, data) inject Redirect(routes.Capsule.list)
    )
  }

  def updateForm(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { capsule =>
      Owner(capsule) {
        Ok(views.html.resource.capsule.form.update(
          forms.capsule.capsuleFormOf(capsule), capsule
        )).fuccess
      }
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { capsule =>
      Owner(capsule) {
        implicit def req = ctx.body
        forms.capsule.capsuleForm.bindFromRequest.fold(
          fail => Ok(views.html.resource.capsule.form.update(fail, capsule)).fuccess,
          data => api.update(id, capsule, data) inject Redirect(routes.Capsule.list)
        )
      }
    }
  }

  def remove(id: String) = Auth { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { capsule =>
      Owner(capsule) {
        api.remove(id) inject Redirect(routes.Capsule.list)
      }
    }
  }

  def enable(id: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(api.byId(id)) { capsule =>
      Owner(capsule) {
        implicit val req = ctx.body
        Form(single("enabled" -> numberIn(enabledSelect))).bindFromRequest.fold(
          _ => Redirect(routes.Capsule.list).fuccess,
          enabled => api.enable(id, (enabled == 1)) inject Redirect(routes.Capsule.list)
        )
      }
    }
  }

  def addPuzzle(id: String) = Auth { implicit ctx => implicit me =>
    OptionFuResult(api.byId(id)) { capsule =>
      Owner(capsule) {
        val idList = get("ids").??(_.split(",").toList).distinct.map(_.toInt) take 15
        if ((capsule.puzzlesWithoutRemove ++ idList.toSet).size > 15) {
          BadRequest(jsonError("每个列表最多添加15道战术题")).fuccess
        } else {
          api.addPuzzle(capsule, idList.toSet) inject jsonOkResult
        }
      }
    }
  }

  def delPuzzle(id: String) = Auth { implicit ctx => implicit me =>
    OptionFuResult(api.byId(id)) { capsule =>
      Owner(capsule) {
        val idList = get("ids").??(_.split(",").toList).distinct.map(_.toInt) take 15
        api.delPuzzle(capsule, idList.toSet) inject Redirect(routes.Resource.puzzleCapsule(id, 1))
      }
    }
  }

  private def Owner(capsule: CapsuleModel)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (ctx.me.??(me => capsule.isCreator(me.id))) f
    else ForbiddenResult
  }

  private def ForbiddenResult(implicit ctx: Context): Fu[Result] =
    Forbidden(views.html.site.message.authFailed).fuccess

}
