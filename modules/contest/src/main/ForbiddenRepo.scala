package lila.contest

import lila.db.dsl._
import reactivemongo.bson._

object ForbiddenRepo {

  private[contest] lazy val coll = Env.current.forbiddenColl

  import BSONHandlers.forbiddenHandler

  def byId(id: String): Fu[Option[Forbidden]] = coll.byId[Forbidden](id)

  def getByContest(contestId: Contest.ID): Fu[List[Forbidden]] =
    coll.find(contestQuery(contestId)).list[Forbidden]()

  def upsert(forbidden: Forbidden): Funit = coll.update($id(forbidden.id), forbidden, upsert = true).void

  def remove(forbidden: Forbidden): Funit = coll.remove($id(forbidden.id)).void

  def contestQuery(contestId: Contest.ID) = $doc("contestId" -> contestId)

}
