package lila.contest

import lila.db.dsl._
import lila.user.User
import reactivemongo.api.ReadPreference
import reactivemongo.bson.BSONString

object ScoreSheetRepo {

  private[contest] lazy val coll = Env.current.scoreSheetColl

  import BSONHandlers._

  def byId(id: ScoreSheet.ID): Fu[Option[ScoreSheet]] =
    coll.byId[ScoreSheet](id)

  def insertMany(contestId: Contest.ID, scoreSheetList: List[ScoreSheet]): Funit =
    coll.bulkInsert(
      documents = scoreSheetList.map(scoreSheetHandler.write).toStream,
      ordered = true
    ).void

  def getByRound(contestId: Contest.ID, no: Round.No): Fu[List[ScoreSheet]] =
    coll.find(roundQuery(contestId, no)).sort($sort asc "rank").list[ScoreSheet]()

  def getByContest(contestId: Contest.ID): Fu[List[ScoreSheet]] =
    coll.find(contestQuery(contestId)).sort($sort asc "rank").list[ScoreSheet]()

  def setRank(id: ScoreSheet.ID, rank: Int): Funit =
    coll.updateField($id(id), "rank", rank).void

  def setCancelScore(scoreSheet: ScoreSheet): Funit =
    coll.update(
      $doc("playerUid" -> scoreSheet.playerUid),
      $set(
        "cancelled" -> true
      ),
      multi = true
    ).void

  def setCancelledRank(contestId: Contest.ID): Funit =
    coll.update(
      contestQuery(contestId) ++ $doc("cancelled" -> true),
      $set(
        "rank" -> 10000
      ),
      multi = true
    ).void

  def findChampion(ids: List[Contest.ID]): Fu[List[(Contest.ID, User.ID)]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregateList(
      Match($doc("rank" -> 1, "contestId" -> $in(ids: _*))),
      List(
        GroupField("contestId")("round" -> MaxField("roundNo"), "records" -> GroupFunction("$push", BSONString("$$ROOT"))),
        Project($doc(
          "records" -> $doc(
            "$filter" -> $doc(
              "input" -> BSONString("$records"),
              "as" -> BSONString("item"),
              "cond" -> $doc(
                "$eq" -> $arr(BSONString("$$item.roundNo"), BSONString("$round"))
              )
            )
          )
        )),
        UnwindField("records"),
        Project($doc(
          "userId" -> "$records.playerUid"
        ))
      ),
      maxDocs = 100,
      ReadPreference.secondaryPreferred
    ).map(_.flatMap { obj =>
        obj.getAs[String]("_id") flatMap { id =>
          obj.getAs[String]("userId") map {
            id -> _
          }
        }
      })
  }

  def roundQuery(contestId: Contest.ID, no: Round.No) = $doc("contestId" -> contestId, "roundNo" -> no)
  def contestQuery(contestId: Contest.ID) = $doc("contestId" -> contestId)
}
