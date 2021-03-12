package lila.contest

import lila.db.dsl._
import lila.user.User

object InviteRepo {

  private[contest] lazy val coll = Env.current.inviteColl

  import BSONHandlers._

  def insert(invite: Invite): Funit = coll.insert(invite).void

  def byId(id: Invite.ID): Fu[Option[Invite]] = coll.byId[Invite](id)

  def find(contestId: Contest.ID, userId: User.ID): Fu[Option[Invite]] =
    byId(Invite.makeId(contestId, userId))

  def exists(contestId: Contest.ID, userId: User.ID): Fu[Boolean] =
    coll.exists(
      $id(Invite.makeId(contestId, userId))
    )

  def getByContest(contestId: Contest.ID): Fu[List[Invite]] =
    coll.find(contestQuery(contestId)).sort($doc("status" -> 1, "date" -> -1)).list[Invite]()

  def setStatus(id: Invite.ID, status: Invite.InviteStatus): Funit =
    coll.update(
      $id(id),
      $set("status" -> status.id)
    ).void

  def remove(id: Invite.ID): Funit = coll.remove($id(id)).void

  def contestQuery(contestId: Contest.ID) = $doc("contestId" -> contestId)

}
