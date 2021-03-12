package lila.home

import actorApi._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import play.api.libs.iteratee._
import play.api.libs.json._
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.home.{ ReloadAppt, ReloadCalendar }
import lila.socket.{ LoneSocket, Socket, SocketTrouper }

private[home] final class HomeSocket(
    system: ActorSystem,
    sriTtl: FiniteDuration
) extends SocketTrouper[Member](system, sriTtl) with LoneSocket {

  def monitoringName = "home"
  def broomFrequency = 4073 millis

  system.lilaBus.subscribe(this, 'changeFeaturedGame, 'homeSocket, 'changeAppt, 'changeCalendar)
  system.scheduler.schedule(1 minute, 1 minute)(this ! Cleanup)

  private var idleSris = collection.mutable.Set[String]()

  def receiveSpecific = {

    case GetSrisP(promise) =>
      promise success Socket.Sris(members.keySet.map(Socket.Sri.apply)(scala.collection.breakOut))

    case Cleanup =>
      idleSris retain members.contains

    case Join(sri, user, blocks, mobile, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, blocks, sri, mobile)
      addMember(sri, member)
      promise success Connected(enumerator, member)

    case ReloadAppt(userIds) => userIds foreach { userId =>
      membersByUserId(userId) foreach (_ push reloadAppt)
    }

    case ReloadCalendar(userIds) => userIds foreach { userId =>
      membersByUserId(userId) foreach (_ push reloadCalendar)
    }

    case ChangeFeatured(_, msg) => notifyAllActive(msg)

    case SetIdle(sri, true) => idleSris += sri.value
    case SetIdle(sri, false) => idleSris -= sri.value

  }

  private def reloadAppt = makeMessage("reload_appt")
  private def reloadCalendar = makeMessage("reload_calendar")

  private def notifyAllActive(msg: JsObject) =
    members.foreach {
      case (sri, member) => if (!idleSris(sri)) member push msg
    }

  override protected def afterQuit(sri: Socket.Sri, member: Member) = {
    idleSris -= sri.value
  }

  private case object Cleanup
}
