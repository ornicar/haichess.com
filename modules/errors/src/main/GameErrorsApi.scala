package lila.errors

import chess.Division
import chess.format.FEN
import lila.db.dsl._
import lila.user.User
import reactivemongo.bson._
import lila.common.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.paginator.Adapter
import lila.analyse.Analysis
import lila.errors.GameErrors.Judgement
import lila.game.{ Game, GameRepo }
import lila.round.JsonView.WithFlags

final class GameErrorsApi(coll: Coll, bus: lila.common.Bus) {

  import BSONHandlers._

  def receive(game: Game, analysis: Analysis): Funit = {
    if (game.hasAi) funit
    else {
      val plyJudgements: List[(Int, Judgement)] = {
        analysis.infoAdvices.foldLeft(List.empty[(Int, Judgement)]) {
          case (list, (info, adviceOption)) => {
            adviceOption.fold(list) { advice =>
              val name = advice.judgment.name
              if (name == "Mistake" || name == "Blunder") {
                list :+ (info.ply, Judgement(name.toLowerCase))
              } else list
            }
          }
        }
      }

      if (plyJudgements.isEmpty || analysis.studyId.isDefined) funit
      else {
        GameRepo.initialFen(game) flatMap { initialFen =>
          val divider = lila.game.Env.current.divider(game, initialFen)
          val mainlineNodeList = lila.round.TreeBuilder(
            game = game,
            analysis = analysis.some,
            initialFen = initialFen | FEN(game.variant.initialFen),
            withFlags = WithFlags()
          ).mainlineNodeList.filter(n => plyJudgements.exists(n.ply == _._1))

          val eco = chess.opening.Ecopening fromGame game.pgnMoves.toList
          val errors = mainlineNodeList.map { node =>
            val color = node.color.unary_!
            GameErrors.make(
              gameId = game.id,
              ply = node.ply,
              fen = node.fen,
              color = color,
              lastMove = node.moveOption.map(_.uci.uci),
              judgement = plyJudgements.find(_._1 == node.ply).map(_._2) err "cannot find judgement",
              phase = toPhase(divider, node.ply),
              eco = eco,
              gameAt = game.createdAt,
              opponent = color.fold(game.blackPlayer.userId, game.whitePlayer.userId) err s"the game has AI ${game.id}",
              userId = color.fold(game.whitePlayer.userId, game.blackPlayer.userId) err s"the game has AI ${game.id}"
            )
          }

          coll.bulkInsert(
            documents = errors.map(BSONHandlers.GameErrorsBSONHandler.write).toStream,
            ordered = true
          ).void
        }
      }
    }
  }

  private def toPhase(divider: Division, ply: Int): Option[GameErrors.Phase] = {
    val isOpening = divider.openingBounds.exists { b => ply >= b._1 && ply < b._2 }
    val isMiddle = divider.middleBounds.exists { b => ply >= b._1 && ply < b._2 }
    val isEnd = divider.endBounds.exists { b => ply >= b._1 && ply < b._2 }
    val phaseId =
      if (isOpening) 1.some
      else if (isMiddle) 2.some
      else if (isEnd) 3.some
      else none
    phaseId.map { id =>
      GameErrors.Phase(id)
    }
  }

  def removeByIds(ids: List[String]): Funit =
    coll.remove($inIds(ids)).void

  def removeById(id: Int, userId: User.ID): Funit =
    coll.remove($id(PuzzleErrors.makeId(userId, id))).void

  def page(page: Int, userId: User.ID, query: GameQuery): Fu[Paginator[GameErrors]] = {
    var condition = $doc("createBy" -> userId)
    if (query.gameAtMin.isDefined || query.gameAtMax.isDefined) {
      var gameAtRange = $doc()
      query.gameAtMin foreach { gameAtMin =>
        gameAtRange = gameAtRange ++ $gte(gameAtMin)
      }
      query.gameAtMax foreach { gameAtMax =>
        gameAtRange = gameAtRange ++ $lte(gameAtMax)
      }
      condition = condition ++ $doc("gameAt" -> gameAtRange)
    }

    query.color foreach { tg =>
      val color = tg map { _.toLowerCase == "white" }
      condition = condition ++ $doc("color" -> $in(color: _*))
    }
    query.opponent foreach { o =>
      condition = condition ++ $doc("opponent" -> o)
    }
    query.phase foreach { p =>
      condition = condition ++ $doc("phase" -> p)
    }
    query.judgement foreach { j =>
      condition = condition ++ $doc("judgement" -> j)
    }
    query.eco foreach { e =>
      condition = condition ++ $doc("eco" $regex (e, "i"))
    }
    //println(BSONDocument.pretty(condition))

    Paginator(
      adapter = new Adapter(
        collection = coll,
        selector = condition,
        projection = $empty,
        sort = $doc("createAt" -> -1)
      ),
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

}
