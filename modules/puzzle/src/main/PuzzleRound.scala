package lila.puzzle

import lila.user.User
import org.joda.time.DateTime
import ornicar.scalalib.Random

case class PuzzleRound(
    id: PuzzleRound.ID,
    userId: User.ID,
    puzzleId: PuzzleId,
    userRating: Int,
    puzzleRating: Int,
    createTime: DateTime,
    season: Int,
    result: Result,
    seconds: Int,
    lines: List[ResultNode],
    timeout: Option[Boolean],
    rushId: Option[PuzzleRush.ID] = None,
    battleId: Option[PuzzleRush.ID] = None
) {

}

object PuzzleRound {

  case class RoundWithPuzzle(round: PuzzleRound, puzzle: Puzzle) {

    import play.api.libs.json._
    def toJson: JsObject = Json.obj(
      "id" -> puzzle.id,
      "fen" -> puzzle.fenAfterInitialMove.get,
      "color" -> puzzle.color.name,
      "rating" -> round.puzzleRating,
      "lastMove" -> puzzle.initialUci,
      "win" -> round.result.win
    ).add("timeout" -> round.timeout)
  }

  type ID = String

  def makeByResult(res: lila.puzzle.Puzzle.UserResult): PuzzleRound = PuzzleRound(
    id = makeId,
    userId = res.userId,
    puzzleId = res.puzzleId,
    userRating = res.rating._1,
    puzzleRating = res.puzzleRating._1,
    createTime = DateTime.now(),
    season = makeSeason,
    result = res.result,
    seconds = res.seconds,
    lines = res.lines,
    timeout = res.timeout,
    rushId = res.rushId,
    battleId = res.battleId
  )

  def makeId = Random nextString 12
  def makeSeason = DateTime.now().toString("yyyyMM").toInt

  object BSONFields {
    val id = "_id"
    val userId = "u"
    val puzzleId = "p"
    val userRating = "ur"
    val puzzleRating = "pr"
    val createTime = "d"
    val season = "s"
    val result = "r"
    val seconds = "sc"
    val lines = "lines"
    val timeout = "to"
    val rushId = "ri"
    val battleId = "bi"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler

  private implicit val ResultBSONHandler = booleanAnyValHandler[Result](_.win, Result.apply)
  private implicit val ResultNodeHandler = Macros.handler[ResultNode]
  private implicit val ResultNodeArrayHandler = bsonArrayToListHandler[ResultNode]

  implicit val PuzzleRoundBSONHandler = new BSON[PuzzleRound] {
    import BSONFields._
    def reads(r: BSON.Reader): PuzzleRound = {
      PuzzleRound(
        id = r.str(id),
        userId = r.str(userId),
        puzzleId = r.int(puzzleId),
        userRating = r.int(userRating),
        puzzleRating = r.int(puzzleRating),
        createTime = r.get[DateTime](createTime),
        season = r.int(season),
        result = r.get[Result](result),
        seconds = r.int(seconds),
        lines = r.getO[List[ResultNode]](lines) | List.empty,
        timeout = r.boolO(timeout),
        rushId = r.strO(rushId),
        battleId = r.strO(battleId)
      )
    }

    def writes(w: BSON.Writer, o: PuzzleRound) = BSONDocument(
      id -> o.id,
      userId -> o.userId,
      puzzleId -> o.puzzleId,
      userRating -> o.userRating,
      puzzleRating -> o.puzzleRating,
      createTime -> o.createTime,
      season -> o.season,
      result -> o.result,
      seconds -> o.seconds,
      lines -> o.lines,
      timeout -> o.timeout,
      rushId -> o.rushId,
      battleId -> o.battleId
    )
  }
}

case class ResultNode(san: String, uci: String, fen: String)
