package lila.clazz

import lila.user.User

case class ClazzUser(classmate: Set[User.ID], coach: Set[User.ID]) {

  def isMyClassmateOrCoach(userId: User.ID) = isMyClassmate(userId) || isMyCoach(userId)

  def isMyClassmate(userId: User.ID) = classmate.contains(userId)

  def isMyCoach(userId: User.ID) = coach.contains(userId)

}

object ClazzUser {

  val empty = ClazzUser(Set.empty, Set.empty)

}
