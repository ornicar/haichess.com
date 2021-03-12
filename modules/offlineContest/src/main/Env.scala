package lila.offlineContest

import com.typesafe.config.Config
import lila.hub.DuctMap
import akka.actor._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem,
    roundMap: DuctMap[_],
    hub: lila.hub.Env,
    lightUserApi: lila.user.LightUserApi,
    historyApi: lila.history.HistoryApi,
    notifyApi: lila.notify.NotifyApi,
    clazzApi: lila.clazz.ClazzApi,
    scheduler: lila.common.Scheduler,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private implicit val sys = system
  private[offlineContest] val settings = new {
    val CollectionContest = config getString "collection.contest"
    val CollectionRound = config getString "collection.round"
    val CollectionBoard = config getString "collection.board"
    val CollectionPlayer = config getString "collection.player"
    val CollectionForbidden = config getString "collection.forbidden"
    val CollectionScoreSheet = config getString "collection.scoresheet"
    val CollectionImage = config getString "collection.image"
  }

  import settings._
  private[offlineContest] lazy val contestColl = db(CollectionContest)
  private[offlineContest] lazy val roundColl = db(CollectionRound)
  private[offlineContest] lazy val boardColl = db(CollectionBoard)
  private[offlineContest] lazy val playerColl = db(CollectionPlayer)
  private[offlineContest] lazy val forbiddenColl = db(CollectionForbidden)
  private[offlineContest] lazy val scoreSheetColl = db(CollectionScoreSheet)
  private[offlineContest] lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new lila.db.Photographer(imageColl, "offlineContest")

  lazy val pager = new OffContestPager()
  lazy val forms = new DataForm()
  lazy val api = new OffContestApi(
    system = system,
    notifyApi = notifyApi,
    clazzApi = clazzApi,
    photographer = photographer
  )

  private val trf = new OffSwissTrf()
  private val pairingSystem = new OffPairingSystem(trf)
  private val pairingDirector = new OffPairingDirector(pairingSystem)

  lazy val roundApi = new OffRoundApi(pairingDirector, notifyApi)

}

object Env {

  lazy val current: Env = "offlineContest" boot new Env(
    config = lila.common.PlayApp loadConfig "offlineContest",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    roundMap = lila.round.Env.current.roundMap,
    hub = lila.hub.Env.current,
    lightUserApi = lila.user.Env.current.lightUserApi,
    historyApi = lila.history.Env.current.api,
    notifyApi = lila.notify.Env.current.api,
    clazzApi = lila.clazz.Env.current.api,
    scheduler = lila.common.PlayApp.scheduler,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
