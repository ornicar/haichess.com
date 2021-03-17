package lila.puzzle

import akka.actor.{ ActorSelection, ActorSystem }
import com.typesafe.config.Config

final class Env(
    config: Config,
    renderer: ActorSelection,
    historyApi: lila.history.HistoryApi,
    lightUserApi: lila.user.LightUserApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle,
    isOnline: lila.user.User.ID => Boolean,
    lightUser: lila.common.LightUser.GetterSync,
    rankingApi: lila.user.RankingApi
) {

  private val settings = new {
    val CollectionPuzzle = config getString "collection.puzzle"
    val CollectionRound = config getString "collection.round"
    val CollectionVote = config getString "collection.vote"
    val CollectionHead = config getString "collection.head"
    val CollectionTagger = config getString "collection.tagger"
    val CollectionPuzzleRound = config getString "collection.round_all"
    val CollectionPuzzleRush = config getString "collection.rush"
    val CollectionPuzzleRushRankHistory = config getString "collection.rush_rank_history"
    val CollectionPuzzleRushRankToday = config getString "collection.rush_rank_today"
    val CollectionPuzzleRushRankSeason = config getString "collection.rush_rank_season"
    val CollectionPuzzleThemeRecord = config getString "collection.puzzle_theme_record"

    val ApiToken = config getString "api.token"
    val AnimationDuration = config duration "animation.duration"
    val PuzzleIdMin = config getInt "selector.puzzle_id_min"
    val PuzzleMarkIdMin = config getInt "selector.puzzle_mark_id_min"
  }
  import settings._

  private val db = new lila.db.Env("puzzle", config getConfig "mongodb", lifecycle)

  private lazy val gameJson = new GameJson(asyncCache, lightUserApi)

  lazy val jsonView = new JsonView(
    gameJson,
    animationDuration = AnimationDuration
  )

  lazy val userJsonView = new UserJsonView(
    isOnline = isOnline,
    lightUser = lightUser
  )

  lazy val api = new PuzzleApi(
    puzzleColl = puzzleColl,
    roundColl = roundColl,
    voteColl = voteColl,
    headColl = headColl,
    taggerColl = taggerColl,
    puzzleIdMin = PuzzleIdMin,
    puzzleMarkIdMin = PuzzleMarkIdMin,
    asyncCache = asyncCache,
    apiToken = ApiToken,
    bus = system.lilaBus
  )

  lazy val finisher = new Finisher(
    historyApi = historyApi,
    api = api,
    puzzleColl = puzzleColl,
    bus = system.lilaBus,
    rankingApi = rankingApi
  )

  lazy val selector = new Selector(
    puzzleColl = puzzleColl,
    api = api,
    puzzleIdMin = PuzzleIdMin,
    bus = system.lilaBus
  )

  lazy val batch = new PuzzleBatch(
    puzzleColl = puzzleColl,
    api = api,
    finisher = finisher,
    puzzleIdMin = PuzzleIdMin
  )

  lazy val resource = new PuzzleResource(
    puzzleColl = puzzleColl,
    api = api,
    bus = system.lilaBus
  )

  lazy val userInfos = new UserInfosApi(
    roundColl = roundColl,
    currentPuzzleId = api.head.currentPuzzleId
  )

  lazy val puzzleRushApi = new PuzzleRushApi(
    puzzleRushColl = puzzleRushColl,
    puzzleRoundApi = puzzleRoundApi,
    finisher = finisher,
    system = system
  )

  lazy val puzzleRushRankHistoryApi = new PuzzleRushRankHistoryApi(
    puzzleRushRankHistoryColl = puzzleRushRankHistoryColl
  )

  lazy val puzzleRushRankTodayApi = new PuzzleRushRankTodayApi(
    puzzleRushRankTodayColl = puzzleRushRankTodayColl
  )

  lazy val puzzleRushRankSeasonApi = new PuzzleRushRankSeasonApi(
    puzzleRushRankSeasonColl = puzzleRushRankSeasonColl,
    asyncCache = asyncCache
  )

  lazy val puzzleRoundApi = new PuzzleRoundApi(
    puzzleRoundColl = puzzleRoundColl
  )

  lazy val puzzleRushSelector = new PuzzleRushSelector(
    puzzleColl = puzzleColl,
    puzzleRoundApi = puzzleRoundApi
  )

  lazy val puzzleFixApi = new PuzzleFixApi(
    puzzleColl = puzzleColl
  )

  lazy val puzzleThemeRecord = new ThemeRecordApi(
    coll = puzzleThemeRecordColl
  )

  lazy val forms = DataForm

  lazy val daily = new Daily(
    puzzleColl,
    renderer,
    asyncCache = asyncCache,
    system.scheduler
  )

  lazy val activity = new PuzzleActivity(
    puzzleColl = puzzleColl,
    roundColl = roundColl
  )(system)

  def cli = new lila.common.Cli {
    def process = {
      case "puzzle" :: "disable" :: id :: Nil => parseIntOption(id) ?? { id =>
        api.puzzle disable id inject "Done"
      }
    }
  }

  lazy val puzzleColl = db(CollectionPuzzle)
  private[puzzle] lazy val roundColl = db(CollectionRound)
  private[puzzle] lazy val voteColl = db(CollectionVote)
  private[puzzle] lazy val headColl = db(CollectionHead)
  private[puzzle] lazy val taggerColl = db(CollectionTagger)
  private[puzzle] lazy val puzzleRoundColl = db(CollectionPuzzleRound)
  private[puzzle] lazy val puzzleRushColl = db(CollectionPuzzleRush)
  private[puzzle] lazy val puzzleRushRankHistoryColl = db(CollectionPuzzleRushRankHistory)
  private[puzzle] lazy val puzzleRushRankTodayColl = db(CollectionPuzzleRushRankToday)
  private[puzzle] lazy val puzzleRushRankSeasonColl = db(CollectionPuzzleRushRankSeason)
  private[puzzle] lazy val puzzleThemeRecordColl = db(CollectionPuzzleThemeRecord)

  system.lilaBus.subscribeFun('finishPuzzle, 'beginRush) {
    case res: lila.puzzle.Puzzle.UserResult => puzzleRoundApi.createBySub(res)
    case rush: lila.puzzle.PuzzleRush => puzzleRushApi.scheduleFinish(rush)
  }

  system.lilaBus.subscribeFun('finishRush) {
    case rush: lila.puzzle.PuzzleRush => puzzleRushRankHistoryApi.createBySub(rush)
  }

  system.lilaBus.subscribeFun('finishRush) {
    case rush: lila.puzzle.PuzzleRush => puzzleRushRankTodayApi.createBySub(rush)
  }

  system.lilaBus.subscribeFun('finishRush) {
    case rush: lila.puzzle.PuzzleRush => puzzleRushRankSeasonApi.createBySub(rush)
  }

  system.lilaBus.subscribeFun('nextThemePuzzle) {
    case lila.hub.actorApi.puzzle.NextThemePuzzle(puzzleId, userId, queryString) => puzzleThemeRecord.upsert(userId, puzzleId, queryString)
  }

}

object Env {

  lazy val current: Env = "puzzle" boot new Env(
    config = lila.common.PlayApp loadConfig "puzzle",
    renderer = lila.hub.Env.current.renderer,
    historyApi = lila.history.Env.current.api,
    lightUserApi = lila.user.Env.current.lightUserApi,
    asyncCache = lila.memo.Env.current.asyncCache,
    system = lila.common.PlayApp.system,
    lifecycle = lila.common.PlayApp.lifecycle,
    isOnline = lila.user.Env.current.isOnline,
    lightUser = lila.user.Env.current.lightUserSync,
    rankingApi = lila.user.Env.current.rankingApi
  )
}
