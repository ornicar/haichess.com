package lila.puzzle

import scala.collection.breakOut
import chess.Color
import chess.format.{ Forsyth, Uci }
import org.joda.time.DateTime

case class Puzzle(
    id: PuzzleId,
    gameId: String,
    history: List[String],
    fen: String,
    lines: List[Line],
    depth: Int,
    color: Color,
    date: DateTime,
    perf: PuzzlePerf,
    vote: AggregateVote,
    attempts: Int,
    mate: Boolean,
    likes: Option[Int] = None,
    ipt: Option[ImportMeta] = None,
    mark: Option[PuzzleMark] = None
) {

  val rating = perf.glicko.rating

  def isImport = ipt.isDefined

  def initialPly: Int = {
    val ply = initialPlyOld
    ipt.fold(ply) { i =>
      if (ply == 0 && i.hasLastMove) 1 else ply
    }
  }

  // ply after "initial move" when we start solving
  def initialPlyOld: Int = {
    fen.split(' ').lastOption flatMap parseIntOption map { move =>
      move * 2 - color.fold(0, 1)
    }
  } | 0

  // (1 - 3)/(1 + 3) = -0.5
  def enabled = vote.ratio > AggregateVote.minRatio || vote.nb < AggregateVote.minVotes

  def withVote(f: AggregateVote => AggregateVote) = copy(vote = f(vote))

  def initialMove: Uci.Move = history.lastOption flatMap Uci.Move.apply err s"Bad initial move $this"

  def initialUci: String =
    if (ipt.isEmpty) {
      initialMove.uci
    } else ""

  def fenAfterInitialMove: Option[String] = {
    ipt.fold(fenAfterInitialMove2) { i =>
      if (i.hasLastMove) fenAfterInitialMove2 else fen.some
    }
  }

  def fenAfterInitialMove2: Option[String] = {
    for {
      sit1 <- Forsyth << fen
      sit2 <- sit1.move(initialMove).toOption.map(_.situationAfter)
    } yield Forsyth >> sit2
  }

  def withId(id: PuzzleId): Puzzle = copy(id = id)
}

object Puzzle {

  case class UserResult(
      puzzleId: PuzzleId,
      userId: lila.user.User.ID,
      result: Result,
      rating: (Int, Int),
      puzzleRating: (Int, Int),
      seconds: Int,
      lines: List[ResultNode],
      source: String, // puzzle(计分), them(不计分), rush(不计分), battle(不计分)
      timeout: Option[Boolean],
      rushId: Option[PuzzleRush.ID] = None,
      battleId: Option[PuzzleRush.ID] = None,
      homeworkId: Option[String] = None
  )

  def make(
    gameId: String,
    history: List[String],
    fen: String,
    color: Color,
    lines: Lines,
    mate: Boolean,
    ipt: Option[ImportMeta] = None
  )(id: PuzzleId) = new Puzzle(
    id = id,
    gameId = gameId,
    history = history,
    fen = fen,
    lines = lines,
    depth = Line minDepth lines,
    color = color,
    date = DateTime.now,
    perf = PuzzlePerf.default,
    vote = AggregateVote.default,
    attempts = 0,
    mate = mate,
    ipt = ipt
  )

  def default: Puzzle = {
    make(
      gameId = "-",
      history = Nil,
      fen = chess.format.Forsyth.initial,
      color = Color.white,
      lines = Nil,
      mate = false,
      ImportMeta.make(
        pgn = "",
        tags = none,
        hasLastMove = false,
        fenAfterMove = chess.format.Forsyth.initial.some,
        userId = none,
        date = none
      )
    )(0)
  }

  sealed abstract class Source(val id: String, val name: String)
  object Source {
    case object Checkmate1 extends Source(id = "checkmate1", name = "生成/一步杀")
    case object Checkmate2 extends Source(id = "checkmate2", name = "生成/二步杀")
    case object Checkmate3 extends Source(id = "checkmate3", name = "生成/三步杀")
    case object Changjiang extends Source(id = "draw_changjiang", name = "生成/长将")
    case object Qizi extends Source(id = "draw_qizi", name = "生成/弃子")
    case object Mark extends Source(id = "lichess-mark", name = "Lichess/标注")
    case object Unmark extends Source(id = "lichess-unmark", name = "Lichess/非标注")

    val default = Checkmate1

    val all = List(Checkmate1, Checkmate2, Checkmate3, Changjiang, Qizi, Mark, Unmark)

    val keys = all map { v => v.id } toSet

    val byId = all map { v => (v.id, v) } toMap

    def apply(id: String): Source = byId.get(id) err s"Bad Source $this"

  }

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  private implicit val lineBSONHandler = new BSONHandler[BSONDocument, Lines] {
    private def readMove(move: String) = chess.Pos.doublePiotrToKey(move take 2) match {
      case Some(m) => s"$m${move drop 2}"
      case _ => sys error s"Invalid piotr move notation: $move"
    }
    def read(doc: BSONDocument): Lines = doc.elements.map {
      case BSONElement(move, BSONBoolean(true)) => Win(readMove(move))

      case BSONElement(move, BSONBoolean(false)) => Retry(readMove(move))

      case BSONElement(move, more: BSONDocument) =>
        Node(readMove(move), read(more))

      case BSONElement(move, value) =>
        throw new Exception(s"Can't read value of $move: $value")
    }(breakOut)
    private def writeMove(move: String) = chess.Pos.doubleKeyToPiotr(move take 4) match {
      case Some(m) => s"$m${move drop 4}"
      case _ => sys error s"Invalid move notation: $move"
    }
    def write(lines: Lines): BSONDocument = BSONDocument(lines map {
      case Win(move) => writeMove(move) -> BSONBoolean(true)
      case Retry(move) => writeMove(move) -> BSONBoolean(false)
      case Node(move, lines) => writeMove(move) -> write(lines)
    })
  }

  object BSONFields {
    val id = "_id"
    val gameId = "gameId"
    val history = "history"
    val fen = "fen"
    val lines = "lines"
    val depth = "depth"
    val white = "white"
    val date = "date"
    val perf = "perf"
    val rating = s"$perf.gl.r"
    val vote = "vote"
    val voteNb = s"$vote.nb"
    val voteRatio = s"$vote.ratio"
    val day = "day"
    val attempts = "attempts"
    val mate = "mate"
    val mark = "mark"
    val ipt = "ipt"
    val likes = "likes"
    val likers = "likers"
    val taggers = "taggers"
    val source = "idHistory.source"
  }

  implicit val puzzleBSONHandler = new BSON[Puzzle] {

    import BSONFields._
    import PuzzlePerf.puzzlePerfBSONHandler
    import AggregateVote.aggregatevoteBSONHandler
    import ImportMeta.importBSONHandler
    import PuzzleMark.markBSONHandler

    def reads(r: BSON.Reader): Puzzle = Puzzle(
      id = r int id,
      gameId = r str gameId,
      history = r str history split ' ' toList,
      fen = r str fen,
      lines = r.get[Lines](lines),
      depth = r int depth,
      color = Color(r bool white),
      date = r date date,
      perf = r.get[PuzzlePerf](perf),
      vote = r.get[AggregateVote](vote),
      attempts = r int attempts,
      mate = r bool mate,
      likes = r intO likes,
      ipt = r.getO[ImportMeta](ipt),
      mark = r.getO[PuzzleMark](mark)
    )

    def writes(w: BSON.Writer, o: Puzzle) = BSONDocument(
      id -> o.id,
      gameId -> o.gameId,
      history -> o.history.mkString(" "),
      fen -> o.fen,
      lines -> o.lines,
      depth -> o.depth,
      white -> o.color.white,
      date -> o.date,
      perf -> o.perf,
      vote -> o.vote,
      attempts -> o.attempts,
      mate -> o.mate,
      ipt -> o.ipt
    )
  }

}
