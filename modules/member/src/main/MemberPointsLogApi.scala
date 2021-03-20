package lila.member

import lila.db.dsl._
import lila.user.User
import ornicar.scalalib.Random
import org.joda.time.DateTime

case class MemberPointsLogApi(coll: Coll) {

  import BSONHandlers.MemberPointsLogBSONHandler

  def byId(id: User.ID): Fu[Option[MemberPointsLog]] = coll.byId[MemberPointsLog](id)

  def setLog(
    userId: User.ID,
    typ: String,
    diff: Int,
    orderId: Option[String]
  ): Funit = (diff != 0) ?? coll.insert(MemberPointsLog(
    _id = Random nextString 8,
    typ = MemberPointsLog.PointsType(typ),
    userId = userId,
    diff = diff,
    createAt = DateTime.now,
    orderId: Option[String]
  )).void

  def mine(userId: User.ID): Fu[List[MemberPointsLog]] = coll.find(
    $doc("userId" -> userId)
  ).sort($sort desc "createAt").list()

}
