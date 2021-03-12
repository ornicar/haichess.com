package lila.offlineContest

import lila.db.dsl._
import reactivemongo.bson._

object OffForbiddenRepo {

  private[offlineContest] lazy val coll = Env.current.forbiddenColl
  import BSONHandlers.forbiddenHandler

  def byId(id: String): Fu[Option[OffForbidden]] = coll.byId[OffForbidden](id)

  def getByContest(contestId: OffContest.ID): Fu[List[OffForbidden]] =
    coll.find(contestQuery(contestId)).list[OffForbidden]()

  def upsert(forbidden: OffForbidden): Funit = coll.update($id(forbidden.id), forbidden, upsert = true).void

  def remove(forbidden: OffForbidden): Funit = coll.remove($id(forbidden.id)).void

  def contestQuery(contestId: OffContest.ID) = $doc("contestId" -> contestId)

}
