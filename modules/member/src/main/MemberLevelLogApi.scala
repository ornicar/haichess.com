package lila.member

import lila.db.dsl._
import lila.user.User
import ornicar.scalalib.Random
import org.joda.time.DateTime

case class MemberLevelLogApi(coll: Coll) {

  import BSONHandlers.MemberLevelLogBSONHandler

  def byId(id: User.ID): Fu[Option[MemberLevelLog]] = coll.byId[MemberLevelLog](id)

  def setLog(
    userId: String,
    typ: String,
    level: String,
    oldExpireAt: Option[DateTime],
    newExpireAt: Option[DateTime],
    desc: String,
    orderId: Option[String],
    cardId: Option[String]
  ): Funit = coll.insert(
    MemberLevelLog(
      _id = Random nextString 8,
      typ = MemberLevelLog.MemberLevelChangeType(typ),
      userId = userId,
      level = lila.user.MemberLevel(level),
      oldExpireAt = oldExpireAt,
      newExpireAt = newExpireAt,
      createAt = DateTime.now,
      desc = desc,
      note = none,
      orderId = orderId,
      cardId = cardId
    )
  ).void

  def mine(userId: User.ID): Fu[List[MemberLevelLog]] = coll.find(
    $doc("userId" -> userId)
  ).sort($sort desc "createAt").list()

}
