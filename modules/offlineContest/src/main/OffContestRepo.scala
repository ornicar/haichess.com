package lila.offlineContest

import lila.db.dsl._
import lila.user.User
import reactivemongo.api.ReadPreference

object OffContestRepo {

  private[offlineContest] lazy val coll = Env.current.contestColl

  private[offlineContest] val currentSelect = $doc("status" $in OffContest.Status.current.map(_.id))
  private[offlineContest] val historySelect = $doc("status" $in OffContest.Status.history.map(_.id))
  private[offlineContest] def statusSelect(status: OffContest.Status) = $doc("status" -> status.id)
  private[offlineContest] def createBySelect(userId: User.ID) = $doc("createdBy" -> userId)
  private[offlineContest] def idsSelect(ids: List[String]) = $doc("_id" $in ids)

  private[offlineContest] val createdSelect = $doc("status" -> OffContest.Status.Created.id)
  private[offlineContest] val startedSelect = $doc("status" -> OffContest.Status.Started.id)
  private[offlineContest] val finishedSelect = $doc("status" -> OffContest.Status.Finished.id)

  private[offlineContest] val createDesc = $doc("createdAt" -> -1)
  private[offlineContest] val startAsc = $doc("startsAt" -> 1)
  private[offlineContest] val startDesc = $doc("startsAt" -> -1)

  import BSONHandlers.contestHandler

  def insert(contest: OffContest): Funit = coll.insert(contest).void

  def update(contest: OffContest): Funit = coll.update($id(contest.id), contest).void

  def remove(id: OffContest.ID): Funit = coll.remove($id(id)).void

  def exists(id: OffContest.ID): Fu[Boolean] = coll exists $id(id)

  def nameExists(name: String, groupName: Option[String], id: Option[String]): Fu[Boolean] =
    coll.exists($doc("name" -> name) ++ groupName.??(g => $doc("groupName" -> g)) ++ id.??(i => $doc("_id" $ne i)))

  def byId(id: OffContest.ID): Fu[Option[OffContest]] = coll.byId(id)

  def byIds(ids: Iterable[String]): Fu[List[OffContest]] =
    coll.find($inIds(ids)).list[OffContest](none)

  def byOrderedIds(ids: Seq[String]): Fu[List[OffContest]] =
    coll.byOrderedIds[OffContest, String](
      ids,
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def createdById(id: OffContest.ID): Fu[Option[OffContest]] =
    coll.find($id(id) ++ createdSelect).uno[OffContest]

  def startedById(id: OffContest.ID): Fu[Option[OffContest]] =
    coll.find($id(id) ++ startedSelect).uno[OffContest]

  def finishedById(id: OffContest.ID): Fu[Option[OffContest]] =
    coll.find($id(id) ++ finishedSelect).uno[OffContest]

  def setStatus(id: OffContest.ID, status: OffContest.Status): Funit =
    coll.update(
      $id(id),
      $set("status" -> status.id)
    ).void

  def setCurrentRound(id: OffContest.ID, no: OffRound.No): Funit =
    coll.updateField($id(id), "currentRound", no).void

  def setPlayers(id: OffContest.ID, count: Int): Funit =
    coll.update($id(id), $set("nbPlayers" -> count)).void

  def incPlayers(id: OffContest.ID, by: Int): Funit =
    coll.update($id(id), $inc("nbPlayers" -> by)).void
}
