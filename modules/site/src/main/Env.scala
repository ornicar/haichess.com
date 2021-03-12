package lila.site

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.libs.concurrent.Akka.system

import lila.socket.actorApi.SendToFlag

final class Env(
    config: Config,
    hub: lila.hub.Env,
    system: ActorSystem
) {

  private val SocketSriTtl = config duration "socket.sri.ttl"

  private val socket = new Socket(system, SocketSriTtl)

  lazy val socketHandler = new SocketHandler(socket, hub)
}

object Env {

  lazy val current = "site" boot new Env(
    config = lila.common.PlayApp loadConfig "site",
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system
  )
}
