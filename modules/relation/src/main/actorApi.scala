package lila.relation
package actorApi

import lila.common.LightUser

private[relation] case class AllOnlineFriends(onlines: Map[ID, LightUser])
private[relation] case object ComputeMovement

case class OnlineFriends(users: List[LightUser], playing: Set[String], studying: Set[String]) {
  def patrons: List[String] = users collect {
    case u if u.isPatron => u.id
  }

  def heads: List[(String, String)] = users collect {
    case u if u.head.isDefined => (u.id -> u.head.get)
  }
}
object OnlineFriends {
  val empty = OnlineFriends(Nil, Set.empty, Set.empty)
}
