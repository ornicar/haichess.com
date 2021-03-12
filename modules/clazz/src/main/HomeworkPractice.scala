package lila.clazz

import java.net.URLDecoder
import lila.user.User
import lila.game.Game
import chess.{ Clock, Color }
import lila.puzzle.Puzzle.UserResult
import play.api.libs.json.{ JsObject, Json }

case class HomeworkPractice(
    capsules: List[PuzzleCapsule], // 战术题
    replayGames: List[ReplayGame], // 打谱
    recallGames: List[RecallGame], // 记谱
    fromPositions: List[FromPosition]
) {

  def hasContent = capsules.nonEmpty || replayGames.nonEmpty || recallGames.nonEmpty || fromPositions.nonEmpty

  def puzzles: List[MiniPuzzle] = capsules.foldLeft(List.empty[MiniPuzzle]) {
    case (all, capsule) => all ++ capsule.puzzles
  }.distinct

}

object HomeworkPractice {

  val maxCapsule = 5
  val maxPuzzles = 15
  val maxReplayGame = 5
  val maxRecallGame = 5
  val maxFromPosition = 5

  def empty = HomeworkPractice(
    capsules = List.empty[PuzzleCapsule],
    replayGames = List.empty[ReplayGame],
    recallGames = List.empty[RecallGame],
    fromPositions = List.empty[FromPosition]
  )
}

case class PuzzleCapsule(id: String, name: String, puzzles: List[MiniPuzzle])
case class MiniPuzzle(id: Int, fen: String, color: Color, lastMove: Option[String], lines: String) {

  def firstRightMoveSet = {
    val ln = URLDecoder.decode(lines, "UTF-8")
    Json.parse(ln).asOpt[JsObject].map(_.keys)
  } err s"can not find puzzle right move of $id"

  override def toString: String = id.toString

}
case class Node(san: String, uci: String, fen: String)
case class Move(index: Int, white: Option[Node], black: Option[Node])
case class ReplayGame(chapterLink: String, name: String, root: String, moves: List[Move]) {

  def studyIdFromLink = {
    val arr = chapterLink.split("/")
    arr(arr.length - 2)
  }

  def chapterIdFromLink = {
    val arr = chapterLink.split("/")
    arr(arr.length - 1)
  }

  def isContains(studyId: String, chapterId: String) =
    studyId == studyIdFromLink && chapterId == chapterIdFromLink

  override def toString: String = s"$studyIdFromLink $chapterIdFromLink"

}
case class RecallGame(root: String, pgn: String, turns: Option[Int], color: Option[Color], title: Option[String]) {

  override def toString: String = s"${if (pgn.length < 50) pgn else pgn.substring(pgn.length - 51, pgn.length - 1)} ${turns | 0} ${color.map(_.name) | ""} ${title | ""}"

  def hashMD5: String = {
    import java.security.MessageDigest
    val md5 = MessageDigest.getInstance("MD5")
    val encoded = md5.digest((pgn + turns.??(_.toString) + color.??(_.name)).getBytes)
    encoded.map("%02x".format(_)).mkString
  }

}
case class FromPosition(fen: String, clock: Clock.Config, num: Int) {
  override def toString: String = s"$fen ${clock.show} $num"
}

//----------------------------------------------------------------------------------------------------------------------

case class HomeworkPracticeWithResult(
    puzzles: List[MiniPuzzleWithResult], // 战术题
    replayGames: List[ReplayGameWithResult], // 打谱
    recallGames: List[RecallGameWithResult], // 记谱
    fromPositions: List[FromPositionWithResult]
) {

  def findPuzzle(puzzle: MiniPuzzle) =
    puzzles.find(_.puzzle == puzzle) err s"can not find puzzle of $puzzle"

  def findReplayGame(replayGame: ReplayGame) =
    replayGames.find(_.replayGame == replayGame) err s"can not find replayGame of $replayGame"

  def findRecallGame(recallGame: RecallGame) =
    recallGames.find(_.recallGame.toString == recallGame.toString) err s"can not find recallGame of $recallGame"

  def findFromPosition(fromPosition: FromPosition) =
    fromPositions.find(_.fromPosition == fromPosition) err s"can not find fromPosition of $fromPosition"

  def count =
    puzzles.size +
      replayGames.size +
      recallGames.size +
      fromPositions.size

  def finishCount =
    puzzles.count(_.isComplete) +
      replayGames.count(_.isComplete) +
      recallGames.count(_.isComplete) +
      fromPositions.count(_.isComplete)
}

object HomeworkPracticeWithResult {

  def empty = HomeworkPracticeWithResult(
    puzzles = List.empty[MiniPuzzleWithResult],
    replayGames = List.empty[ReplayGameWithResult],
    recallGames = List.empty[RecallGameWithResult],
    fromPositions = List.empty[FromPositionWithResult]
  )

}

case class PuzzleResult(win: Boolean, lines: List[Node])
case class MiniPuzzleWithResult(puzzle: MiniPuzzle, result: Option[List[PuzzleResult]]) {

  val nonEmptyResultList = result.?? { r =>
    r.filterNot(_.lines.isEmpty)
  }

  def isTry = result.isDefined

  def isComplete = result.?? { r =>
    r.exists(_.win)
  }

  def isFirstComplete = result.?? { r =>
    r.headOption.exists(_.win)
  }

  def notComplete = isComplete

  def firstMoveUci = nonEmptyResultList.nonEmpty.?? {
    nonEmptyResultList.head.lines.head.uci
  }

  def firstMoveSan = nonEmptyResultList.nonEmpty.?? {
    nonEmptyResultList.head.lines.head.san
  }

  def firstRightMove = nonEmptyResultList.find(_.win).?? { r =>
    r.lines.head.uci
  }

  def finishPuzzle(res: UserResult): MiniPuzzleWithResult = copy(
    result = result.fold(
      List(
        PuzzleResult(
          win = res.result.win,
          lines = res.lines.map { n =>
            Node(n.san, n.uci, n.fen)
          }
        )
      )
    ) { old =>
        old :+ PuzzleResult(
          win = res.result.win,
          lines = res.lines.map { n =>
            Node(n.san, n.uci, n.fen)
          }
        )
      }.some
  )

}

case class ReplayGameResult(win: Boolean)
case class ReplayGameWithResult(replayGame: ReplayGame, result: Option[ReplayGameResult]) {

  def isTry = result.isDefined

  def isComplete = result.?? { _.win }

  def isContains(studyId: String, chapterId: String) = replayGame.isContains(studyId, chapterId)

  def finishReplayGame: ReplayGameWithResult = copy(
    result = {
      result | ReplayGameResult(true)
    }.some
  )
}

case class RecallGameResult(win: Boolean, turns: Int)
case class RecallGameWithResult(recallGame: RecallGame, result: Option[RecallGameResult]) {

  def isComplete = result.?? { _.win }

  def turns = result.??(_.turns)

  def finishRecallGame(win: Boolean, turns: Int): RecallGameWithResult = copy(
    result = {
      result.map(r =>
        if (turns > r.turns) {
          r.copy(
            win, turns
          )
        } else r) | RecallGameResult(win, turns)
    }.some
  )

}

case class FromPositionResult(gameId: Game.ID, white: User.ID, black: User.ID)
case class FromPositionWithResult(fromPosition: FromPosition, result: Option[List[FromPositionResult]]) {

  def sameFen(fen: String) = fen.split("-")(0) == fromPosition.fen.split("-")(0)

  def sameClock(clock: String) = clock == fromPosition.clock.show

  def isComplete = completeSize >= fromPosition.num

  def completeSize = result.??(_.size)

  def finishFromPosition(game: Game): FromPositionWithResult = copy(
    result = {
      result.fold(
        List(
          FromPositionResult(
            game.id,
            game.whitePlayer.userId | User.anonymous,
            game.blackPlayer.userId | User.anonymous
          )
        )
      ) { r =>
          r :+ FromPositionResult(
            game.id,
            game.whitePlayer.userId | User.anonymous,
            game.blackPlayer.userId | User.anonymous
          )
        }
    }.some
  )

}

//----------------------------------------------------------------------------------------------------------------------

case class HomeworkPracticeReport(
    puzzles: List[MiniPuzzleWithReport], // 战术题
    replayGames: List[ReplayGameWithReport], // 打谱
    recallGames: List[RecallGameWithReport], // 记谱
    fromPositions: List[FromPositionWithReport]
)

case class MoveNum(move: String, num: Int)
case class PuzzleReport(completeRate: Double, rightRate: Double, firstMoveRightRate: Double, rightMoveDistribute: List[MoveNum], wrongMoveDistribute: List[MoveNum])
case class MiniPuzzleWithReport(puzzle: MiniPuzzle, report: PuzzleReport)

case class ReplayGameReport(complete: Int)
case class ReplayGameWithReport(replayGame: ReplayGame, report: ReplayGameReport)

case class RecallGameReport(turns: Int, num: Int)
case class RecallGameWithReport(recallGame: RecallGame, report: List[RecallGameReport])

case class FromPositionReport(rounds: Int, num: Int)
case class FromPositionWithReport(fromPosition: FromPosition, report: List[FromPositionReport])