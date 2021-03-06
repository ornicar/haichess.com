package lila.study

import org.joda.time.DateTime

import lila.user.User

case class StudyMember(id: User.ID, role: StudyMember.Role) {

  def canContribute = role.canWrite
}

object StudyMember {

  type MemberMap = Map[User.ID, StudyMember]

  def make(user: User) = StudyMember(id = user.id, role = Role.Read)

  sealed abstract class Role(val id: String, val canWrite: Boolean)
  object Role {
    case object Read extends Role("r", false)
    case object Write extends Role("w", true)
    val byId = List(Read, Write).map { x => x.id -> x }.toMap
  }
}

case class StudyMembers(members: StudyMember.MemberMap) {

  def +(member: StudyMember) = copy(members = members + (member.id -> member))

  def contains(userId: User.ID): Boolean = members contains userId
  def contains(user: User): Boolean = contains(user.id)
  def containsAndHasWriteRole(userId: User.ID): Boolean = (members contains userId) && members.find(_._1 == userId).exists(m => m._2.role == StudyMember.Role.Write)

  def containsIfOne(userIds: Set[User.ID]): Boolean = members.exists(m => userIds.contains(m._1))
  def containsAndHasWriteRoleIfOne(userIds: Set[User.ID]): Boolean = members.exists(m => userIds.contains(m._1) && m._2.role == StudyMember.Role.Write)

  def get = members.get _

  def ids = members.keys

  def contributorIds: Set[User.ID] = members.collect {
    case (id, member) if member.canContribute => id
  }(scala.collection.breakOut)
}

object StudyMembers {
  val empty = StudyMembers(Map.empty)
}
