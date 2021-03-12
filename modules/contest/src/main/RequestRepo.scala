package lila.contest

import lila.db.dsl._
import lila.user.User

object RequestRepo {

  private[contest] lazy val coll = Env.current.requestColl

  import BSONHandlers._

  def insert(request: Request): Funit = coll.insert(request).void

  def byId(id: Request.ID): Fu[Option[Request]] = coll.byId[Request](id)

  def exists(contestId: Contest.ID, userId: User.ID): Fu[Boolean] =
    coll.exists(uniqueQuery(contestId, userId))

  def find(contestId: Contest.ID, userId: User.ID): Fu[Option[Request]] =
    coll.uno[Request](uniqueQuery(contestId, userId))

  def getByContest(contestId: Contest.ID): Fu[List[Request]] =
    coll.find(contestQuery(contestId)).sort($doc("status" -> 1, "date" -> -1)).list[Request]()

  def countByContest(contestId: Contest.ID): Fu[Int] =
    coll.countSel(contestQuery(contestId))

  def getByUserId(userId: User.ID): Fu[List[Request]] =
    coll.find($doc("userId" -> userId)).list[Request]()

  def remove(id: Request.ID) =
    coll.remove($id(id)).void

  def setStatus(id: Request.ID, status: Request.RequestStatus): Funit =
    coll.update(
      $id(id),
      $set("status" -> status.id)
    ).void

  def uniqueQuery(contestId: Contest.ID, userId: User.ID) = $doc("contestId" -> contestId, "userId" -> userId)
  def contestQuery(contestId: Contest.ID) = $doc("contestId" -> contestId)

}
