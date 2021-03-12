package lila.recall

import chess.format.FEN
import lila.db.dsl._
import lila.user.User
import lila.game.{ Game, GameRepo, PgnDump }
import lila.importer.{ ImportData, Preprocessed }
import lila.common.paginator.Paginator
import lila.db.paginator.Adapter
import lila.common.MaxPerPage
import chess.format.Forsyth
import scalaz.{ Failure, Success }

final class RecallApi(
    coll: Coll,
    bus: lila.common.Bus,
    studyApi: lila.study.StudyApi,
    importer: lila.importer.Importer,
    pgnDump: lila.game.PgnDump
) {

  import BSONHandlers._

  def byId(id: Recall.ID): Fu[Option[Recall]] = coll.byId[Recall](id)

  def page(userId: User.ID, page: Int): Fu[Paginator[Recall]] = {
    val adapter = new Adapter[Recall](
      collection = coll,
      selector = $doc(
        "deleted" -> false,
        "createBy" -> userId
      ),
      projection = $empty,
      sort = $sort desc "createAt"
    )
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

  def history(userId: User.ID): Fu[List[Recall]] =
    coll.find(
      $doc(
        "deleted" -> false,
        "createBy" -> userId
      )
    ).sort($sort desc "createAt").list(20)

  def create(data: RecallData, userId: User.ID): Fu[Recall] =
    {
      data.game match {
        case None => data.pgn match {
          case None => data.chapter match {
            case None => fufail("no available pgn")
            case Some(c) => fetchChapterPgn(data.studyId(c), data.chapterId(c)) flatMap {
              case None => fufail("no available chapter")
              case Some(p) => addGame(p, userId).map { game =>
                makeRecall(data, userId, game.id)
              }
            }
          }
          case Some(p) => addGame(p, userId).map { game =>
            makeRecall(data, userId, game.id)
          }
        }
        case Some(g) => fuccess(makeRecall(data, userId, data.gameId(g)))
      }
    } flatMap { recall =>
      coll.insert(recall) inject recall
    }

  private def fetchChapterPgn(studyId: String, chapterId: String): Fu[Option[String]] =
    studyApi.chapterPgn(studyId, chapterId)

  private def addGame(pgn: String, userId: User.ID): Fu[Game] =
    importer(
      ImportData(pgn, none),
      user = userId.some
    )

  def gamePgn(data: RecallData): Fu[(String, String)] = {
    data.pgn match {
      case None => data.chapter match {
        case None => data.game match {
          case None => fufail("no available pgn")
          case Some(url) => GameRepo.gameWithInitialFen(data.gameId(url)) flatMap {
            case None => fufail("no available game")
            case Some((g, initialFen)) => toPgn(g, initialFen)
          }
        }
        case Some(c) => fetchChapterPgn(data.studyId(c), data.chapterId(c)) flatMap {
          case None => fufail("no available chapter")
          case Some(p) => validToPgn(p)
        }
      }
      case Some(p) => validToPgn(p)
    }
  }

  private def validToPgn(pgn: String): Fu[(String, String)] =
    ImportData(pgn, none).preprocess(user = none) match {
      case Success(p) => toPgn(p.game.withId("-"), p.initialFen)
      case Failure(e) => fufail(e.toString())
    }

  private def toPgn(game: Game, initialFen: Option[FEN]): Fu[(String, String)] =
    pgnDump(
      game,
      initialFen,
      PgnDump.WithFlags(clocks = false, evals = false, opening = false)
    ).map(dump => dump.toString -> (initialFen.map(_.value) | Forsyth.initial))

  def update(recall: Recall, data: RecallEdit): Funit =
    coll.update(
      $id(recall.id),
      recall.copy(
        name = data.name,
        turns = data.turns,
        color = if (data.color == "all") None else chess.Color(data.color)
      )
    ).void

  def delete(recall: Recall): Funit =
    coll.update(
      $id(recall.id),
      $set("deleted" -> true)
    ).void

  def finish(id: Option[Recall.ID], homeworkId: Option[String], win: Boolean, turns: Int, userId: User.ID): Funit = {
    bus.publish(lila.hub.actorApi.Recall(id, homeworkId, win, turns, userId), 'recallFinished)
    funit
  }

  private def makeRecall(data: RecallData, userId: User.ID, gameId: String): Recall =
    Recall.make(
      name = data.name,
      gameId = gameId,
      turns = data.turns,
      color = if (data.color == "all") None else chess.Color(data.color),
      userId = userId
    )

}
