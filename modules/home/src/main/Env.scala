package lila.home

import akka.actor._
import com.typesafe.config.Config
import lila.game.Pov

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    blocking: String => Fu[Set[String]],
    gameCache: lila.game.Cached,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: ActorSystem
) {

  private val settings = new {
    val SocketSriTtl = config duration "socket.sri.ttl"
  }
  import settings._

  private val socket = new HomeSocket(system, SocketSriTtl)

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socket = socket,
    blocking = blocking
  )
}

object Env {

  lazy val current = "home" boot new Env(
    config = lila.common.PlayApp loadConfig "home",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    blocking = lila.relation.Env.current.api.fetchBlocking,
    gameCache = lila.game.Env.current.cached,
    asyncCache = lila.memo.Env.current.asyncCache,
    system = lila.common.PlayApp.system
  )
}
