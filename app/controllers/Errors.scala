package controllers

import lila.app._
import lila.app.Env
import lila.errors.DataForm
import lila.common.paginator.Paginator
import views._

object Errors extends LilaController {

  def puzzleErrorsApi = Env.errors.puzzleErrorsApi
  def gameErrorsApi = Env.errors.gameErrorsApi

  def puzzle(page: Int) = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    def searchForm = DataForm.puzzle
    searchForm.bindFromRequest.fold(
      fail => Ok(views.html.errors.puzzle(fail, Paginator.empty)).fuccess,
      data => puzzleErrorsApi.page(page, me.id, data) map { pager =>
        Ok(views.html.errors.puzzle(searchForm fill data, pager))
      }
    )
  }

  def puzzleBatchRemove = AuthBody { implicit ctx => me =>
    val idList = get("ids").??(_.split(",").toList).distinct take 50
    puzzleErrorsApi.removeByIds(idList) inject Redirect(routes.Errors.puzzle(1))
  }

  def puzzleRemove(id: Int) = Auth { implicit ctx => me =>
    puzzleErrorsApi.removeById(id, me.id) map { _ =>
      jsonOkResult
    }
  }

  def game(page: Int) = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    def searchForm = DataForm.game
    searchForm.bindFromRequest.fold(
      fail => Ok(views.html.errors.game(fail, Paginator.empty)).fuccess,
      data => gameErrorsApi.page(page, me.id, data) map { pager =>
        Ok(views.html.errors.game(searchForm fill data, pager))
      }
    )
  }

  def gameBatchRemove = AuthBody { implicit ctx => me =>
    val idList = get("ids").??(_.split(",").toList).distinct take 50
    gameErrorsApi.removeByIds(idList) inject Redirect(routes.Errors.game(1))
  }

  def gameRemove(id: Int) = Auth { implicit ctx => me =>
    gameErrorsApi.removeById(id, me.id) map { _ =>
      jsonOkResult
    }
  }

}
