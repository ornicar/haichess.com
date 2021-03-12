package lila.errors

import akka.actor._
import com.typesafe.config.Config
import lila.puzzle.PuzzleApi

final class Env(
    config: Config,
    system: ActorSystem,
    hub: lila.hub.Env,
    db: lila.db.Env,
    puzzleApi: PuzzleApi
) {

  val CollectionPuzzleErrors = config getString "collection.puzzle"
  val CollectionGameErrors = config getString "collection.game"

  private lazy val puzzleErrorsColl = db(CollectionPuzzleErrors)
  private lazy val gameErrorsColl = db(CollectionGameErrors)

  lazy val puzzleErrorsApi = new PuzzleErrorsApi(puzzleErrorsColl, puzzleApi, hub.bus)
  lazy val gameErrorsApi = new GameErrorsApi(gameErrorsColl, hub.bus)

  val forms = DataForm

  system.lilaBus.subscribeFun('finishPuzzle, 'analysisReady) {
    case res: lila.puzzle.Puzzle.UserResult => if (!res.result.win && !(res.timeout | false)) {
      puzzleErrorsApi.receive(res)
    }
    case lila.analyse.actorApi.AnalysisReady(game, analysis) =>
      gameErrorsApi.receive(game, analysis)
  }

}

object Env {

  lazy val current: Env = "errors" boot new Env(
    config = lila.common.PlayApp loadConfig "errors",
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current,
    puzzleApi = lila.puzzle.Env.current.api
  )
}
