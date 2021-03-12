package controllers

import lila.api.Context
import lila.app._
import lila.app.mashup.GameFilterMenu
import lila.common.paginator.Paginator
import lila.game.GameRepo
import play.api.mvc.Result
import views._

object Resource extends LilaController {

  private val env = Env.resource
  private val puzzleEnv = Env.puzzle
  private val bookmarkEnv = Env.bookmark
  private val gameEnv = Env.game
  private def userGameSearch = Env.gameSearch.userGameSearch

  def puzzleLiked(page: Int) = AuthBody { implicit ctx => me =>
    Permiss {
      implicit def req = ctx.body
      def searchForm = env.forms.puzzle.liked
      searchForm.bindFromRequest.fold(
        failure => Ok(views.html.resource.puzzle.liked(failure, Paginator.empty, Set())).fuccess,
        data => {
          for {
            tags <- puzzleEnv.resource.likedTags(me.id)
            pager <- puzzleEnv.resource.liked(page, me.id, data)
          } yield {
            Ok(views.html.resource.puzzle.liked(searchForm fill data, pager, tags))
          }
        }
      )
    }
  }

  def puzzleImported(page: Int) = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    def searchForm = env.forms.puzzle.imported
    searchForm.bindFromRequest.fold(
      failure => Ok(views.html.resource.puzzle.imported(failure, Paginator.empty, Set())).fuccess,
      data => {
        for {
          tags <- puzzleEnv.resource.importedTags(me.id)
          pager <- puzzleEnv.resource.imported(page, me.id, data)
        } yield {
          Ok(views.html.resource.puzzle.imported(searchForm fill data, pager, tags))
        }
      }
    )
  }

  def puzzleTheme(page: Int) = AuthBody { implicit ctx => me =>
    Permiss {
      implicit def req = ctx.body
      def searchForm = env.forms.puzzle.theme
      searchForm.bindFromRequest.fold(
        failure => Ok(views.html.resource.puzzle.theme(failure, Paginator.empty, Set())).fuccess,
        data => {
          for {
            tags <- puzzleEnv.resource.themeTags()
            pager <- puzzleEnv.resource.theme(page, me.id, data)
          } yield {
            Ok(views.html.resource.puzzle.theme(searchForm fill data, pager, tags))
          }
        }
      )
    }
  }

  def puzzleCapsule(capsuleId: String, page: Int) = Auth { implicit ctx => me =>
    Permiss {
      Env.resource.capsuleApi.byId(capsuleId).flatMap {
        case None => Ok(views.html.resource.puzzle.capsule(Paginator.empty, capsuleId)).fuccess
        case Some(capsule) => {
          puzzleEnv.resource.capsule(page, capsule.puzzlesWithoutRemove.toList) map { pager =>
            Ok(views.html.resource.puzzle.capsule(pager, capsuleId))
          }
        }
      }
    }
  }

  def puzzleBatchDelete = AuthBody { implicit ctx => implicit me =>
    val idList = get("ids").??(_.split(",").toList).distinct.map(_.toInt) take 50
    puzzleEnv.resource.disableByIds(idList, me) inject Redirect(routes.Resource.puzzleImported(1))
  }

  def puzzleBatchUnlike = AuthBody { implicit ctx => implicit me =>
    Permiss {
      val idList = get("ids").??(_.split(",").toList).distinct.map(_.toInt) take 50
      puzzleEnv.api.tagger.removeByPuzzleIdsAndUser(idList, me.id) inject Redirect(routes.Resource.puzzleLiked(1))
    }
  }

  def gameLiked(page: Int) = AuthBody { implicit ctx => me =>
    Permiss {
      implicit def req = ctx.body
      def searchForm = env.forms.game.liked
      searchForm.bindFromRequest.fold(
        failure => Ok(views.html.resource.game.liked(failure, me, Paginator.empty, Set())).fuccess,
        data => {
          for {
            tags <- bookmarkEnv.api.bookmarkTags(me.id)
            pager <- bookmarkEnv.api.gamePaginatorByUser(me, page, data)
          } yield {
            Ok(views.html.resource.game.liked(searchForm fill data, me, pager, tags))
          }
        }
      )
    }
  }

  def gameImported(page: Int) = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    def searchForm = env.forms.game.imported
    searchForm.bindFromRequest.fold(
      failure => Ok(views.html.resource.game.imported(failure, me, Paginator.empty, Set())).fuccess,
      data => {
        for {
          tags <- GameRepo.importedTags(me.id)
          pager <- GameFilterMenu.imported(me, page, data)
        } yield {
          Ok(views.html.resource.game.imported(searchForm fill data, me, pager, tags))
        }
      }
    )
  }

  def gameSearch(page: Int) = AuthBody { implicit ctx => me =>
    Permiss {
      implicit def req = ctx.body
      userGameSearch(me, page) map { pager =>
        Ok(views.html.resource.game.search(userGameSearch.requestForm, me, pager))
      }
    }
  }

  def gameBatchDelete = AuthBody { implicit ctx => implicit me =>
    val idList = get("ids").??(_.split(",").toList).distinct take 50
    GameRepo.removeByIds(idList, me.id) inject Redirect(routes.Resource.gameImported(1))
  }

  def gameBatchUnlike = AuthBody { implicit ctx => implicit me =>
    Permiss {
      val idList = get("ids").??(_.split(",").toList).distinct take 50
      bookmarkEnv.api.removeByGameIdsAndUser(idList, me.id) inject Redirect(routes.Resource.gameLiked(1))
    }
  }

  private[controllers] def Permiss(f: => Fu[Result])(implicit ctx: Context): Fu[Result] = {
    if (ctx.me.??(_.hasResource)) f
    else NotAcceptable(html.member.notAccept()).fuccess
  }

}
