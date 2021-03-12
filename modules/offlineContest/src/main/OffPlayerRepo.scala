package lila.offlineContest

import lila.user.User
import lila.db.dsl._
import reactivemongo.bson._
import scala.collection.breakOut

object OffPlayerRepo {

  private[offlineContest] lazy val coll = Env.current.playerColl
  import BSONHandlers.playerHandler
  import BSONHandlers.boardHandler

  type ID = String

  def byId(id: OffPlayer.ID): Fu[Option[OffPlayer]] = coll.byId[OffPlayer](id)

  def byIds(ids: List[OffPlayer.ID]): Fu[List[OffPlayer]] = coll.byIds[OffPlayer](ids)

  def byOrderedIds(ids: List[OffPlayer.ID]): Fu[List[OffPlayer]] =
    coll.byOrderedIds[OffPlayer, OffPlayer.ID](ids)(_.id)

  def find(contestId: OffContest.ID, userId: User.ID): Fu[Option[OffPlayer]] =
    byId(OffPlayer.makeId(contestId, userId))

  def insert(player: OffPlayer): Funit = coll.insert(player).void

  def bulkUpdate(contestId: OffContest.ID, players: List[OffPlayer]): Funit =
    removeByContest(contestId) >> bulkInsert(players).void

  def bulkInsert(players: List[OffPlayer]): Funit = coll.bulkInsert(
    documents = players.map(playerHandler.write).toStream,
    ordered = true
  ).void

  def countByContest(contestId: OffContest.ID): Fu[Int] =
    coll.countSel(contestQuery(contestId))

  def getByUserId(userId: User.ID): Fu[List[OffPlayer]] =
    coll.find($doc("userId" -> userId)).list[OffPlayer]()

  def getByContest(contestId: OffContest.ID): Fu[List[OffPlayer]] =
    coll.find(contestQuery(contestId)).sort($sort asc "no").list[OffPlayer]()

  def remove(id: OffPlayer.ID): Funit =
    coll.remove($id(id)).void

  def removeByContest(contestId: OffContest.ID): Funit =
    coll.remove(contestQuery(contestId)).void

  def kick(id: OffPlayer.ID, isSetOutcome: Boolean): Funit =
    coll.update(
      $id(id),
      $set(
        "absent" -> true,
        "kick" -> true
      ) ++ (if (isSetOutcome) { $push("outcomes" -> OffBoard.Outcome.Kick.id) } else $empty)
    ).void

  def unAbsentByContest(contestId: OffContest.ID): Funit =
    coll.update(
      $doc("contestId" -> contestId, "manualAbsent" -> true, "kick" -> false),
      $set(
        "absent" -> false,
        "manualAbsent" -> false
      ),
      multi = true
    ).void >>
      coll.update(
        $doc("contestId" -> contestId, "manualAbsent" -> true, $or("kick" $eq true)),
        $set(
          "manualAbsent" -> false
        ),
        multi = true
      ).void

  def findNextNo(contestId: OffContest.ID): Fu[Int] = {
    coll.find(contestQuery(contestId), $doc("_id" -> false, "no" -> true))
      .sort($sort desc "no")
      .uno[Bdoc] map {
        _ flatMap { doc => doc.getAs[Int]("no") map (1 + _) } getOrElse 1
      }
  }

  def rounds(contest: OffContest): Fu[List[(OffPlayer, Map[OffRound.No, OffBoard])]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregateList(
      Match($doc("contestId" -> contest.id)),
      List(
        PipelineOperator(
          $doc(
            "$lookup" -> $doc(
              "from" -> Env.current.settings.CollectionBoard,
              "let" -> $doc("pno" -> "$no"),
              "pipeline" -> $arr(
                $doc(
                  "$match" -> $doc(
                    "$expr" -> $doc(
                      "$and" -> $arr(
                        $doc("$eq" -> $arr("$contestId", contest.id)),
                        $doc("$or" -> $arr(
                          $doc("$eq" -> $arr("$$pno", "$whitePlayer.no")),
                          $doc("$eq" -> $arr("$$pno", "$blackPlayer.no"))
                        ))
                      )
                    )
                  )
                )
              ),
              "as" -> "boards"
            )
          )
        )
      ),
      maxDocs = 1000
    ).map {
        _.flatMap { doc =>
          val result = for {
            player <- playerHandler.readOpt(doc)
            boards <- doc.getAs[List[OffBoard]]("boards")
            boardMap = boards.map { b =>
              b.roundNo -> b
            }.toMap
          } yield (player, boardMap)
          result
        }(breakOut).toList
      }
  }

  def setOutcomes(contestId: OffContest.ID, players: List[OffPlayer.No], outcome: OffBoard.Outcome, score: Double): Funit =
    coll.update(
      contestQuery(contestId) ++ $doc("no" -> $in(players: _*)),
      $inc(
        "score" -> score,
        "points" -> score
      ) ++ $push("outcomes" -> outcome.id),
      multi = true
    ).void

  def setNo(id: OffPlayer.ID, no: OffPlayer.No): Funit =
    coll.updateField($id(id), "no", no).void

  def update(player: OffPlayer): Funit = {
    coll.update(
      $id(player.id),
      player
    ).void
  }

  def externalExists(contestId: OffContest.ID, srcUsername: String): Fu[Boolean] =
    coll.exists($id(OffPlayer.makeId(contestId, OffPlayer.withExternal(srcUsername))))

  def contestQuery(contestId: OffContest.ID) =
    $doc("contestId" -> contestId)

}
