package lila.importer

import chess.format.pgn._
import chess.format.{ FEN, Forsyth }
import chess.{ Color, Mode, Replay, Status }
import play.api.data._
import play.api.data.Forms._
import scalaz.Validation.FlatMap._
import lila.game._
import lila.puzzle.{ ImportMeta, Lines, Node, Puzzle, Win }

private[importer] final class DataForm(val batchMaxSize: Int) {

  lazy val importForm = Form(mapping(
    "pgn" -> nonEmptyText.verifying("invalidPgn", checkPgn _),
    "analyse" -> optional(nonEmptyText),
    "gameTag" -> optional(text)
  )(ImportData.apply)(ImportData.unapply))

  private def checkPgn(pgn: String): Boolean = MultiPgn.split(pgn, max = batchMaxSize).value.map { onePgn =>
    ImportData(onePgn, none).preprocess(none).isSuccess
  }.forall(_ == true)

  lazy val fenForm = Form(mapping(
    "pgn" -> nonEmptyText,
    "hasLastMove" -> boolean,
    "puzzleTag" -> optional(text)
  )(PuzzleImportData.apply)(PuzzleImportData.unapply).verifying("invalidPgn", _.checkPuzzle))

}

private[importer] case class TagResult(status: Status, winner: Option[Color])
case class Preprocessed(
    game: NewGame,
    replay: Replay,
    initialFen: Option[FEN],
    parsed: ParsedPgn
)

case class ImportData(pgn: String, analyse: Option[String], gameTag: Option[String] = None) {

  private type TagPicker = Tag.type => TagType

  private val maxPlies = 600

  private def evenIncomplete(result: Reader.Result): Replay = result match {
    case Reader.Result.Complete(replay) => replay
    case Reader.Result.Incomplete(replay, _) => replay
  }

  def makeTags = {
    gameTag.map { t =>
      val tags = t.trim()
      if (tags.isEmpty) {
        List()
      } else {
        tags.split(",").toList
      }
    }
  }

  def preprocess(user: Option[String]): Valid[Preprocessed] = Parser.full(pgn) flatMap {
    case parsed @ ParsedPgn(_, tags, sans) => Reader.fullWithSans(
      pgn,
      sans => sans.copy(value = sans.value take maxPlies),
      Tags.empty
    ) map evenIncomplete map {
        case replay @ Replay(setup, _, state) =>
          val initBoard = parsed.tags.fen.map(_.value) flatMap Forsyth.<< map (_.board)
          val fromPosition = initBoard.nonEmpty && !parsed.tags.fen.contains(FEN(Forsyth.initial))
          val variant = {
            parsed.tags.variant | {
              if (fromPosition) chess.variant.FromPosition
              else chess.variant.Standard
            }
          } match {
            case chess.variant.Chess960 if !Chess960.isStartPosition(setup.board) => chess.variant.FromPosition
            case chess.variant.FromPosition if parsed.tags.fen.isEmpty => chess.variant.Standard
            case chess.variant.Standard if fromPosition => chess.variant.FromPosition
            case v => v
          }
          val game = state.copy(situation = state.situation withVariant variant)
          val initialFen = parsed.tags.fen.map(_.value) flatMap {
            Forsyth.<<<@(variant, _)
          } map Forsyth.>> map FEN.apply

          val status = parsed.tags(_.Termination).map(_.toLowerCase) match {
            case Some("normal") | None => Status.Resign
            case Some("abandoned") => Status.Aborted
            case Some("time forfeit") => Status.Outoftime
            case Some("rules infraction") => Status.Cheat
            case Some(_) => Status.UnknownFinish
          }

          val date = parsed.tags.anyDate

          def name(whichName: TagPicker, whichRating: TagPicker): String = parsed.tags(whichName).fold("?") { n =>
            n + ~parsed.tags(whichRating).map(e => s" (${e take 8})")
          }

          val dbGame = Game.make(
            chess = game,
            whitePlayer = Player.make(chess.White, None) withName name(_.White, _.WhiteElo),
            blackPlayer = Player.make(chess.Black, None) withName name(_.Black, _.BlackElo),
            mode = Mode.Casual,
            source = Source.Import,
            pgnImport = PgnImport.make(user = user, date = date, pgn = pgn, tags = makeTags).some
          ).sloppy.start |> { dbGame =>
            // apply the result from the board or the tags
            game.situation.status match {
              case Some(situationStatus) => dbGame.finish(situationStatus, game.situation.winner).game
              case None => parsed.tags.resultColor.map {
                case Some(color) => TagResult(status, color.some)
                case None if status == Status.Outoftime => TagResult(status, none)
                case None => TagResult(Status.Draw, none)
              }.filter(_.status > Status.Started).fold(dbGame) { res =>
                dbGame.finish(res.status, res.winner).game
              }
            }
          }

          Preprocessed(NewGame(dbGame), replay.copy(state = game), initialFen, parsed)
      }
  }
}

case class PuzzleImportData(
    pgn: String,
    hasLastMove: Boolean,
    puzzleTag: Option[String] = None
) {

  private val maxPlies = 600

  def preprocess(userId: Option[String]) = Parser.full(pgn) flatMap {
    case parsed @ ParsedPgn(_, tags, sans) => Reader.fullWithSans(
      pgn,
      sans => sans.copy(value = sans.value take maxPlies),
      Tags.empty
    ) map {
        /*failureNel(s"Invalid FEN $atFen")*/
        case Reader.Result.Complete(replay) => {
          val moves: List[chess.Move] = replay.moves.map { mod =>
            mod.fold(
              move => move,
              drop => throw sys.error(drop.toString())
            )
          }

          val fen = parsed.tags.fen.fold(throw sys.error("缺少FEN标签"))(_.value)
          val lastMove = hasLastMove ?? List(moves.last.toUci.uci)
          val solution = if (!hasLastMove) moves else moves.take(moves.length - 1)
          val color = solution.last.situationBefore.color
          val mate = replay.state.situation.check
          val lines = toLine(solution.reverse)
          val date = parsed.tags.anyDate

          Puzzle.make(
            gameId = "-",
            history = lastMove,
            fen = fen,
            color = color,
            lines = lines,
            mate = mate,
            ImportMeta.make(
              pgn = pgn,
              tags = makeTags,
              hasLastMove = hasLastMove,
              fenAfterMove = if (hasLastMove) (Forsyth >> solution.last.situationBefore).some else None,
              userId = userId,
              date = date
            )
          )(-1)
        }
        case Reader.Result.Incomplete(replay, err) => throw sys.error(err.toString())
      }
  }

  def toLine(moves: List[chess.Move]): Lines = {
    val uci = moves.head.toUci.uci
    if (moves.length == 1) {
      List(Win(uci))
    } else {
      List(Node(uci, toLine(moves.drop(1))))
    }
  }

  def makeTags = {
    puzzleTag.map { t =>
      val tags = t.trim()
      if (tags.isEmpty) {
        List()
      } else {
        tags.split(",").toList
      }
    }
  }

  def checkPuzzle = MultiPgn.split(pgn, max = 20).value.map { onePgn =>
    checkOne(onePgn)
  } forall (_ == true)

  def checkOne(pgn: String): Boolean = {
    Parser.full(pgn) flatMap {
      case parsed @ ParsedPgn(_, _, _) =>
        if (parsed.tags.fen.isEmpty) return false
        Reader.fullWithSans(
          pgn,
          sans => sans.copy(value = sans.value take maxPlies),
          Tags.empty
        )
    } fold (_ => false, {
      case Reader.Result.Complete(replay) => {
        val moves: List[chess.Move] = replay.moves.map { mod =>
          mod.fold(
            move => move,
            _ => null
          )
        }
        val moveFilters = moves.filter(_ != null)
        if (replay.moves.length > moveFilters.length) return false
        if (moveFilters.length < 1 || (hasLastMove && moveFilters.length < 2)) return false
      }
      case Reader.Result.Incomplete(_, _) => return false
    })
    return true
  }

}
