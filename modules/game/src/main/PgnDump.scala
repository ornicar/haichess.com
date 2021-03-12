package lila.game

import chess.format.Forsyth
import chess.format.pgn.{ Pgn, Tag, Tags, TagType, Parser, ParsedPgn }
import chess.format.{ FEN, pgn => chessPgn }
import chess.{ Centis, Color }

import lila.common.LightUser

final class PgnDump(
    netBaseUrl: String,
    getLightUser: LightUser.Getter
) {

  import PgnDump._

  def apply(game: Game, initialFen: Option[FEN], flags: WithFlags): Fu[Pgn] = {
    val imported = game.pgnImport.flatMap { pgni =>
      Parser.full(pgni.pgn).toOption
    }
    val tagsFuture =
      if (flags.tags) tags(game, initialFen, imported, withOpening = flags.opening)
      else fuccess(Tags(Nil))
    tagsFuture map { ts =>
      val turns = flags.moves ?? {
        val fenSituation = ts.fen.map(_.value) flatMap Forsyth.<<<
        val moves2 = if (fenSituation.exists(_.situation.color.black)) ".." +: game.pgnMoves else game.pgnMoves
        makeTurns(
          moves2,
          fenSituation.map(_.fullMoveNumber) | 1,
          flags.clocks ?? ~game.bothClockStates,
          game.startColor
        )
      }
      Pgn(ts, turns)
    }
  }

  private def gameUrl(id: String) = s"$netBaseUrl/$id"

  private def gameLightUsers(game: Game): Fu[(Option[LightUser], Option[LightUser])] =
    (game.whitePlayer.userId ?? getLightUser) zip (game.blackPlayer.userId ?? getLightUser)

  private def rating(p: Player) = p.rating.fold("?")(_.toString)

  def player(p: Player, u: Option[LightUser]) =
    p.aiLevel.fold(u.fold(p.name | lila.user.User.anonymous)(_.name))("lichess AI level " + _)

  private val customStartPosition: Set[chess.variant.Variant] =
    Set(chess.variant.Chess960, chess.variant.FromPosition, chess.variant.Horde, chess.variant.RacingKings)

  private def eventOf(game: Game) = {
    val perf = game.perfType.fold("Standard")(_.name)
    game.tournamentId.map { id =>
      s"${game.mode} $perf tournament https://haichess.com/tournament/$id"
    } orElse game.simulId.map { id =>
      s"$perf simul https://haichess.com/simul/$id"
    } getOrElse {
      s"${game.mode} $perf game"
    }
  }

  private def ratingDiffTag(p: Player, tag: Tag.type => TagType) =
    p.ratingDiff.map { rd => Tag(tag(Tag), s"${if (rd >= 0) "+" else ""}$rd") }

  def tags(
    game: Game,
    initialFen: Option[FEN],
    imported: Option[ParsedPgn],
    withOpening: Boolean
  ): Fu[Tags] = gameLightUsers(game) map {
    case (wu, bu) => Tags {
      val importedDate = imported.flatMap(_.tags(_.Date))
      List[Option[Tag]](
        Tag(_.Event, imported.flatMap(_.tags(_.Event)) | { if (game.imported) "Import" else eventOf(game) }).some,
        Tag(_.Site, gameUrl(game.id)).some,
        Tag(_.Date, importedDate | Tag.UTCDate.format.print(game.createdAt)).some,
        Tag(_.Round, imported.flatMap(_.tags(_.Round)) | "-").some,
        Tag(_.White, player(game.whitePlayer, wu)).some,
        Tag(_.Black, player(game.blackPlayer, bu)).some,
        Tag(_.Result, result(game)).some,
        importedDate.isEmpty option Tag(_.UTCDate, imported.flatMap(_.tags(_.UTCDate)) | Tag.UTCDate.format.print(game.createdAt)),
        importedDate.isEmpty option Tag(_.UTCTime, imported.flatMap(_.tags(_.UTCTime)) | Tag.UTCTime.format.print(game.createdAt)),
        Tag(_.WhiteElo, rating(game.whitePlayer)).some,
        Tag(_.BlackElo, rating(game.blackPlayer)).some,
        ratingDiffTag(game.whitePlayer, _.WhiteRatingDiff),
        ratingDiffTag(game.blackPlayer, _.BlackRatingDiff),
        wu.flatMap(_.title).map { t => Tag(_.WhiteTitle, t) },
        bu.flatMap(_.title).map { t => Tag(_.BlackTitle, t) },
        Tag(_.Variant, game.variant.name.capitalize).some,
        Tag.timeControl(game.clock.map(_.config)).some,
        Tag(_.ECO, game.opening.fold("?")(_.opening.eco)).some,
        withOpening option Tag(_.Opening, game.opening.fold("?")(_.opening.name)),
        Tag(_.Termination, {
          import chess.Status._
          game.status match {
            case Created | Started => "Unterminated"
            case Aborted | NoStart => "Abandoned"
            case Timeout | Outoftime => "Time forfeit"
            case Resign | Draw | Stalemate | Mate | VariantEnd => "Normal"
            case Cheat => "Rules infraction"
            case UnknownFinish => "Unknown"
          }
        }).some
      ).flatten ::: customStartPosition(game.variant).??(List(
          Tag(_.FEN, initialFen.fold("?")(_.value)),
          Tag("SetUp", "1")
        ))
    }
  }

  private def makeTurns(moves: Seq[String], from: Int, clocks: Vector[Centis], startColor: Color): List[chessPgn.Turn] =
    (moves grouped 2).zipWithIndex.toList map {
      case (moves, index) =>
        val clockOffset = startColor.fold(0, 1)
        chessPgn.Turn(
          number = index + from,
          white = moves.headOption filter (".." !=) map { san =>
            chessPgn.Move(
              san = san,
              secondsLeft = clocks lift (index * 2 - clockOffset) map (_.roundSeconds)
            )
          },
          black = moves lift 1 map { san =>
            chessPgn.Move(
              san = san,
              secondsLeft = clocks lift (index * 2 + 1 - clockOffset) map (_.roundSeconds)
            )
          }
        )
    } filterNot (_.isEmpty)
}

object PgnDump {

  case class WithFlags(
      clocks: Boolean = true,
      moves: Boolean = true,
      tags: Boolean = true,
      evals: Boolean = true,
      opening: Boolean = true,
      literate: Boolean = false
  )

  def result(game: Game) =
    if (game.finished) Color.showResult(game.winnerColor)
    else "*"
}
