package lila.team

import lila.common.Region
import org.joda.time.DateTime
import ornicar.scalalib.Random
import lila.user.User

case class Team(
    _id: Team.ID,
    name: String,
    province: String,
    city: String,
    description: String,
    logo: Option[String],
    envPicture: Option[List[String]],
    nbMembers: Int,
    enabled: Boolean,
    open: Boolean,
    tagTip: Boolean,
    certification: Option[Certification],
    clazzIds: Option[List[String]],
    createdAt: DateTime,
    createdBy: User.ID
) {

  def id = _id

  def slug = id

  def disabled = !enabled

  def certified = certification ?? (_.status.approved)

  def isCreator(user: String) = user == createdBy

  def provinceName = Region.Province.name(province)

  def cityName = Region.City.name(city)

  def logoOrDefault = logo | "images/logo.256.png"

  def envPictureOrDefault = envPicture | List.empty[String]

  def location = s"${provinceName} ${cityName}"
}

object Team {

  type ID = String

  case class TeamWithMember(team: Team, member: Member)
  case class IdsStr(value: String) extends AnyVal {

    def contains(teamId: ID) =
      value.startsWith(teamId) ||
        value.endsWith(teamId) ||
        value.contains(s"${IdsStr.separator}$teamId${IdsStr.separator}")

    def toArray: Array[String] = value.split(IdsStr.separator)
    def toList = if (value.isEmpty) Nil else toArray.toList
  }

  object IdsStr {

    private val separator = ' '

    val empty = IdsStr("")

    def apply(ids: Iterable[ID]): IdsStr = IdsStr(ids mkString separator.toString)
  }

  def make(
    id: String,
    name: String,
    province: String,
    city: String,
    description: String,
    open: Boolean,
    createdBy: User
  ): Team = new Team(
    _id = id,
    name = name,
    province = province,
    city = city,
    description = description,
    logo = None,
    envPicture = None,
    nbMembers = 1,
    enabled = true,
    open = open,
    tagTip = true,
    certification = None,
    clazzIds = None,
    createdAt = DateTime.now,
    createdBy = createdBy.id
  )

  /*
  def makeId = Random nextString 8

    def nameToId(name: String) = (lila.common.String slugify name) |> { slug =>
    // if most chars are not latin, go for random slug
    if (slug.size > (name.size / 2)) slug else Random nextString 8
  }*/
}
