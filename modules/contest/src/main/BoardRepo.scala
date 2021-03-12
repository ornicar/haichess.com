package lila.contest

import reactivemongo.bson._
import lila.db.dsl._
import lila.game.Game
import org.joda.time.DateTime

object BoardRepo {

  private[contest] lazy val coll = Env.current.boardColl
  import BSONHandlers._

  def insertMany(roundId: Round.ID, boardList: List[Board]): Funit =
    coll.remove(roundQuery(roundId)) >>
      coll.bulkInsert(
        documents = boardList.map(boardHandler.write).toStream,
        ordered = true
      ).void

  def byId(id: Game.ID): Fu[Option[Board]] = coll.byId[Board](id)

  def getByContest(contestId: Contest.ID): Fu[List[Board]] =
    coll.find(contestQuery(contestId)).list[Board]()

  def getByUser(contestId: Contest.ID, no: Round.No, userId: String): Fu[List[Board]] =
    coll.find(
      contestQuery(contestId) ++ $or(
        $doc("whitePlayer.userId" -> userId),
        $doc("blackPlayer.userId" -> userId)
      ) ++ $doc("roundNo" $lte no)
    ).sort($doc("startsAt" -> 1)).list[Board]()

  def getByRound(roundId: Round.ID): Fu[List[Board]] =
    coll.find(roundQuery(roundId)).list[Board]()

  def setStatus(id: Game.ID, status: chess.Status): Funit =
    coll.update(
      $id(id),
      $set("status" -> status.id)
    ).void

  def finishGame(game: Game): Funit = {
    val d =
      $doc(
        "status" -> game.status.id,
        "turns" -> game.turns.some,
        "finishAt" -> DateTime.now
      ) ++ game.wonBy(chess.Color.White).?? { win =>
          $doc("whitePlayer.isWinner" -> win)
        } ++ game.wonBy(chess.Color.Black).?? { win =>
          $doc("blackPlayer.isWinner" -> win)
        }

    coll.update(
      $id(game.id),
      $doc(
        "$set" -> d
      )
    ).void

  }

  def allFinished(id: Round.ID, boards: Option[Int]): Fu[Boolean] =
    coll.countSel(roundQuery(id) ++ $doc("status" $gte chess.Status.Aborted.id)) map { count =>
      boards.?? { _ == count }
    }

  def pending: Fu[List[Board]] =
    coll.find($doc("status" -> chess.Status.Created.id, "startsAt" -> ($lte(DateTime.now) ++ $gte(DateTime.now minusMinutes 20))) /* ++ apptQuery*/ ).list[Board]()

  // 找到未来1分钟内将要开始的对局
  def remindAtSoon: Fu[List[Board]] = {
    val doc = $doc("status" -> chess.Status.Created.id, "startsAt" -> ($lte(DateTime.now plusMinutes 1) ++ $gte(DateTime.now)), "reminded" -> false) /* ++ apptQuery*/
    coll.find(doc).list[Board]()
  }

  def setReminded(id: Game.ID): Funit =
    coll.update(
      $id(id),
      $set(
        "reminded" -> true
      )
    ).void

  def setWinner(id: Game.ID, winner: Option[chess.Color]): Funit =
    winner.fold {
      coll.update(
        $id(id),
        $unset("whitePlayer.isWinner", "blackPlayer.isWinner")
      ).void
    } { color =>
      coll.update(
        $id(id),
        $set(
          "whitePlayer.isWinner" -> (color == chess.Color.White),
          "blackPlayer.isWinner" -> (color == chess.Color.Black)
        )
      ).void
    }

  def update(board: Board): Funit =
    coll.update($id(board.id), board).void

  def remove(id: Game.ID): Funit = coll.remove($id(id)).void

  def setStartsTimeByRound(roundId: Round.ID, st: DateTime): Funit =
    coll.update(
      roundQuery(roundId),
      $set(
        "startsAt" -> st
      ),
      multi = true
    ).void

  def gameStartBatch(ids: List[Game.ID], startsAt: DateTime): Funit =
    coll.update(
      $doc("_id" -> $in(ids: _*)),
      $set(
        "status" -> chess.Status.Started.id,
        "startsAt" -> startsAt
      ),
      multi = true
    ).void

  def apptComplete(id: Game.ID, st: DateTime): Funit =
    coll.update(
      $id(id),
      $set("startsAt" -> st, "apptComplete" -> true)
    ).void

  def setStartsTime(id: Game.ID, st: DateTime): Funit =
    coll.update(
      $id(id),
      $set("startsAt" -> st)
    ).void

  def contestQuery(contestId: Contest.ID) = $doc("contestId" -> contestId)

  def roundQuery(roundId: Round.ID) = $doc("roundId" -> roundId)

  val apptQuery = $or(
    $doc("appt" -> false),
    $doc("appt" -> true, "apptComplete" -> true)
  )
}
