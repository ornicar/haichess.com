package lila.puzzle

import chess.Situation
import chess.format.{ Forsyth, Uci, UciCharPair }
import play.api.libs.json._
import lila.game.GameRepo
import lila.resource.ThemeQuery
import lila.tree
import org.joda.time.DateTime

final class JsonView(
    gameJson: GameJson,
    animationDuration: scala.concurrent.duration.Duration
) {

  def apply(
    puzzle: Puzzle,
    userInfos: Option[UserInfos],
    mode: String,
    themeShow: Option[ThemeShow] = None,
    puzzleErrors: Option[PuzzleErrors] = None,
    capsule: Option[LightCapsule] = None,
    homework: Option[LightHomework] = None,
    mobileApi: Option[lila.common.ApiVersion],
    round: Option[Round] = None,
    result: Option[Result] = None,
    voted: Option[Boolean],
    puzzleApi: PuzzleApi,
    showNextPuzzle: Boolean = true,
    rated: Boolean = true
  ): Fu[JsObject] = {
    val isOldMobile = mobileApi.exists(_.value < 3)
    val isMobile = mobileApi.isDefined
    val gameJsObject: Fu[Option[JsObject]] = puzzle.ipt.fold {
      (!isOldMobile ?? gameJson(
        gameId = puzzle.gameId,
        plies = puzzle.initialPly,
        onlyLast = isMobile
      ) map some)
    } { i =>
      fuccess(importGameJson(puzzle, i))
    }

    for {
      gameJson <- gameJsObject
      puzzleTagger <- puzzleApi.tagger.find(puzzle.id, userInfos)
    } yield {
      Json.obj(
        "game" -> gameJson,
        "puzzle" -> puzzleJson(puzzle, isOldMobile)
          .add("liked" -> puzzleTagger.isDefined)
          .add("tagger" -> puzzleTagger.map { pt =>
            Json.obj("tags" -> pt.tags, "date" -> pt.date)
          })
          .add("ipt" -> puzzle.ipt.map { i =>
            Json.obj(
              "tags" -> i.tags,
              "hasLastMove" -> i.hasLastMove,
              "userId" -> i.userId,
              "date" -> i.date
            )
          })
          .add("mark" -> puzzle.mark.map { m =>
            Json.obj(
              "rating" -> m.rating,
              "phase" -> ThemeQuery.parseLabel(m.phase, ThemeQuery.phase),
              "moveFor" -> ThemeQuery.parseArrayLabel(m.moveFor, ThemeQuery.moveFor),
              "subject" -> ThemeQuery.parseArrayLabel(m.subject, ThemeQuery.subject),
              "strength" -> ThemeQuery.parseArrayLabel(m.strength, ThemeQuery.strength),
              "chessGame" -> ThemeQuery.parseArrayLabel(m.chessGame, ThemeQuery.chessGame),
              "comprehensive" -> ThemeQuery.parseArrayLabel(m.comprehensive, ThemeQuery.comprehensive),
              "tag" -> m.tag
            )
          }),
        "mode" -> mode,
        "themeShow" -> themeShow.map { ts =>
          Json.obj(
            "rnf" -> ts.rnf,
            "showDrawer" -> ts.showDrawer
          )
        },
        "puzzleErrors" -> puzzleErrors.map { pe =>
          Json.obj(
            "rating" -> pe.rating,
            "time" -> pe.time.toString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
          )
        },
        "capsule" -> capsule.map { ca =>
          Json.obj(
            "id" -> ca.id,
            "name" -> ca.name
          )
        },
        "homework" -> homework.map { hm =>
          Json.obj(
            "id" -> hm.id,
            "clazzId" -> hm.clazzId,
            "courseId" -> hm.courseId,
            "clazzName" -> hm.clazzName,
            "week" -> hm.week,
            "index" -> hm.index,
            "dateTime" -> hm.dateTime.toString("yyyy-MM-dd HH:mm")
          )
        },
        "showNextPuzzle" -> showNextPuzzle,
        "rated" -> rated,
        "attempt" -> round.ifTrue(isOldMobile).map { r =>
          Json.obj(
            "userRatingDiff" -> r.ratingDiff,
            "win" -> r.result.win,
            "seconds" -> "a few" // lol we don't have the value anymore
          )
        },
        "voted" -> voted,
        "user" -> userInfos.map(JsonView.infos(isOldMobile)),
        "difficulty" -> isOldMobile.option {
          Json.obj(
            "choices" -> Json.arr(
              Json.arr(2, "Normal")
            ),
            "current" -> 2
          )
        }
      ).noNull
    }
  }

  def importGameJson(puzzle: Puzzle, ipt: ImportMeta): Option[JsObject] = {
    Json.obj(
      "id" -> puzzle.gameId,
      "perf" -> Json.obj(
        "icon" -> "8",
        "name" -> "Standard"
      ),
      "rated" -> false,
      "players" -> JsArray(
        List(
          Json.obj(
            "userId" -> "",
            "name" -> "?",
            "color" -> "white"
          ),
          Json.obj(
            "userId" -> "",
            "name" -> "?",
            "color" -> "white"
          )
        )
      ),
      "treeParts" -> makeTreeParts(puzzle, ipt)
    ).some
  }

  def makeTreeParts(puzzle: Puzzle, ipt: ImportMeta) = {
    val treePart = JsArray()
    val ply = puzzle.initialPly
    if (!ipt.hasLastMove) {
      val currentSituation: Situation = (Forsyth << puzzle.fen) get
      val currentNode =
        Json.obj(
          "ply" -> ply,
          "fen" -> puzzle.fen
        ).add("check" -> currentSituation.check)
      treePart.append(currentNode)
    } else {
      val prevFen = puzzle.fen
      val currentFen = ipt.fenAfterMove.get
      val prevMove = puzzle.history.head
      val prevSituation: Situation = (Forsyth << prevFen) get
      val currentSituation: Situation = (Forsyth << currentFen) get
      val move = Uci.Move.apply(prevMove).get
      val uci = Uci(prevMove).get
      val san = chess.Game(none, prevFen.some).apply(move).foldRight(s"error with move $move")((r, _) => r._1.pgnMoves.last)
      val id = UciCharPair(uci).toString()

      val prevNode = Json.obj(
        "ply" -> (ply - 1),
        "fen" -> prevFen
      ).add("check" -> prevSituation.check)

      val currentNode =
        Json.obj(
          "ply" -> ply,
          "fen" -> currentFen
        ).add("check" -> currentSituation.check)
          .add("id" -> id.some)
          .add("uci" -> prevMove.some)
          .add("san" -> san.some)
      treePart.append(prevNode).append(currentNode)
    }
  }

  def pref(p: lila.pref.Pref) = Json.obj(
    "blindfold" -> p.blindfold,
    "coords" -> p.coords,
    "rookCastle" -> p.rookCastle,
    "animation" -> Json.obj(
      "duration" -> p.animationFactor * animationDuration.toMillis
    ),
    "destination" -> p.destination,
    "resizeHandle" -> p.resizeHandle,
    "moveEvent" -> p.moveEvent,
    "highlight" -> p.highlight,
    "is3d" -> p.is3d
  )

  def batch(puzzles: List[Puzzle], userInfos: UserInfos): Fu[JsObject] = for {
    games <- GameRepo.gameOptionsFromSecondary(puzzles.map(_.gameId))
    jsons <- (puzzles zip games).collect {
      case (puzzle, Some(game)) =>
        gameJson.noCache(game, puzzle.initialPly, true) map { gameJson =>
          Json.obj(
            "game" -> gameJson,
            "puzzle" -> puzzleJson(puzzle, isOldMobile = false)
          )
        }
    }.sequenceFu
  } yield Json.obj(
    "user" -> JsonView.infos(false)(userInfos),
    "puzzles" -> jsons
  )

  def puzzleJson(puzzle: Puzzle, isOldMobile: Boolean): JsObject = Json.obj(
    "id" -> puzzle.id,
    "rating" -> puzzle.perf.intRating,
    "attempts" -> puzzle.attempts,
    "fen" -> puzzle.fen,
    "color" -> puzzle.color.name,
    "initialPly" -> puzzle.initialPly,
    "gameId" -> puzzle.gameId,
    "lines" -> lila.puzzle.Line.toJson(puzzle.lines),
    "vote" -> puzzle.vote.sum
  ).add("initialMove" -> puzzle.initialUci.some)
    .add("branch" -> (!isOldMobile).option(makeBranch(puzzle)))
    .add("enabled" -> puzzle.enabled)

  private def makeBranch(puzzle: Puzzle): Option[tree.Branch] = {
    import chess.format._
    val fullSolution: List[Uci.Move] = (Line solution puzzle.lines).map { uci =>
      Uci.Move(uci) err s"Invalid puzzle solution UCI $uci"
    }
    val solution =
      if (fullSolution.isEmpty) {
        logger.warn(s"Puzzle ${puzzle.id} has an empty solution from ${puzzle.lines}")
        fullSolution
      } else if (fullSolution.size % 2 == 0) fullSolution.init
      else fullSolution
    def init = {
      val g = chess.Game(none, puzzle.fenAfterInitialMove)
      puzzle.ipt.fold(
        g.withTurns(puzzle.initialPly)
      ) { i =>
          if (i.hasLastMove) {
            g.withTurns(puzzle.initialPly)
          } else {
            g
          }
        }
    }
    val (_, branchList) = solution.foldLeft[(chess.Game, List[tree.Branch])]((init, Nil)) {
      case ((prev, branches), uci) =>
        val (game, move) = prev(uci.orig, uci.dest, uci.promotion).prefixFailuresWith(s"puzzle ${puzzle.id}").err
        val branch = tree.Branch(
          id = UciCharPair(move.toUci),
          ply = game.turns,
          move = Uci.WithSan(move.toUci, game.pgnMoves.last),
          fen = chess.format.Forsyth >> game,
          check = game.situation.check,
          crazyData = none
        )
        (game, branch :: branches)
    }
    branchList.foldLeft[Option[tree.Branch]](None) {
      case (None, branch) => branch.some
      case (Some(child), branch) => Some(branch addChild child)
    }
  }

  object light {

    def make(puzzle: Puzzle): JsObject =
      Json.obj(
        "game" -> gameJson(puzzle),
        "puzzle" -> puzzleJson(puzzle)
      )

    def puzzleJson(puzzle: Puzzle): JsObject = Json.obj(
      "id" -> puzzle.id,
      "rating" -> puzzle.perf.intRating,
      "attempts" -> puzzle.attempts,
      "fen" -> puzzle.fen,
      "fenAfterLastMove" -> puzzle.fenAfterInitialMove,
      "color" -> puzzle.color.name,
      "initialPly" -> puzzle.initialPly,
      "gameId" -> puzzle.gameId,
      "lines" -> lila.puzzle.Line.toJson(puzzle.lines),
      "vote" -> puzzle.vote.sum,
      "initialMove" -> puzzle.initialUci.some
    )

    def gameJson(puzzle: Puzzle): JsObject =
      Json.obj(
        "treeParts" -> makeTreeParts(puzzle)
      )

    def makeTreeParts(puzzle: Puzzle) = {
      val treePart = JsArray()
      val ply = puzzle.initialPly

      val prevFen = puzzle.fen
      val currentFen = puzzle.fenAfterInitialMove get
      val prevMove = puzzle.history.head
      val prevSituation: Situation = (Forsyth << prevFen) get
      val currentSituation: Situation = (Forsyth << currentFen) get
      val move = Uci.Move.apply(prevMove).get
      val uci = Uci(prevMove).get
      val san = chess.Game(none, prevFen.some).apply(move).foldRight(s"error with move $move")((r, _) => r._1.pgnMoves.last)
      val id = UciCharPair(uci).toString()

      val prevNode = Json.obj(
        "ply" -> (ply - 1),
        "fen" -> prevFen
      ).add("check" -> prevSituation.check)

      val currentNode =
        Json.obj(
          "ply" -> ply,
          "fen" -> currentFen
        ).add("check" -> currentSituation.check)
          .add("id" -> id.some)
          .add("uci" -> prevMove.some)
          .add("san" -> san.some)
      treePart.append(prevNode).append(currentNode)
    }

  }
}

object JsonView {

  def infos(isOldMobile: Boolean)(i: UserInfos): JsObject = Json.obj(
    "rating" -> i.user.perfs.puzzle.intRating,
    "history" -> isOldMobile.option(i.history.map(_.rating)), // for mobile BC
    "recent" -> i.history.map { r =>
      Json.arr(r.id.puzzleId, r.ratingDiff, r.rating)
    }
  ).noNull

  def round(r: Round): JsObject = Json.obj(
    "ratingDiff" -> r.ratingDiff,
    "win" -> r.result.win
  )

}

