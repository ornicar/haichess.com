package lila.user

import lila.common.Region
import lila.db.{ BSON, dsl }
import org.joda.time.DateTime

case class Profile(
    head: Option[String] = None,
    country: Option[String] = None,
    bio: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fideRating: Option[Int] = None,
    uscfRating: Option[Int] = None,
    ecfRating: Option[Int] = None,
    links: Option[String] = None,
    province: Option[String] = None,
    city: Option[String] = None,
    realName: Option[String] = None,
    sex: Option[String] = None,
    levels: Option[Levels] = None,
    birthyear: Option[Int] = None,
    /*mobile: Option[String] = None,*/
    wechat: Option[String] = None
) {

  def withLevelKey(lv: String) = withLevels(Level(level = lv, current = 1))

  def withLevels(lv: Level) = copy(
    levels = ((levels | Levels.empty) + lv).some
  )

  def lvs = levels match {
    case None => sys error "Invalid Level"
    case Some(lvs) => lvs
  }

  def currentLevel: Level = lvs.current

  def ofLevel = FormSelect.Level.byKey(currentLevel.level)

  def ofSex = sex.map(FormSelect.Sex.byKey(_))

  def age = birthyear.map(y => DateTime.now.getYear - y)

  def nonEmptyRealName = realName

  def countryInfo = country flatMap Countries.info

  def nonEmptyBio = ne(bio)

  def location = s"${province.??(Region.Province.name)} ${city.?? { Region.City.name }}"

  def isEmpty = completionPercent == 0

  def isComplete = completionPercent == 100

  def isCoachComplete = coachCompletionPercent == 100

  def completionPercent: Int = {
    //val c = List(country, bio, firstName, lastName)
    val c = List(realName, levels, province, city, sex, birthyear, bio, /*mobile,*/ wechat)
    100 * c.count(_.isDefined) / c.size
  }

  def coachCompletionPercent: Int = {
    val c = List(realName, province, city)
    100 * c.count(_.isDefined) / c.size
  }

  def actualLinks: List[Link] = links ?? Links.make

  import Profile.OfficialRating

  def officialRating: Option[OfficialRating] =
    fideRating.map { OfficialRating("fide", _) } orElse
      uscfRating.map { OfficialRating("uscf", _) } orElse
      ecfRating.map { OfficialRating("ecf", _) }

  private def ne(str: Option[String]) = str.filter(_.nonEmpty)

}

object Profile {

  case class OfficialRating(name: String, rating: Int)

  val default = Profile()

  private[user] implicit def levelsHandler = Levels.levelsBSONHandler
  private[user] val profileBSONHandler = new BSON[Profile] {
    import reactivemongo.bson.BSONDocument

    def writes(w: BSON.Writer, u: Profile): dsl.Bdoc = BSONDocument(
      "head" -> u.head,
      "country" -> u.country,
      "bio" -> u.bio,
      "firstName" -> u.firstName,
      "lastName" -> u.lastName,
      "fideRating" -> u.fideRating,
      "uscfRating" -> u.uscfRating,
      "ecfRating" -> u.ecfRating,
      "links" -> u.links,
      "province" -> u.province,
      "city" -> u.city,
      "realName" -> u.realName,
      "sex" -> u.sex,
      "birthyear" -> u.birthyear,
      /*"mobile" -> u.mobile,*/
      "wechat" -> u.wechat
    )

    def reads(r: BSON.Reader): Profile = Profile(
      head = r.strO("head"),
      country = r.strO("country"),
      bio = r.strO("bio"),
      firstName = r.strO("firstName"),
      lastName = r.strO("lastName"),
      fideRating = r.intO("fideRating"),
      uscfRating = r.intO("uscfRating"),
      ecfRating = r.intO("ecfRating"),
      links = r.strO("links"),
      province = r.strO("province"),
      city = r.strO("city"),
      realName = r.strO("realName"),
      sex = r.strO("sex"),
      levels = r.getO[Levels]("levels"),
      birthyear = r.intO("birthyear"),
      /*mobile = r.strO("mobile"),*/
      wechat = r.strO("wechat")
    )
  }
}
