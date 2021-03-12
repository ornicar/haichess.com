package lila.app

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    val scheduler: lila.common.Scheduler,
    val system: ActorSystem,
    appPath: String
) {

  private val RendererName = config getString "app.renderer.name"

  lazy val preloader = new mashup.Preload(
    tv = Env.tv.tv,
    leaderboard = Env.user.cached.topWeek,
    rushs = () => Env.puzzle.puzzleRushRankSeasonApi.seasonRankTop5,
    contests = () => Env.contest.contestApi.championTop5,
    studys = () => Env.study.api.likesTop5,
    tourneyWinners = Env.tournament.winners.all.map(_.top),
    timelineEntries = Env.timeline.entryApi.userEntries _,
    dailyPuzzle = tryDailyPuzzle,
    liveStreams = () => Env.streamer.liveStreamApi.all,
    countRounds = () => Env.round.count,
    lobbyApi = Env.api.lobbyApi,
    getPlayban = Env.playban.api.currentBan _,
    lightUserApi = Env.user.lightUserApi,
    roundProxyPov = Env.round.proxy.pov _,
    urgentGames = Env.round.proxy.urgentGames _
  )

  lazy val socialInfo = mashup.UserInfo.Social(
    relationApi = Env.relation.api,
    noteApi = Env.user.noteApi,
    prefApi = Env.pref.api
  ) _

  lazy val userNbGames = mashup.UserInfo.NbGames(
    crosstableApi = Env.game.crosstableApi,
    bookmarkApi = Env.bookmark.api,
    gameCached = Env.game.cached
  ) _

  lazy val userInfo = mashup.UserInfo(
    relationApi = Env.relation.api,
    trophyApi = Env.user.trophyApi,
    shieldApi = Env.tournament.shieldApi,
    revolutionApi = Env.tournament.revolutionApi,
    postApi = Env.forum.postApi,
    studyRepo = Env.study.studyRepo,
    getRatingChart = Env.history.ratingChartApi.apply,
    getRanks = Env.user.cached.rankingsOf,
    isHostingSimul = Env.simul.isHosting,
    fetchIsStreamer = Env.streamer.api.isStreamer,
    mineStudents = Env.coach.studentApi.mineStudents,
    fetchTeamIds = Env.team.cached.teamIdsList,
    fetchIsCoach = _ => fuccess(false),
    insightShare = Env.insight.share,
    getPlayTime = Env.game.playTime.apply,
    completionRate = Env.playban.api.completionRate
  ) _

  lazy val teamInfo = new mashup.TeamInfoApi(
    api = Env.team.api,
    getForumNbPosts = Env.forum.categApi.teamNbPosts _,
    getForumPosts = Env.forum.recent.team _,
    asyncCache = Env.memo.asyncCache
  )

  private val tryDailyPuzzle: lila.puzzle.Daily.Try = () =>
    scala.concurrent.Future {
      Env.puzzle.daily.get
    }.flatMap(identity).withTimeoutDefault(200 millis, none)(system) recover {
      case e: Exception =>
        lila.log("preloader").warn("daily puzzle", e)
        none
    }

  def closeAccount(userId: lila.user.User.ID, self: Boolean): Funit = for {
    user <- lila.user.UserRepo byId userId flatten s"No such user $userId"
    goodUser <- !user.lameOrTroll ?? { !Env.playban.api.hasCurrentBan(user.id) }
    _ <- lila.user.UserRepo.disable(user, keepEmail = !goodUser)
    _ = Env.user.onlineUserIdMemo.remove(user.id)
    _ = Env.user.recentTitledUserIdMemo.remove(user.id)
    following <- Env.relation.api fetchFollowing user.id
    _ <- !goodUser ?? Env.activity.write.unfollowAll(user, following)
    _ <- Env.relation.api.unfollowAll(user.id)
    _ <- Env.user.rankingApi.remove(user.id)
    _ <- Env.team.api.quitAll(user.id)
    _ = Env.challenge.api.removeByUserId(user.id)
    _ = Env.tournament.api.withdrawAll(user)
    _ <- Env.plan.api.cancel(user).nevermind
    _ <- Env.lobby.seekApi.removeByUser(user)
    _ <- Env.security.store.disconnect(user.id)
    _ <- Env.push.webSubscriptionApi.unsubscribeByUser(user)
    _ <- Env.streamer.api.demote(user.id)
    _ <- Env.coach.api.remove(user.id)
    reports <- Env.report.api.processAndGetBySuspect(lila.report.Suspect(user))
    _ <- self ?? Env.mod.logApi.selfCloseAccount(user.id, reports)
  } yield {
    system.lilaBus.publish(lila.hub.actorApi.security.CloseAccount(user.id), 'accountClose)
  }

  system.lilaBus.subscribeFun('garbageCollect) {
    case lila.hub.actorApi.security.GarbageCollect(userId, _) =>
      system.scheduler.scheduleOnce(1 second) {
        closeAccount(userId, self = false)
      }
  }

  system.actorOf(Props(new actor.Renderer), name = RendererName)

  lila.common.Chronometer.syncEffect(List(
    Env.socket,
    Env.site,
    Env.tournament,
    Env.lobby,
    Env.home,
    Env.game,
    Env.setup,
    Env.round,
    Env.team,
    Env.message,
    Env.timeline,
    Env.gameSearch,
    Env.teamSearch,
    Env.forumSearch,
    Env.relation,
    Env.report,
    Env.bookmark,
    Env.pref,
    Env.chat,
    Env.puzzle,
    Env.tv,
    Env.blog,
    Env.video,
    Env.playban, // required to load the actor
    Env.shutup, // required to load the actor
    Env.insight, // required to load the actor
    Env.push, // required to load the actor
    Env.perfStat, // required to load the actor
    Env.slack, // required to load the actor
    Env.challenge, // required to load the actor
    Env.explorer, // required to load the actor
    Env.fishnet, // required to schedule the cleaner
    Env.notifyModule, // required to load the actor
    Env.plan, // required to load the actor
    Env.event, // required to load the actor
    Env.activity, // required to load the actor
    Env.relay, // you know the drill by now
    Env.coach,
    Env.clazz,
    Env.resource,
    Env.member,
    Env.contest,
    Env.offlineContest,
    Env.appt,
    Env.calendar,
    Env.recall,
    Env.errors
  )) { lap =>
    lila.log.boot.info(s"${lap.millis}ms Preloading complete")
  }

  def getAlipayOptions: com.alipay.easysdk.kernel.Config = {
    val appId = config getString "alipay.appId"
    val merchantPrivateKey = config getString "alipay.merchantPrivateKey" // 应用私钥
    val merchantCertPath = config getString "alipay.merchantCertPath" // 应用公钥证书文件路径
    val alipayCertPath = config getString "alipay.alipayCertPath" // 支付宝公钥证书文件路径
    val alipayRootCertPath = config getString "alipay.alipayRootCertPath" //支付宝根证书文件路径

    val alipayConfig = new com.alipay.easysdk.kernel.Config()
    alipayConfig.protocol = "https"
    alipayConfig.gatewayHost = "openapi.alipay.com"
    alipayConfig.signType = "RSA2"
    alipayConfig.appId = appId
    alipayConfig.merchantPrivateKey = merchantPrivateKey
    //注：证书文件路径支持设置为文件系统中的路径或CLASS_PATH中的路径，优先从文件系统中加载，加载失败后会继续尝试从CLASS_PATH中加载
    alipayConfig.merchantCertPath = merchantCertPath
    alipayConfig.alipayCertPath = alipayCertPath
    alipayConfig.alipayRootCertPath = alipayRootCertPath
    alipayConfig
  }
  com.alipay.easysdk.factory.Factory.setOptions(getAlipayOptions)

  scheduler.once(5 seconds) { Env.slack.api.publishRestart }
  scheduler.once(10 seconds) {
    // delayed preloads
    Env.oAuth
    Env.studySearch
  }
}

object Env {

  lazy val current = "app" boot new Env(
    config = lila.common.PlayApp.loadConfig,
    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system,
    appPath = lila.common.PlayApp withApp (_.path.getCanonicalPath)
  )

  def api = lila.api.Env.current
  def db = lila.db.Env.current
  def user = lila.user.Env.current
  def security = lila.security.Env.current
  def hub = lila.hub.Env.current
  def socket = lila.socket.Env.current
  def memo = lila.memo.Env.current
  def message = lila.message.Env.current
  def i18n = lila.i18n.Env.current
  def game = lila.game.Env.current
  def bookmark = lila.bookmark.Env.current
  def search = lila.search.Env.current
  def gameSearch = lila.gameSearch.Env.current
  def timeline = lila.timeline.Env.current
  def forum = lila.forum.Env.current
  def forumSearch = lila.forumSearch.Env.current
  def team = lila.team.Env.current
  def teamSearch = lila.teamSearch.Env.current
  def analyse = lila.analyse.Env.current
  def mod = lila.mod.Env.current
  def notifyModule = lila.notify.Env.current
  def site = lila.site.Env.current
  def round = lila.round.Env.current
  def lobby = lila.lobby.Env.current
  def home = lila.home.Env.current
  def setup = lila.setup.Env.current
  def importer = lila.importer.Env.current
  def tournament = lila.tournament.Env.current
  def simul = lila.simul.Env.current
  def relation = lila.relation.Env.current
  def report = lila.report.Env.current
  def pref = lila.pref.Env.current
  def chat = lila.chat.Env.current
  def puzzle = lila.puzzle.Env.current
  def coordinate = lila.coordinate.Env.current
  def tv = lila.tv.Env.current
  def blog = lila.blog.Env.current
  def history = lila.history.Env.current
  def video = lila.video.Env.current
  def playban = lila.playban.Env.current
  def shutup = lila.shutup.Env.current
  def insight = lila.insight.Env.current
  def push = lila.push.Env.current
  def perfStat = lila.perfStat.Env.current
  def slack = lila.slack.Env.current
  def challenge = lila.challenge.Env.current
  def explorer = lila.explorer.Env.current
  def fishnet = lila.fishnet.Env.current
  def study = lila.study.Env.current
  def studySearch = lila.studySearch.Env.current
  def learn = lila.learn.Env.current
  def plan = lila.plan.Env.current
  def event = lila.event.Env.current
  def coach = lila.coach.Env.current
  def pool = lila.pool.Env.current
  def practice = lila.practice.Env.current
  def irwin = lila.irwin.Env.current
  def activity = lila.activity.Env.current
  def relay = lila.relay.Env.current
  def streamer = lila.streamer.Env.current
  def oAuth = lila.oauth.Env.current
  def bot = lila.bot.Env.current
  def evalCache = lila.evalCache.Env.current
  def rating = lila.rating.Env.current
  def clazz = lila.clazz.Env.current
  def resource = lila.resource.Env.current
  def member = lila.member.Env.current
  def contest = lila.contest.Env.current
  def offlineContest = lila.offlineContest.Env.current
  def appt = lila.appt.Env.current
  def calendar = lila.calendar.Env.current
  def recall = lila.recall.Env.current
  def errors = lila.errors.Env.current
}
