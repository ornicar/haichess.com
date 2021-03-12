package lila.offlineContest

import lila.db.dsl._
import lila.user.User
import reactivemongo.api.ReadPreference
import reactivemongo.bson.BSONString

object OffScoreSheetRepo {

  private[offlineContest] lazy val coll = Env.current.scoreSheetColl

  import BSONHandlers.scoreSheetHandler

  def byId(id: OffScoreSheet.ID): Fu[Option[OffScoreSheet]] =
    coll.byId[OffScoreSheet](id)

  def bulkInsert(contestId: OffContest.ID, scoreSheetList: List[OffScoreSheet]): Funit =
    coll.bulkInsert(
      documents = scoreSheetList.map(scoreSheetHandler.write).toStream,
      ordered = true
    ).void

  def getByRound(contestId: OffContest.ID, no: OffRound.No): Fu[List[OffScoreSheet]] =
    coll.find(roundQuery(contestId, no)).sort($sort asc "rank").list[OffScoreSheet]()

  def getByContest(contestId: OffContest.ID): Fu[List[OffScoreSheet]] =
    coll.find(contestQuery(contestId)).sort($sort asc "rank").list[OffScoreSheet]()

  def setRank(id: OffScoreSheet.ID, rank: Int): Funit =
    coll.updateField($id(id), "rank", rank).void

  def findChampion(ids: List[OffContest.ID]): Fu[List[(OffContest.ID, User.ID)]] = {
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

  def roundQuery(contestId: OffContest.ID, no: OffRound.No) = $doc("contestId" -> contestId, "roundNo" -> no)
  def contestQuery(contestId: OffContest.ID) = $doc("contestId" -> contestId)
}
