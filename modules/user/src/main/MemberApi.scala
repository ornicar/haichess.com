package lila.user

import lila.db.dsl._
import User.{ BSONFields => F }
import lila.hub.actorApi.member._
import org.joda.time.{ Days, DateTime }

final class MemberApi(coll: Coll, lightUserApi: LightUserApi, bus: lila.common.Bus) {

  import Member.MemberBSONHandler

  def buyPayed(data: MemberBuyPayed): Funit = {
    UserRepo.byId(data.userId) flatMap {
      case None => fufail(s"can not find member buy order user ${data.userId} of ${data.orderId}")
      case Some(u1) => {
        val level = MemberLevel(data.level)
        val oldMember = u1.memberOrDefault
        val pointsDiff = data.points.map(_ * -1) | 0
        val newMember = oldMember.merge(level, data.days, pointsDiff, 0)

        val oldExpireAt = oldMember.levels.get(data.level).map(_.expireAt)
        val newExpireAt = newMember.levels.get(data.level).map(_.expireAt)
        coll.update(
          $id(data.userId),
          $set(
            F.member -> newMember
          )
        ) >>- (pointsDiff != 0).?? {
            bus.publish(MemberPointsChange(u1.id, "orderPay", pointsDiff, data.orderId.some), 'memberPointsChange)
          } >>- lightUserApi.invalidate(data.userId) >>
          data.inviteUser.?? { userId =>
            UserRepo.byId(userId) flatMap {
              case None => fufail(s"can not find member buy order inviteUser $userId of ${data.orderId}")
              case Some(u2) => {
                val pointsDiff = (data.payAmount / 2).toInt
                coll.update(
                  $id(userId),
                  $set(
                    F.member -> u2.memberOrDefault.mergePoints(pointsDiff)
                  )
                ).void >>- bus.publish(MemberPointsChange(u2.id, "orderPayRebate", pointsDiff, data.orderId.some), 'memberPointsChange)
              }
            }
          } >>- bus.publish(MemberLevelChange(data.userId, "buy", data.level, oldExpireAt, newExpireAt, data.desc, data.orderId.some, none), 'memberLevelChange) >>-
          bus.publish(MemberBuyComplete(data.orderId, data.userId), 'memberBuyComplete)
      }
    }
  }

  def cardUse(data: MemberCardUse): Funit = {
    UserRepo.byId(data.userId) flatMap {
      case None => fufail(s"can not find user ${data.userId}")
      case Some(u1) => {
        val level = MemberLevel(data.level)
        val oldMember = u1.memberOrDefault
        val newMember = oldMember.mergeLevel(level, data.days)

        val oldExpireAt = oldMember.levels.get(data.level).map(_.expireAt)
        val newExpireAt = newMember.levels.get(data.level).map(_.expireAt)
        coll.update(
          $id(data.userId),
          $set(
            F.member -> newMember
          )
        ).void >>- bus.publish(MemberLevelChange(data.userId, "card", data.level, oldExpireAt, newExpireAt, data.desc, none, none), 'memberLevelChange) >>- lightUserApi.invalidate(data.userId)
      }
    }
  }

  def setPoints(data: MemberPointsSet): Funit =
    UserRepo.byId(data.userId) flatMap {
      case None => fufail(s"can not find user ${data.userId} of ${data.orderId}")
      case Some(u) => {
        val pointsDiff = data.diff
        val oldMember = u.memberOrDefault
        val newMember = oldMember.mergePoints(pointsDiff)
        coll.update(
          $id(u.id),
          $set(
            F.member -> newMember
          )
        ).void >>- bus.publish(MemberPointsChange(u.id, "orderPay", pointsDiff, data.orderId), 'memberPointsChange)
      }
    }

  def signup(u: User): Funit = {
    coll.update(
      $id(u.id),
      $set(
        F.member -> u.memberOrDefault
      )
    ).void
  }

  def expiredTesting(): Funit = {
    // 拥有黄金会员（已到期） && code=黄金会员 && 拥有白银会员（未到期） => 更新为白银会员
    UserRepo.goldExpiredAndSilverNotExpired().flatMap { ids =>
      coll.update(
        $inIds(ids),
        $set(F.mcode -> MemberLevel.Silver.code),
        multi = true
      ).void >>- ids.foreach(lightUserApi.invalidate)
    } >> {
      // 拥有黄金会员（已到期） && code=黄金会员 && （不拥有白银会员 或 已到期） => 更新为注册会员
      UserRepo.goldExpiredAndSilverExpired().flatMap { ids =>
        coll.update(
          $inIds(ids),
          $set(F.mcode -> MemberLevel.General.code),
          multi = true
        ).void >>- ids.foreach(lightUserApi.invalidate)
      }
    } >> {
      // 拥有白银会员（已到期）&& code=白银会员 => 更新为注册会员
      UserRepo.silverExpired() flatMap { ids =>
        coll.update(
          $inIds(ids),
          $set(F.mcode -> MemberLevel.General.code),
          multi = true
        ).void >>- ids.foreach(lightUserApi.invalidate)
      }
    }
  }

}
