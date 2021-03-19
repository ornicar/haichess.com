package lila.contest

import com.typesafe.config.Config
import lila.common.{AtMost, Every, ResilientScheduler}
import scala.concurrent.duration._
import lila.hub.{Duct, DuctMap}
import akka.actor._
import lila.hub.actorApi.contest.{GetContestBoard, ContestBoard}

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
  private[contest] val settings = new {
    val CollectionContest = config getString "collection.contest"
    val CollectionRound = config getString "collection.round"
    val CollectionBoard = config getString "collection.board"
    val CollectionScoreSheet = config getString "collection.scoresheet"
    val CollectionPlayer = config getString "collection.player"
    val CollectionRequest = config getString "collection.request"
    val CollectionInvite = config getString "collection.invite"
    val CollectionForbidden = config getString "collection.forbidden"
    val CollectionImage = config getString "collection.image"
    val CollectionFile = config getString "collection.file"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val ApiActorName = config getString "api_actor.name"
    val SequencerTimeout = config duration "sequencer.timeout"
    val NetBaseUrl = config getString "net.base_url"
    val ActorName = config getString "actor.name"
  }

  import settings._
  private[contest] lazy val contestColl = db(CollectionContest)
  private[contest] lazy val roundColl = db(CollectionRound)
  private[contest] lazy val boardColl = db(CollectionBoard)
  private[contest] lazy val scoreSheetColl = db(CollectionScoreSheet)
  private[contest] lazy val playerColl = db(CollectionPlayer)
  private[contest] lazy val requestColl = db(CollectionRequest)
  private[contest] lazy val inviteColl = db(CollectionInvite)
  private[contest] lazy val forbiddenColl = db(CollectionForbidden)
  private[contest] lazy val imageColl = db(CollectionImage)
  private[contest] lazy val fileColl = db(CollectionFile)

  lazy val pager = new ContestPager
  lazy val forms = new DataForm(captcher = hub.captcher)
  lazy val verify = new Condition.Verify(historyApi)
  lazy val cached = new Cached

  private lazy val photographer = new lila.db.Photographer(imageColl, "contest")
  private lazy val fileUploader = new lila.db.FileUploader(fileColl, "contest")

  private val contestSequencerMap = new DuctMap(
    mkDuct = _ => Duct.extra.lazyFu(5.seconds)(system),
    accessTimeout = SequencerTimeout
  )

  private val roundSequencerMap = new DuctMap(
    mkDuct = _ => Duct.extra.lazyFu(5.seconds)(system),
    accessTimeout = SequencerTimeout
  )

  private val trf: SwissTrf = new SwissTrf(NetBaseUrl)
  private val pairingSystem = new PairingSystem(trf)
  private val pairingDirector = new PairingDirector(pairingSystem)

  lazy val roundApi = new RoundApi(
    system = system,
    sequencers = roundSequencerMap,
    roundMap = roundMap,
    notifyApi = notifyApi,
    pairingDirector = pairingDirector
  )

  lazy val contestApi = new ContestApi(
    system = system,
    sequencers = contestSequencerMap,
    renderer = hub.renderer,
    timeline = hub.timeline,
    verify = verify,
    notifyApi = notifyApi,
    clazzApi = clazzApi,
    roundApi = roundApi,
    roundMap = roundMap,
    photographer = photographer,
    fileUploader = fileUploader,
    reminder = new ContestReminder(system.lilaBus),
    asyncCache = asyncCache
  )

  system.actorOf(Props(new Actor {
    def receive = {
      case GetContestBoard(gameId: String) => {
        sender ! {
          contestApi.fullBoardInfo(gameId) map {
            _.map { c =>
              ContestBoard(c.contest.id, c.contest.fullName, c.contest.teamRated, c.round.no)
            }
          }
        }
      }
    }
  }), name = ActorName)

  // 报名截止
  ResilientScheduler(
    every = Every(3 seconds),
    atMost = AtMost(30 seconds),
    logger = logger,
    initialDelay = 30 seconds
  ) { contestApi.enterStop }(system)

  // 比赛开始
  ResilientScheduler(
    every = Every(1 seconds),
    atMost = AtMost(30 seconds),
    logger = logger,
    initialDelay = 30 seconds
  ) { contestApi.start }(system)

  // Round开始
  ResilientScheduler(
    every = Every(1 seconds),
    atMost = AtMost(30 seconds),
    logger = logger,
    initialDelay = 31 seconds
  ) { roundApi.start }(system)

  // Game开始
  ResilientScheduler(
    every = Every(1 seconds),
    atMost = AtMost(30 seconds),
    logger = logger,
    initialDelay = 32 seconds
  ) { contestApi.launch }(system)

  // Game开始-通知
  ResilientScheduler(
    every = Every(10 seconds),
    atMost = AtMost(30 seconds),
    logger = logger,
    initialDelay = 30 seconds
  ) { contestApi.remind }(system)

  system.lilaBus.subscribeFun(
    'finishGame,
    'apptCompleteBus
  ) {
      case lila.game.actorApi.FinishGame(game, _, _) => contestApi finishGame game
      case lila.hub.actorApi.game.ApptCompleteBus(gameId, user, time, source) => if (source == "contest") { roundApi.apptComplete(gameId, time) }
    }

}

object Env {

  lazy val current: Env = "contest" boot new Env(
    config = lila.common.PlayApp loadConfig "contest",
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
