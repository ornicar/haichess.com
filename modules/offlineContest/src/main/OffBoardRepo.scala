package lila.offlineContest

import reactivemongo.bson._
import lila.db.dsl._
import lila.game.Game

object OffBoardRepo {

  private[offlineContest] lazy val coll = Env.current.boardColl
  import BSONHandlers._

  def byId(id: Game.ID): Fu[Option[OffBoard]] = coll.byId[OffBoard](id)

  def getByContest(contestId: OffContest.ID): Fu[List[OffBoard]] =
    coll.find(contestQuery(contestId)).list[OffBoard]()

  def getByUser(contestId: OffContest.ID, no: OffRound.No, userId: String): Fu[List[OffBoard]] =
    coll.find(
      contestQuery(contestId) ++ $or(
        $doc("whitePlayer.userId" -> userId),
        $doc("blackPlayer.userId" -> userId)
      ) ++ $doc("roundNo" $lte no)
    ).sort($doc("startsAt" -> 1)).list[OffBoard]()

  def getByRound(roundId: OffRound.ID): Fu[List[OffBoard]] =
    coll.find(roundQuery(roundId)).list[OffBoard]()

  def bulkInsert(roundId: OffRound.ID, boardList: List[OffBoard]): Funit =
    coll.remove(roundQuery(roundId)) >>
      coll.bulkInsert(
        documents = boardList.map(boardHandler.write).toStream,
        ordered = true
      ).void

  def batchStart(roundId: OffRound.ID): Funit =
    coll.update(
      roundQuery(roundId),
      $set("status" -> OffBoard.Status.Started.id),
      multi = true
    ).void

  def setStatus(id: OffBoard.ID, status: OffBoard): Funit =
    coll.update(
      $id(id),
      $set("status" -> status.id)
    ).void

  def allFinished(id: OffRound.ID, boards: Option[Int]): Fu[Boolean] =
    coll.countSel(roundQuery(id) ++ $doc("status" -> OffBoard.Status.Finished.id)) map { count =>
      boards.?? { _ == count }
    }

  def setWinner(id: Game.ID, winner: Option[chess.Color]): Funit =
    winner.fold {
      coll.update(
        $id(id),
        $set("status" -> OffBoard.Status.Finished.id) ++ $unset("whitePlayer.isWinner", "blackPlayer.isWinner")
      ).void
    } { color =>
      coll.update(
        $id(id),
        $set(
          "whitePlayer.isWinner" -> (color == chess.Color.White),
          "blackPlayer.isWinner" -> (color == chess.Color.Black),
          "status" -> OffBoard.Status.Finished.id
        )
      ).void
    }

  def update(board: OffBoard): Funit =
    coll.update($id(board.id), board).void

  def remove(id: Game.ID): Funit = coll.remove($id(id)).void

  def contestQuery(contestId: OffContest.ID) = $doc("contestId" -> contestId)

  def roundQuery(roundId: OffRound.ID) = $doc("roundId" -> roundId)

}
