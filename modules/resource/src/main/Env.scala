package lila.resource

import akka.actor.ActorSystem
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem
) {

  private val CollectionCapsule = config getString "collection.capsule"

  lazy val capsuleApi = new CapsuleApi(
    coll = db(CollectionCapsule),
    bus = system.lilaBus
  )

  lazy val forms = DataForm

  system.lilaBus.subscribeFun('puzzleResourceRemove) {
    case lila.hub.actorApi.resource.PuzzleResourceRemove(puzzleIds) => capsuleApi.removePuzzle(puzzleIds)
  }

}

object Env {

  lazy val current: Env = "resource" boot new Env(
    config = lila.common.PlayApp loadConfig "resource",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system
  )
}
