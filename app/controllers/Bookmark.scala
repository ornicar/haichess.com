package controllers

import lila.app._
import lila.bookmark.DataForm
import play.api.libs.json._

object Bookmark extends LilaController {

  private def api = Env.bookmark.api

  def toggle(gameId: String) = Auth { implicit ctx => me =>
    api.toggle(gameId, me.id)
  }

  def setTag(gameId: String) = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    DataForm.tag.bindFromRequest.fold(
      jsonFormError,
      tags => {
        val tagList = tags.split(",").toList
        api.setTags(gameId, me.id, tagList) map { _ =>
          Ok(Json.arr(tagList))
        }
      }
    ) map (_ as JSON)
  }

}
