package lila.home

import actorApi._
import lila.user.User
import lila.common.ApiVersion
import lila.socket.{ Socket, Handler }

private[home] final class SocketHandler(
    hub: lila.hub.Env,
    socket: HomeSocket,
    blocking: String => Fu[Set[String]]
) {

  private val pong = Socket.emptyPong

  private def controller(socket: HomeSocket, member: Member, isBot: Boolean): Handler.Controller = {
    case ("idle", o) => socket ! SetIdle(member.sri, ~(o boolean "d"))
  }

  def apply(
    sri: Socket.Sri,
    user: Option[User],
    mobile: Boolean,
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] =
    (user ?? (u => blocking(u.id))) flatMap { blockedUserIds =>
      socket.ask[Connected](Join(sri, user, blockedUserIds, mobile, _)) map {
        case Connected(enum, member) => Handler.iteratee(
          hub,
          controller(socket, member, user.exists(_.isBot)),
          member,
          socket,
          sri,
          apiVersion,
          onPing = (_, _, _, _) => {
            socket setAlive sri
            member push pong
          }
        ) -> enum
      }
    }
}
