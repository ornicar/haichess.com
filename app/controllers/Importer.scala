package controllers

import lila.app._
import lila.common.HTTPRequest
import lila.importer.MultiPgn
import lila.puzzle.PuzzleId
import play.api.libs.json.Json
import play.api.mvc.Results
import scalaz.{ Failure, Success }
import views._

object Importer extends LilaController {

  private def env = Env.importer
  private def puzzle = Env.puzzle

  def importGame = AuthBody { implicit ctx => me =>
    fuccess {
      val pgn = ctx.body.queryString.get("pgn").flatMap(_.headOption).getOrElse("")
      val pgnData = lila.importer.ImportData(pgn, None)
      val pgnForm = env.forms.importForm.fill(pgnData)

      val fenForm = env.forms.fenForm
      Ok(html.game.importGame(pgnForm, fenForm, true, false))
    }
  }

  def feedback(source: String, success: Int) = Auth { implicit ctx => me =>
    Ok(html.game.importFeedback(source, success)).fuccess
  }

  def sendBatchGame = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    env.forms.importForm.bindFromRequest.fold(
      failure => negotiate(
        html = Ok(html.game.importGame(failure, env.forms.fenForm, true, false)).fuccess,
        api = _ => BadRequest(Json.obj("error" -> "Invalid PGN")).fuccess
      ),
      data => MultiPgn.split(data.pgn, max = env.batchMaxSize).value.map { onePgn =>
        env.importer(data.copy(pgn = onePgn), ctx.userId) flatMap { game =>
          (ctx.userId ?? Env.game.cached.clearNbImportedByCache) >>
            (data.analyse.isDefined && game.analysable) ?? {
              Env.fishnet.analyser(game, lila.fishnet.Work.Sender(
                userId = ctx.userId,
                ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
                mod = isGranted(_.Hunter) || isGranted(_.Relay),
                system = false
              ))
            }
        } recover {
          case e => {
            controllerLogger.branch("importer").warn(
              s"Imported game validates but can't be replayed:\n${onePgn}", e
            )
            0
          }
        } inject (1)
      }.sequenceFu.map { list =>
        Redirect(routes.Importer.feedback("game", list.sum))
      }
    )
  }

  /*  def sendPgnGame = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    env.forms.importForm.bindFromRequest.fold(
      failure => negotiate(
        html = Ok(html.game.importGame(failure, env.forms.fenForm, true, false)).fuccess,
        api = _ => BadRequest(Json.obj("error" -> "Invalid PGN")).fuccess
      ),
      data => env.importer(data, ctx.userId) flatMap { game =>
        (ctx.userId ?? Env.game.cached.clearNbImportedByCache) >>
          (data.analyse.isDefined && game.analysable) ?? {
            Env.fishnet.analyser(game, lila.fishnet.Work.Sender(
              userId = ctx.userId,
              ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
              mod = isGranted(_.Hunter) || isGranted(_.Relay),
              system = false
            ))
          } inject Redirect(routes.Round.watcher(game.id, "white"))
      } recover {
        case e =>
          controllerLogger.branch("importer").warn(
            s"Imported game validates but can't be replayed:\n${data.pgn}", e
          )
          Redirect(routes.Importer.importGame)
      }
    )
  }*/

  def systemImport(secret: String, gameId: String, puzzleId: PuzzleId) = OpenBody { implicit ctx =>
    if (secret != "B15iz3lcF12OmQj32wQ6M9jpYqLyKBtN") {
      fuccess(Results.Forbidden("Authorization failed"))
    } else {
      implicit def req = ctx.body
      env.forms.importForm.bindFromRequest.fold(
        failure => jsonFormErrorDefaultLang(failure),
        data => env.puzzleGameImporter(gameId, data.pgn) flatMap { game =>
          (data.analyse.isDefined && game.analysable) ?? {
            Env.fishnet.analyser(game, lila.fishnet.Work.Sender(
              userId = None,
              ip = HTTPRequest.lastRemoteAddress(ctx.req).some,
              mod = true,
              system = true
            ))
          } inject Ok(Json.arr(gameId, puzzleId))
        }
      )
    }
  }

  def sendBatchPuzzle = this.synchronized {
    AuthBody { implicit ctx => me =>
      implicit def req = ctx.body
      env.forms.fenForm.bindFromRequest.fold(
        failure => negotiate(
          html = Ok(html.game.importGame(env.forms.importForm, failure, false, true)).fuccess,
          api = _ => BadRequest(Json.obj("error" -> "Invalid puzzle")).fuccess
        ),
        data => (lila.db.Util findNextId (puzzle.puzzleColl)) flatMap { puzzleMaxId =>
          val minImportPuzzleId = 1000000
          var puzzleId = minImportPuzzleId atLeast puzzleMaxId
          MultiPgn.split(data.pgn, max = env.batchMaxSize).value.map { onePgn =>
            puzzleId = puzzleId + 1
            data.copy(pgn = onePgn) preprocess (me.id.some) match {
              case Success(p) =>
                puzzle.api.puzzle.insert(p.copy(id = puzzleId)) recover {
                  case e => {
                    controllerLogger.branch("importer").warn(
                      s"Imported puzzle validates but can't be replayed:\n${onePgn}", e
                    )
                    0
                  }
                } inject (1)
              case Failure(e) =>
                controllerLogger.branch("importer").warn(
                  s"Imported puzzle validates but can't be replayed:\n${onePgn}"
                )
                fufail(e)
            }
          }.sequenceFu.map { list =>
            Redirect(routes.Importer.feedback("puzzle", list.sum))
          }
        }
      )
    }
  }

  /*def sendPgnPuzzle = AuthBody { implicit ctx => me =>
    implicit def req = ctx.body
    env.forms.fenForm.bindFromRequest.fold(
      failure => negotiate(
        html = Ok(html.game.importGame(env.forms.importForm, failure, false, true)).fuccess,
        api = _ => BadRequest(Json.obj("error" -> "Invalid puzzle")).fuccess
      ),
      data => data preprocess (me.id.some) match {
        case Success(p) =>
          puzzle.api.puzzle.insert(p) map { puzzle =>
            Redirect(routes.Puzzle.show(puzzle.id))
          } recover {
            case e =>
              controllerLogger.branch("importer").warn(
                s"Imported puzzle validates but can't be replayed:\n${data.pgn}", e
              )
              Redirect(routes.Importer.importGame)
          }
        case Failure(e) =>
          Redirect(routes.Importer.importGame).fuccess
      }
    )
  }*/

  def masterGame(id: String, orientation: String) = Open { implicit ctx =>
    Env.explorer.importer(id) map {
      _ ?? { game =>
        val url = routes.Round.watcher(game.id, orientation).url
        val fenParam = get("fen").??(f => s"?fen=$f")
        Redirect(s"$url$fenParam")
      }
    }
  }

}
