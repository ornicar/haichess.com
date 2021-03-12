package lila.contest

import org.joda.time.DateTime
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.user.User
import reactivemongo.api.ReadPreference

object ContestRepo {

  private[contest] lazy val coll = Env.current.contestColl

  import BSONHandlers._

  private[contest] def statusSelect(status: Contest.Status) = $doc("status" -> status.id)
  private[contest] def createBySelect(userId: User.ID) = $doc("createdBy" -> userId)
  private[contest] def idsSelect(ids: List[String]) = $doc("_id" $in ids)
  private[contest] def allPublishedSelect(aheadMinutes: Int) = publishedSelect ++
    $doc("startsAt" $lt (DateTime.now plusMinutes aheadMinutes))

  private[contest] val createdSelect = $doc("status" -> Contest.Status.Created.id)
  private[contest] val publishedSelect = $doc("status" -> Contest.Status.Published.id)
  private[contest] val enterStoppedSelect = $doc("status" -> Contest.Status.EnterStopped.id)
  private[contest] val startedSelect = $doc("status" -> Contest.Status.Started.id)
  private[contest] val finishedSelect = $doc("status" -> Contest.Status.Finished.id)

  private[contest] val allSelect = $doc("status" $in Contest.Status.all2.map(_.id))
  private[contest] val belongSelect = $doc("status" $in Contest.Status.belong.map(_.id))
  private[contest] val ownerSelect = $doc("status" $in Contest.Status.owner.map(_.id))
  private[contest] val finishedOrCancelSelect = $doc("status" $in Contest.Status.finish.map(_.id))
  private[contest] val createDesc = $doc("createdAt" -> -1)
  private[contest] val startAsc = $doc("startsAt" -> 1)
  private[contest] val startDesc = $doc("startsAt" -> -1)

  def insert(tour: Contest) = coll.insert(tour)

  def update(tour: Contest) = coll.update($id(tour.id), tour)

  def remove(id: Contest.ID) = coll.remove($id(id))

  def exists(id: Contest.ID) = coll exists $id(id)

  def nameExists(name: String, groupName: Option[String], id: Option[String]) =
    coll.exists($doc("name" -> name) ++ groupName.??(g => $doc("groupName" -> g)) ++ id.??(i => $doc("_id" $ne i)))

  def byId(id: Contest.ID): Fu[Option[Contest]] = coll.find($id(id)).uno[Contest]

  def byIds(ids: Iterable[String]): Fu[List[Contest]] =
    coll.find($inIds(ids)).list[Contest](none)

  def byOrderedIds(ids: Seq[String]): Fu[List[Contest]] =
    coll.byOrderedIds[Contest, String](
      ids,
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def createdById(id: Contest.ID): Fu[Option[Contest]] =
    coll.find($id(id) ++ createdSelect).uno[Contest]

  def publishedById(id: Contest.ID): Fu[Option[Contest]] =
    coll.find($id(id) ++ publishedSelect).uno[Contest]

  def enterStoppedById(id: Contest.ID): Fu[Option[Contest]] =
    coll.find($id(id) ++ enterStoppedSelect).uno[Contest]

  def startedById(id: Contest.ID): Fu[Option[Contest]] =
    coll.find($id(id) ++ startedSelect).uno[Contest]

  def finishedById(id: Contest.ID): Fu[Option[Contest]] =
    coll.find($id(id) ++ finishedSelect).uno[Contest]

  def published: Fu[List[Contest]] =
    coll.find($doc("deadlineAt" -> ($lte(DateTime.now) ++ $gte(DateTime.now minusMinutes 20))) ++ publishedSelect).list[Contest]()

  def enterStopped: Fu[List[Contest]] =
    coll.find($doc("startsAt" -> ($lte(DateTime.now) ++ $gte(DateTime.now minusMinutes 20))) ++ enterStoppedSelect).list[Contest]()

  def setStatus(id: Contest.ID, status: Contest.Status): Funit =
    coll.update(
      $id(id),
      $set("status" -> status.id)
    ).void

  def setCurrentRound(id: Contest.ID, no: Round.No): Funit =
    coll.updateField($id(id), "currentRound", no).void

  def setAllRoundFinished(id: Contest.ID): Funit =
    coll.updateField($id(id), "allRoundFinished", true).void

  def incPlayers(id: Contest.ID, by: Int): Funit =
    coll.update($id(id), $inc("nbPlayers" -> by)).void

  def setAutoPairing(id: Contest.ID, autoPairing: Boolean): Funit =
    coll.update(
      $id(id),
      $set("autoPairing" -> autoPairing)
    ).void

  def finish(contest: Contest): Funit =
    coll.update(
      $id(contest.id),
      $set(
        "status" -> Contest.Status.Finished.id,
        "realFinishAt" -> DateTime.now
      )
    ).void

  def visibleInTeam(teamId: String, nb: Int): Fu[List[Contest]] =
    coll.find($doc("typ" -> "team-inner", "organizer" -> teamId)).sort(startDesc).list[Contest](nb)

  def findRecently(nb: Int): Fu[List[Contest]] =
    coll.find(
      $doc(
        "status" -> Contest.Status.Finished.id,
        "typ" -> Contest.Type.Public.id
      )
    ).sort(startDesc).list[Contest](nb)

}
