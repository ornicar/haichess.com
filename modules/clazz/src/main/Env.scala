package lila.clazz

import akka.actor.ActorSystem
import com.typesafe.config.Config
import lila.common.{ AtMost, Every, ResilientScheduler }
import lila.notify.NotifyApi
import lila.team.TeamApi
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    notifyApi: NotifyApi,
    teamApi: TeamApi,
    system: ActorSystem,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  val bus = system.lilaBus

  private val CollectionClazz = config getString "collection.clazz"
  private val CollectionCourse = config getString "collection.course"
  private val CollectionHomework = config getString "collection.homework"
  private val CollectionHomeworkStudent = config getString "collection.homework_student"
  private val CollectionHomeworkReport = config getString "collection.homework_report"

  lazy val courseApi = new CourseApi(
    coll = db(CollectionCourse),
    clazzColl = db(CollectionClazz),
    bus = bus
  )

  lazy val api = new ClazzApi(
    coll = db(CollectionClazz),
    courseApi = courseApi,
    teamApi = teamApi
  )

  lazy val studentApi = new StudentApi(
    coll = db(CollectionClazz),
    courseApi = courseApi,
    teamApi = teamApi,
    notifyApi = notifyApi,
    bus = bus
  )

  lazy val homeworkStudentApi = new HomeworkStudentApi(
    coll = db(CollectionHomeworkStudent),
    clazzApi = api,
    courseApi = courseApi,
    notifyApi = notifyApi,
    bus = bus
  )

  lazy val homeworkApi = new HomeworkApi(
    coll = db(CollectionHomework),
    stuApi = homeworkStudentApi,
    courseApi = courseApi,
    notifyApi = notifyApi
  )

  lazy val homeworkSolve = new HomeworkSolve(
    coll = db(CollectionHomeworkStudent),
    asyncCache = asyncCache
  )

  lazy val homeworkReport = new HomeworkReportApi(
    coll = db(CollectionHomeworkReport),
    clazzApi = api,
    homeworkApi = homeworkApi,
    stuApi = homeworkStudentApi
  )

  lazy val form = new ClazzForm(api)

  lazy val courseForm = new CourseForm(courseApi)

  lazy val homeworkForm = new HomeworkForm

  ResilientScheduler(
    every = Every(1 minute),
    atMost = AtMost(30 seconds),
    logger = logger,
    initialDelay = 30 seconds
  ) { homeworkReport.schedulerRefresh }(system)

  system.lilaBus.subscribeFun('homeworkCreate, 'finishPuzzle, 'finishGame, 'finishRush, 'recallFinished) {
    case create: StudentHomeworkCreate => homeworkSolve.handleCreate(create)
    case res: lila.puzzle.Puzzle.UserResult =>
      if (res.source == "puzzle" || res.source == "homework") homeworkSolve.handlePuzzle(res)
    case lila.game.actorApi.FinishGame(game, _, _) =>
      if (game.hasClock && game.nonAi) homeworkSolve.handleGame(game)
    case rush: lila.puzzle.PuzzleRush => homeworkSolve.handleRush(rush)
    case recall: lila.hub.actorApi.Recall => homeworkSolve.handleRecall(recall)
  }

}

object Env {

  lazy val current: Env = "clazz" boot new Env(
    config = lila.common.PlayApp loadConfig "clazz",
    db = lila.db.Env.current,
    notifyApi = lila.notify.Env.current.api,
    teamApi = lila.team.Env.current.api,
    system = lila.common.PlayApp.system,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
