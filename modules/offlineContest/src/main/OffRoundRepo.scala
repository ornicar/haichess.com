package lila.offlineContest

import lila.db.dsl._
import lila.user.User
import reactivemongo.bson._
import reactivemongo.api.ReadPreference

object OffRoundRepo {

  private[offlineContest] lazy val coll = Env.current.roundColl

  import BSONHandlers.roundHandler
  private[offlineContest] val createdSelect = $doc("status" -> OffRound.Status.Created.id)
  private[offlineContest] val pairingSelect = $doc("status" -> OffRound.Status.Pairing.id)
  private[offlineContest] val publishedSelect = $doc("status" -> OffRound.Status.Published.id)
  private[offlineContest] val publishResultSelect = $doc("status" -> OffRound.Status.PublishResult.id)

  def insert(round: OffRound): Funit = coll.insert(round).void

  def update(id: OffRound.ID, round: OffRound): Funit = coll.update($id(id), round).void

  def bulkInsert(roundList: List[OffRound]): Funit = coll.bulkInsert(
    documents = roundList.map(roundHandler.write).toStream,
    ordered = true
  ).void

  def bulkUpdate(contestId: OffContest.ID, roundList: List[OffRound]): Funit =
    removeByContest(contestId) >> bulkInsert(roundList).void

  def find(contestId: OffContest.ID, no: OffRound.No): Fu[Option[OffRound]] =
    byId(OffRound.makeId(contestId, no))

  def byId(id: OffRound.ID): Fu[Option[OffRound]] =
    coll.byId[OffRound](id)

  def getByContest(contestId: OffContest.ID): Fu[List[OffRound]] =
    coll.find(contestQuery(contestId)).list[OffRound]()

  def getNextRounds(contestId: OffContest.ID, no: OffRound.No): Fu[List[OffRound]] =
    coll.find(contestQuery(contestId) ++ $doc("no" $gt no)).list[OffRound]()

  def byOrderedIds(ids: Seq[String]): Fu[List[OffRound]] =
    coll.byOrderedIds[OffRound, String](
      ids,
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def exists(id: OffRound.ID): Fu[Boolean] =
    coll.exists($id(id))

  def find(id: OffRound.ID): Fu[Option[OffRound]] =
    coll.uno[OffRound]($id(id))

  def createdById(id: OffRound.ID): Fu[Option[OffRound]] =
    coll.find($id(id) ++ createdSelect).uno[OffRound]

  def pairingById(id: OffRound.ID): Fu[Option[OffRound]] =
    coll.find($id(id) ++ pairingSelect).uno[OffRound]

  def publishedById(id: OffRound.ID): Fu[Option[OffRound]] =
    coll.find($id(id) ++ publishedSelect).uno[OffRound]

  def publishResultById(id: OffRound.ID): Fu[Option[OffRound]] =
    coll.find($id(id) ++ publishResultSelect).uno[OffRound]

  def countByContest(contestId: OffContest.ID): Fu[Int] =
    coll.countSel(contestQuery(contestId))

  def getByUser(userId: User.ID): Fu[List[OffRound]] =
    coll.find($doc("userId" -> userId)).list[OffRound]()

  def remove(id: OffRound.ID): Funit =
    coll.remove($id(id)).void

  def removeByContest(contestId: OffContest.ID): Funit =
    coll.remove(contestQuery(contestId)).void

  def setBoards(id: OffRound.ID, boards: Int): Funit =
    coll.updateField($id(id), "boards", boards).void

  def setStatus(id: OffRound.ID, status: OffRound.Status): Funit =
    coll.update(
      $id(id),
      $set("status" -> status.id)
    ).void

  def contestQuery(contestId: OffContest.ID) = $doc("contestId" -> contestId)

}
