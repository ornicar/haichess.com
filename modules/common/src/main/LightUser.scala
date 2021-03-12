package lila.common

import play.api.libs.json.{ Json, OWrites }

case class LightUser(
    id: String,
    name: String,
    title: Option[String],
    isPatron: Boolean,
    member: Option[String] = None, // general, silver, gold
    head: Option[String] = None,
    roles: List[String] = Nil
) {

  def titleName = title.fold(name)(_ + " " + name)

  def isBot = title has "BOT"

  def isGeneral = member.??(_ == "general")
  def isSilver = member.??(_ == "silver")
  def isGold = member.??(_ == "gold")
  def isCoach = roles.contains("ROLE_COACH")
  def isTeam = roles.contains("ROLE_TEAM")
}

object LightUser {

  implicit val lightUserWrites = OWrites[LightUser] { u =>
    Json.obj(
      "id" -> u.id,
      "name" -> u.name
    ).add("title" -> u.title)
      .add("patron" -> u.isPatron)
      .add("member" -> u.member)
      .add("head" -> u.head)
      .add("coach" -> u.isCoach)
      .add("team" -> u.isTeam)
  }

  def fallback(userId: String) = LightUser(
    id = userId,
    name = userId,
    title = None,
    isPatron = false
  )

  type Getter = String => Fu[Option[LightUser]]
  type GetterSync = String => Option[LightUser]
  type IsBotSync = String => Boolean
}
