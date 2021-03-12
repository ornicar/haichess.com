package lila.chat

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

import lila.socket.Socket.makeMessage
import lila.socket.SocketMember
import lila.user.User

private final class Palantir(bus: lila.common.Bus) {

  import Palantir._

  private val channels: Cache[Chat.Id, Channel] = Scaffeine()
    .expireAfterWrite(1 minute)
    .build[Chat.Id, Channel]

  def ping(chatId: Chat.Id, userId: User.ID, member: SocketMember): Unit = {
    val channel = channels.get(chatId, _ => new Channel)
    channel.add(userId, member)
    member.push(makeMessage("palantir", channel.userIds.filter(userId !=)))
    lila.mon.palantir.channels(channels.estimatedSize)
  }

  private def hangUp(userId: User.ID) =
    channels.asMap.foreach {
      case (_, channel) => channel get userId foreach {
        _ push makeMessage("palantirOff")
      }
    }

  bus.subscribeFun('shadowban, 'accountClose) {
    case lila.hub.actorApi.mod.Shadowban(userId, true) => hangUp(userId)
    case lila.hub.actorApi.security.CloseAccount(userId) => hangUp(userId)
  }
}

private object Palantir {

  class Channel {

    private val members: Cache[User.ID, SocketMember] = Scaffeine()
      .expireAfterWrite(7 seconds)
      .build[User.ID, SocketMember]

    def add(uid: User.ID, member: SocketMember) = members.put(uid, member)

    def get(uid: User.ID) = members getIfPresent uid

    def userIds = members.asMap.keys
  }

  case class Ping(chatId: Chat.Id, userId: User.ID, member: SocketMember)
}
