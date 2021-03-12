package lila.recall

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    hub: lila.hub.Env,
    db: lila.db.Env,
    lightUserApi: lila.user.LightUserApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    studyApi: lila.study.StudyApi,
    importer: lila.importer.Importer,
    gamePgnDump: lila.game.PgnDump
) {

  val CollectionRecall = config getString "collection.recall"
  val AnimationDuration = config duration "animation.duration"

  private lazy val gameJson = new GameJson(asyncCache, lightUserApi, importer)

  private lazy val replayColl = db(CollectionRecall)

  lazy val api = new RecallApi(
    coll = replayColl,
    bus = hub.bus,
    studyApi = studyApi,
    importer = importer,
    pgnDump = gamePgnDump
  )

  lazy val jsonView = new JsonView(
    gameJson = gameJson,
    animationDuration = AnimationDuration
  )

  lazy val form = new DataForm(studyApi)

}

object Env {

  lazy val current: Env = "recall" boot new Env(
    config = lila.common.PlayApp loadConfig "recall",
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current,
    lightUserApi = lila.user.Env.current.lightUserApi,
    asyncCache = lila.memo.Env.current.asyncCache,
    studyApi = lila.study.Env.current.api,
    importer = lila.importer.Env.current.importer,
    gamePgnDump = lila.game.Env.current.pgnDump
  )
}
