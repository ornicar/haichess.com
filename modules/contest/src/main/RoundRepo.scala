package lila.contest

import lila.contest.Round.ID
import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import reactivemongo.bson._

object RoundRepo {

  private[contest] lazy val coll = Env.current.roundColl

  import BSONHandlers._
  private[contest] val createdSelect = $doc("status" -> Round.Status.Created.id)
  private[contest] val pairingSelect = $doc("status" -> Round.Status.Pairing.id)
  private[contest] val publishedSelect = $doc("status" -> Round.Status.Published.id)
  private[contest] val startedSelect = $doc("status" -> Round.Status.Started.id)
  private[contest] val finishedSelect = $doc("status" -> Round.Status.Finished.id)

  def insert(round: Round): Funit = coll.insert(round).void

  def update(id: Round.ID, round: Round): Funit = coll.update($id(id), round).void

  def bulkUpdate(contestId: Contest.ID, roundList: List[Round]): Funit =
    removeByContest(contestId) >> bulkInsert(roundList).void

  def bulkInsert(roundList: List[Round]): Funit = coll.bulkInsert(
    documents = roundList.map(roundHandler.write).toStream,
    ordered = true
  ).void

  def find(contestId: Contest.ID, no: Round.No) = byId(Round.makeId(contestId, no))

  def byId(id: Round.ID): Fu[Option[Round]] =
    coll.byId[Round](id)

  def getByContest(contestId: Contest.ID): Fu[List[Round]] =
    coll.find(contestQuery(contestId)).list[Round]()

  def getNextRounds(contestId: Contest.ID, no: Round.No): Fu[List[Round]] =
    coll.find(contestQuery(contestId) ++ $doc("no" $gt no)).list[Round]()

  def byOrderedIds(ids: Seq[String]): Fu[List[Round]] =
    coll.byOrderedIds[Round, String](
      ids,
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def exists(id: Round.ID): Fu[Boolean] =
    coll.exists($id(id))

  def find(id: Round.ID): Fu[Option[Round]] =
    coll.uno[Round]($id(id))

  def createdById(id: Round.ID): Fu[Option[Round]] =
    coll.find($id(id) ++ createdSelect).uno[Round]

  def pairingById(id: Round.ID): Fu[Option[Round]] =
    coll.find($id(id) ++ pairingSelect).uno[Round]

  def publishedById(id: Round.ID): Fu[Option[Round]] =
    coll.find($id(id) ++ publishedSelect).uno[Round]

  def startedById(id: Round.ID): Fu[Option[Round]] =
    coll.find($id(id) ++ startedSelect).uno[Round]

  def finishedById(id: Round.ID): Fu[Option[Round]] =
    coll.find($id(id) ++ finishedSelect).uno[Round]

  def countByContest(contestId: Contest.ID): Fu[Int] =
    coll.countSel(contestQuery(contestId))

  def getByUser(userId: User.ID): Fu[List[Round]] =
    coll.find($doc("userId" -> userId)).list[Round]()

  def remove(id: Round.ID): Funit =
    coll.remove($id(id)).void

  def removeByContest(contestId: Contest.ID): Funit =
    coll.remove(contestQuery(contestId)).void

  def setBoards(id: Round.ID, boards: Int): Funit =
    coll.updateField($id(id), "boards", boards).void

  def setStatus(id: Round.ID, status: Round.Status): Funit =
    coll.update(
      $id(id),
      $set("status" -> status.id)
    ).void

  def allFinished(contestId: Contest.ID, round: Round.No): Fu[Boolean] =
    coll.exists(contestQuery(contestId) ++ $doc("status" $gte Round.Status.Finished.id, "no" -> round))

  // 找到过去20分钟内published的Round
  def published: Fu[List[Round]] =
    coll.find($doc("actualStartsAt" -> ($lte(DateTime.now) ++ $gte(DateTime.now minusMinutes 20))) ++ publishedSelect).list[Round]()

  def finish(id: Round.ID): Funit =
    coll.update(
      $id(id),
      $set(
        "status" -> Round.Status.Finished.id,
        "finishAt" -> DateTime.now
      )
    ).void

  def setStartsTime(id: ID, st: DateTime): Funit =
    coll.updateField($id(id), "actualStartsAt", st).void

  def contestQuery(contestId: Contest.ID) = $doc("contestId" -> contestId)

}
