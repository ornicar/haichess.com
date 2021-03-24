package lila.team

import akka.actor._
import com.typesafe.config.Config
import lila.common.MaxPerPage
import lila.game.actorApi.FinishGame
import lila.hub.actorApi.offContest.OffContestRoundResult
import lila.mod.ModlogApi
import lila.notify.NotifyApi

final class Env(
    config: Config,
    rootConfig: Config,
    hub: lila.hub.Env,
    modLog: ModlogApi,
    notifyApi: NotifyApi,
    system: ActorSystem,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env
) {

  private val settings = new {
    val CollectionTeam = config getString "collection.team"
    val CollectionMember = config getString "collection.member"
    val CollectionRequest = config getString "collection.request"
    val CollectionInvite = config getString "collection.invite"
    val CollectionTag = config getString "collection.tag"
    val CollectionRating = config getString "collection.rating"
    val CollectionImage = config getString "collection.image"
    val AdminUid = rootConfig getString "net.admin_uid"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val PaginatorMaxUserPerPage = config getInt "paginator.max_user_per_page"
  }
  import settings._

  private[team] lazy val colls = new Colls(
    team = db(CollectionTeam),
    request = db(CollectionRequest),
    invite = db(CollectionInvite),
    member = db(CollectionMember),
    tag = db(CollectionTag),
    rating = db(CollectionRating)
  )

  lazy val forms = new DataForm(
    colls.team,
    captcherActor = hub.captcher,
    smsCaptcherActor = hub.smsCaptcher
  )

  lazy val memberStream = new TeamMemberStream(colls.member)(system)

  private lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new lila.db.Photographer(imageColl, "team")

  lazy val api = new TeamApi(
    coll = colls,
    cached = cached,
    notifier = notifier,
    bus = system.lilaBus,
    indexer = hub.teamSearch,
    timeline = hub.timeline,
    modLog = modLog,
    photographer = photographer,
    contestActor = hub.contest,
    adminUid = AdminUid
  )

  lazy val certificationApi = new CertificationApi(
    coll = colls,
    notifier = notifier,
    indexer = hub.teamSearch,
    adminUid = AdminUid,
    hub = hub
  )

  lazy val paginator = new PaginatorBuilder(
    coll = colls,
    maxPerPage = MaxPerPage(PaginatorMaxPerPage),
    maxUserPerPage = MaxPerPage(PaginatorMaxUserPerPage)
  )

  lazy val cli = new Cli(api, colls)

  lazy val cached = new Cached(asyncCache)(system)

  private lazy val notifier = new Notifier(notifyApi = notifyApi)

  system.lilaBus.subscribeFun('shadowban) {
    case lila.hub.actorApi.mod.Shadowban(userId, true) => api deleteRequestsByUserId userId
  }

  system.lilaBus.subscribeFun('finishGame, 'offContestRoundResult) {
    case FinishGame(game, white, black) =>
      if (game.nonAi && game.hasClock) {
        api.updateOnlineRating(game, white, black)
      }
    case result: OffContestRoundResult => if (result.teamRated) {
      api.updateOfflineRating(result)
    }
  }
}

object Env {

  lazy val current = "team" boot new Env(
    config = lila.common.PlayApp loadConfig "team",
    rootConfig = lila.common.PlayApp.loadConfig,
    hub = lila.hub.Env.current,
    modLog = lila.mod.Env.current.logApi,
    notifyApi = lila.notify.Env.current.api,
    system = lila.common.PlayApp.system,
    asyncCache = lila.memo.Env.current.asyncCache,
    db = lila.db.Env.current
  )
}
