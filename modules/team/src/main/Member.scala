package lila.team

import org.joda.time.DateTime
import lila.user.User

case class Member(
    _id: String,
    team: String,
    user: String,
    role: Member.Role,
    rating: Option[EloRating],
    clazzIds: Option[List[String]],
    tags: Option[MemberTags],
    mark: Option[String],
    date: DateTime
) {

  def tagsIfEmpty: MemberTags = tags | MemberTags.empty
  def tagValue(field: String) = tagsIfEmpty.tagMap.get(field) ?? (_.value)
  def is(userId: String): Boolean = user == userId
  def is(user: User): Boolean = is(user.id)
  def id = _id
  def isOwner = role == Member.Role.Owner
  def isCoach = role == Member.Role.Coach
  def isOwnerOrCoach = isOwner || isCoach
  def intRating = rating.map(_.intValue)

  def clazzIdOrDefault = clazzIds | Nil

}

object Member {

  def makeId(team: String, user: String) = user + "@" + team

  def make(
    team: String,
    user: String,
    role: Role = Role.Trainee,
    tags: Option[MemberTags] = None,
    mark: Option[String] = None,
    rating: Option[Int] = None,
    clazzIds: Option[List[String]] = None
  ): Member = new Member(
    _id = makeId(team, user),
    user = user,
    team = team,
    role = role,
    rating = rating.map(EloRating(_, 0)),
    tags = tags,
    mark = mark,
    clazzIds = clazzIds,
    date = DateTime.now
  )

  sealed abstract class Role(val id: String, val name: String, val canWrite: Boolean, val sort: Int)
  object Role {
    case object Owner extends Role("owner", "所有者", true, 1)
    case object Coach extends Role("coach", "教练", false, 2)
    case object Trainee extends Role("trainee", "会员", false, 3)

    val all = List(Owner, Coach, Trainee)
    def list = all.map { r => r.id -> r.name }
    def byId = all.map { x => x.id -> x }.toMap
    def apply(id: String): Role = all.find(_.id == id) err s"Count find Role: $id"
  }

}

case class MemberWithUser(member: Member, user: User) {
  def team = member.team
  def date = member.date
  def profile = user.profileOrDefault
  def viewName = member.mark | user.realNameOrUsername
}

case class RangeMemberTag(field: String, min: Option[String], max: Option[String])
case class MemberTag(field: String, value: Option[String])
object MemberTag {
  type TagField = String
  type MemberTagMap = Map[TagField, MemberTag]
}

case class MemberTags(tagMap: MemberTag.MemberTagMap) {
  def toList = tagMap.toList
}

object MemberTags {

  val empty = MemberTags(Map.empty)
  def byTagList(tagList: List[MemberTag]) = MemberTags(tagList.map(t => t.field -> t).toMap)
}