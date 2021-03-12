package lila.home
package actorApi

import scala.concurrent.Promise
import lila.socket.SocketMember
import lila.socket.Socket.{ Sri, Sris }
import lila.user.User

private[home] case class Member(
    channel: JsChannel,
    user: Option[HomeUser],
    sri: Sri,
    mobile: Boolean
) extends SocketMember {

  val userId = user.map(_.id)
}

private[home] object Member {

  def apply(channel: JsChannel, user: Option[User], blocking: Set[String], sri: Sri, mobile: Boolean): Member = Member(
    channel = channel,
    user = user map { HomeUser.make(_, blocking) },
    sri = sri,
    mobile = mobile
  )
}

private[home] case class Connected(enumerator: JsEnumerator, member: Member)
private[home] case class Join(sri: Sri, user: Option[User], blocking: Set[String], mobile: Boolean, promise: Promise[Connected])
private[home] case class SetIdle(sri: Sri, value: Boolean)
private[home] case class GetSrisP(promise: Promise[Sris])
