package lila.coach

import lila.security.Permission
import akka.actor._
import com.typesafe.config.Config
import com.alipay.easysdk.factory.Factory

final class Env(
    config: Config,
    rootConfig: Config,
    notifyApi: lila.notify.NotifyApi,
    hub: lila.hub.Env,
    system: ActorSystem,
    db: lila.db.Env
) {

  private val CollectionCoach = config getString "collection.coach"
  private val CollectionImage = config getString "collection.image"
  private val CollectionCoachStudent = config getString "collection.student"
  private val AdminUid = rootConfig getString "net.admin_uid"

  private lazy val coachColl = db(CollectionCoach)
  private lazy val imageColl = db(CollectionImage)
  private lazy val studentColl = db(CollectionCoachStudent)

  lazy val photographer = new lila.db.Photographer(imageColl, "coach")

  lazy val pager = new CoachPager(coachColl)

  lazy val certifyForm = new CoachCertifyForm(hub.smsCaptcher)

  lazy val certifyApi = new CoachCertifyApi(
    bus = system.lilaBus,
    coachColl = coachColl,
    notifyApi = notifyApi,
    adminUid = AdminUid
  )

  lazy val api = new CoachApi(coachColl, photographer)

  lazy val studentApi = new StudentApi(studentColl, notifyApi)

  system.lilaBus.subscribeFun('adjustCheater, 'userActive, 'finishGame, 'shadowban, 'setPermissions, 'clazzJoinAccept) {
    case lila.user.User.Active(user) if !user.seenRecently => api setSeenAt user
    case lila.hub.actorApi.mod.Shadowban(userId, true) =>
      certifyApi.toggleQualifyApproved(userId, false)
    case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
      certifyApi.toggleQualifyApproved(userId, false)
    case lila.hub.actorApi.mod.SetPermissions(userId, oldPermissions, newPermissions) =>
      if (oldPermissions.has(Permission.Coach.name) && !newPermissions.has(Permission.Coach.name)) {
        certifyApi.toggleQualifyApproved(userId, false)
      } else if (!oldPermissions.has(Permission.Coach.name) && newPermissions.has(Permission.Coach.name)) {
        certifyApi.toggleQualifyApproved(userId, true)
      }
    case lila.game.actorApi.FinishGame(game, white, black) if game.rated =>
      if (game.perfType.exists(lila.rating.PerfType.standard.contains)) {
        white ?? api.setRating
        black ?? api.setRating
      }
    case lila.hub.actorApi.clazz.ClazzJoinAccept(_, _, coachId, studentId) => studentApi.join(coachId, studentId)
  }

  def cli = new lila.common.Cli {
    def process = {
      case "coach" :: "enable" :: username :: Nil => certifyApi.toggleQualifyApproved(username, true)
      case "coach" :: "disable" :: username :: Nil => certifyApi.toggleQualifyApproved(username, false)
    }
  }
}

object Env {

  lazy val current: Env = "coach" boot new Env(
    config = lila.common.PlayApp loadConfig "coach",
    rootConfig = lila.common.PlayApp.loadConfig,
    notifyApi = lila.notify.Env.current.api,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current
  )
}
