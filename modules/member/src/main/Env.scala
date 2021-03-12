package lila.member

import akka.actor.ActorSystem
import com.typesafe.config.Config
import lila.notify.NotifyApi
import scala.concurrent.duration._
import lila.hub.actorApi.member.{ MemberBuyComplete, MemberLevelChange, MemberPointsChange }
import lila.hub.actorApi.puzzle._

final class Env(
    config: Config,
    rootConfig: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    notifyApi: NotifyApi,
    system: ActorSystem
) {

  val bus = hub.bus

  private val payTest = rootConfig getBoolean "alipay.test"
  private val appId = rootConfig getString "alipay.appId"
  private val sellerId = rootConfig getString "alipay.sellerId"
  private val CollectionOrder = config getString "collection.member_order"
  private val CollectionCard = config getString "collection.member_card"
  private val CollectionOrderStatusLog = config getString "collection.member_order_statuslog"
  private val CollectionOrderPay = config getString "collection.member_order_pay"
  private val CollectionMemberPointsLog = config getString "collection.member_points_log"
  private val CollectionMemberLevelLog = config getString "collection.member_level_log"
  private val CollectionMemberCardLog = config getString "collection.member_card_log"
  private val CollectionMemberCardStatusLog = config getString "collection.member_card_statuslog"
  private val CollectionMemberActive = config getString "collection.member_active"

  lazy val form = new DataForm

  lazy val orderStatusLogApi = OrderStatusLogApi(
    db(CollectionOrderStatusLog)
  )

  lazy val memberPointsLogApi = MemberPointsLogApi(
    db(CollectionMemberPointsLog)
  )

  lazy val memberLevelLogApi = MemberLevelLogApi(
    db(CollectionMemberLevelLog)
  )

  lazy val memberCardLogApi = MemberCardLogApi(
    db(CollectionMemberCardLog)
  )

  lazy val memberCardStatusLogApi = MemberCardStatusLogApi(
    db(CollectionMemberCardStatusLog)
  )

  lazy val memberCardApi = MemberCardApi(
    db(CollectionCard),
    memberCardLogApi,
    memberCardStatusLogApi,
    bus,
    notifyApi
  )

  lazy val orderPayApi = OrderPayApi(
    db(CollectionOrderPay)
  )

  lazy val orderApi = OrderApi(
    db(CollectionOrder),
    orderStatusLogApi,
    orderPayApi,
    memberCardApi,
    payTest,
    appId,
    sellerId,
    bus,
    notifyApi
  )

  lazy val memberActiveRecordApi = MemberActiveRecordApi(db(CollectionMemberActive))

  bus.subscribeFun('nextPuzzle, 'nextThemePuzzle, 'startPuzzleRush) {
    case NextPuzzle(_, userId) => memberActiveRecordApi.updateRecord(userId, puzzle = true)
    case NextThemePuzzle(_, userId) => memberActiveRecordApi.updateRecord(userId, themePuzzle = true)
    case StartPuzzleRush(_, userId) => memberActiveRecordApi.updateRecord(userId, puzzleRush = true)
  }

  bus.subscribeFun('memberLevelChange, 'memberPointsChange, 'memberBuyComplete) {
    case MemberLevelChange(userId, typ, level, oldExpireAt, newExpireAt, desc, orderId, cardId) =>
      memberLevelLogApi.setLog(userId, typ, level, oldExpireAt, newExpireAt, desc, orderId, cardId)
    case MemberPointsChange(userId, typ, pointsDiff, orderId) =>
      memberPointsLogApi.setLog(userId, typ, pointsDiff, orderId)
    case MemberBuyComplete(orderId, _) =>
      orderApi.member.completed(orderId)
  }

  bus.subscribeFun('userSignup) {
    case lila.security.Signup(user, _, _, _, _) => memberCardApi.addCardBySignup(user.id)
  }

  system.scheduler.schedule(1 minute, 1 minute) {
    memberCardApi.expiredTesting
    orderApi.expiredTesting
  }

}

object Env {

  lazy val current: Env = "member" boot new Env(
    config = lila.common.PlayApp loadConfig "member",
    rootConfig = lila.common.PlayApp.loadConfig,
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    notifyApi = lila.notify.Env.current.api,
    system = lila.common.PlayApp.system
  )
}
