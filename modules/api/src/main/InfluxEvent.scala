package lila.api

import akka.actor._
import play.api.libs.ws.WS
import play.api.Play.current

import lila.hub.actorApi.{ DeployPre, DeployPost }

private final class InfluxEvent(endpoint: String, env: String) extends Actor {

  override def preStart(): Unit = {
    context.system.lilaBus.subscribe(self, 'deploy)
    event("lila_start", "Lila starts")
  }

  def receive = {
    case DeployPre => event("lila_deploy_pre", "Haichess 将会在一分钟之内更新")
    case DeployPost => event("lila_deploy_post", "Haichess 正在更新中")
  }

  def event(key: String, text: String) = {
    val data = s"""event,program=lila,env=$env,title=$key text="$text""""
    WS.url(endpoint).post(data).effectFold(
      err => onError(s"$endpoint $data $err"),
      res => if (res.status != 204) onError(s"$endpoint $data ${res.status}")
    )
  }

  def onError(msg: String) = lila.log("influx_event").warn(msg)
}
