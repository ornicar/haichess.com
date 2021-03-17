package lila.team

import akka.actor.ActorSelection
import lila.common.{ CellphoneAddress, Region, SmsTemplate }
import play.api.data._
import play.api.data.Forms._
import lila.db.dsl._
import lila.security.{ Granter, SmsCaptcher }
import lila.user.FormSelect
import play.api.data.validation.Constraints

private[team] final class DataForm(
    teamColl: Coll,
    val captcherActor: akka.actor.ActorSelection,
    val smsCaptcherActor: akka.actor.ActorSelection
) extends lila.hub.CaptchedForm with lila.hub.SmsCaptchedForm {

  override def captcher: ActorSelection = captcherActor
  override def smsCaptcha: ActorSelection = smsCaptcherActor

  private object Fields {
    val name = "name" -> text(minLength = 3, maxLength = 20)
    val province = "province" -> text.verifying(Region.Province.keySet contains _)
    val city = "city" -> text.verifying(Region.City.keySet contains _)
    val description = "description" -> text(minLength = 10, maxLength = 1000)
    val logo = "logo" -> optional(text(minLength = 5, maxLength = 150))
    val envPicture = "envPicture" -> optional(list(text(minLength = 5, maxLength = 150)))
    val open = "open" -> number
    val tagTip = "tagTip" -> number
    val gameId = "gameId" -> text
    val move = "move" -> text
  }

  val create = Form(mapping(
    Fields.name,
    Fields.province,
    Fields.city,
    Fields.description,
    Fields.open,
    Fields.gameId,
    Fields.move
  )(TeamSetup.apply)(TeamSetup.unapply)
    .verifying("当前俱乐部已经存在", d => !TeamRepo.teamExists(d).awaitSeconds(2))
    .verifying(captchaFailMessage, validateCaptcha _))

  def setting(team: Team) = Form(mapping(
    Fields.open,
    Fields.tagTip,
    "ratingSetting" -> mapping(
      "open" -> boolean,
      "defaultRating" -> number(min = RatingSetting.min, max = RatingSetting.max),
      "coachSupport" -> boolean,
      "turns" -> number(min = 0, max = 500),
      "minutes" -> number(min = 0, max = 240)
    )(RatingSetting.apply)(RatingSetting.unapply)
  )(TeamSetting.apply)(TeamSetting.unapply)) fill TeamSetting(
    open = if (team.open) 1 else 0,
    tagTip = if (team.tagTip) 1 else 0,
    ratingSetting = team.ratingSettingOrDefault
  )

  def edit(team: Team) = Form(mapping(
    Fields.name,
    Fields.province,
    Fields.city,
    Fields.description,
    Fields.logo,
    Fields.envPicture
  )(TeamEdit.apply)(TeamEdit.unapply)) fill TeamEdit(
    name = team.name,
    province = team.province,
    city = team.city,
    description = team.description,
    logo = team.logo,
    envPicture = team.envPicture
  )

  def certificationForm(user: lila.user.User) = Form(mapping(
    "leader" -> nonEmptyText(minLength = 2, maxLength = 20),
    "businessLicense" -> nonEmptyText(minLength = 5, maxLength = 150),
    "members" -> nonEmptyText.verifying(Constraints.pattern(regex = """^[1-9]\d{0,5}$""".r, error = "手机号格式错误")),
    "org" -> nonEmptyText(minLength = 2, maxLength = 20),
    "addr" -> nonEmptyText(minLength = 2, maxLength = 20),
    "message" -> optional(nonEmptyText(minLength = 2, maxLength = 1000)),
    SmsCaptcher.form.cellphone2,
    SmsCaptcher.form.template,
    SmsCaptcher.form.code
  )(CertificationData.apply)(CertificationData.unapply)
    .verifying(smsCaptchaFailMessage, validSmsCaptcha(user.username, _) awaitSeconds 3))

  def certificationOf(user: lila.user.User, team: Team) = team.certification.fold {
    certificationForm(user).fill(CertificationData(
      leader = "",
      businessLicense = "",
      members = "",
      org = "",
      addr = "",
      message = None,
      cellphone = "",
      template = SmsTemplate.Confirm.code,
      code = ""
    ))
  } { c =>
    certificationForm(user).fill(CertificationData(
      leader = c.leader,
      businessLicense = c.businessLicense,
      members = c.members.toString,
      org = c.org,
      addr = c.addr,
      message = c.message,
      cellphone = c.leaderContact,
      template = SmsTemplate.Confirm.code,
      code = ""
    ))
  }

  val certificationProcessForm = Form(tuple(
    "process" -> nonEmptyText,
    "comments" -> optional(nonEmptyText)
  ))

  val request = Form(mapping(
    "message" -> text(minLength = 10, maxLength = 2000),
    Fields.gameId,
    Fields.move
  )(RequestSetup.apply)(RequestSetup.unapply)
    .verifying(captchaFailMessage, validateCaptcha _)) fill RequestSetup(
    message = "您好，我想加入俱乐部！",
    gameId = "",
    move = ""
  )

  val processRequest = Form(tuple(
    "process" -> nonEmptyText,
    "url" -> nonEmptyText
  ))

  val selectMember = Form(single(
    "userId" -> lila.user.DataForm.historicalUsernameField
  ))

  val kickForm = Form(tuple(
    "userId" -> lila.user.DataForm.historicalUsernameField,
    "url" -> nonEmptyText
  ))

  def createWithCaptcha = withCaptcha(create)

  def memberSearch = Form(mapping(
    "username" -> optional(lila.user.DataForm.historicalUsernameField),
    "role" -> optional(text.verifying(Member.Role.list.map(_._1) contains _)),
    "name" -> optional(text(minLength = 2, maxLength = 20)),
    "sex" -> optional(text.verifying(FormSelect.Sex.keySet contains _)),
    "age" -> optional(number(min = 0, max = 100)),
    "level" -> optional(text.verifying(FormSelect.Level.keySet contains _)),
    "fields" -> list(mapping(
      "fieldName" -> nonEmptyText,
      "fieldValue" -> optional(nonEmptyText)
    )(MemberTag.apply)(MemberTag.unapply)),
    "rangeFields" -> list(mapping(
      "fieldName" -> nonEmptyText,
      "min" -> optional(nonEmptyText),
      "max" -> optional(nonEmptyText)
    )(RangeMemberTag.apply)(RangeMemberTag.unapply))
  )(MemberSearch.apply)(MemberSearch.unapply))

  def memberAdd = Form(mapping(
    "mark" -> optional(text(minLength = 1, maxLength = 20)),
    "rating" -> optional(number(min = EloRating.min, EloRating.max)),
    "fields" -> list(mapping(
      "fieldName" -> nonEmptyText,
      "fieldValue" -> optional(nonEmptyText)
    )(MemberTag.apply)(MemberTag.unapply))
  )(MemberAdd.apply)(MemberAdd.unapply))

  def memberEditOf(team: Team, mwu: MemberWithUser) = memberEdit(mwu.user) fill MemberEdit(
    role = mwu.member.role.id,
    mark = mwu.member.mark,
    fields = mwu.member.tagsIfEmpty.toList.map {
      case (_, t) => t
    }
  )

  def memberEdit(u: lila.user.User) = Form(mapping(
    "role" -> nonEmptyText.verifying("role can not apply", Member.Role.list.map(_._1).contains _),
    "mark" -> optional(text(minLength = 1, maxLength = 10)),
    "fields" -> list(mapping(
      "fieldName" -> nonEmptyText,
      "fieldValue" -> optional(nonEmptyText)
    )(MemberTag.apply)(MemberTag.unapply))
  )(MemberEdit.apply)(MemberEdit.unapply))

  val tagAdd = Form(mapping(
    "typ" -> nonEmptyText.verifying("非法类型", Tag.Type.keySet.contains _),
    "label" -> nonEmptyText(minLength = 1, maxLength = 10),
    "value" -> optional(nonEmptyText)
  )(TagAdd.apply)(TagAdd.unapply)
    .verifying("当前类型可选值必填", _.mustHaveValue))

  def tagEditOf(tag: Tag) = tagEdit fill TagEdit(tag.typ.id, tag.label, tag.value)

  def tagEdit = Form(mapping(
    "typ" -> nonEmptyText.verifying("非法类型", Tag.Type.keySet.contains _),
    "label" -> nonEmptyText(minLength = 1, maxLength = 10),
    "value" -> optional(nonEmptyText)
  )(TagEdit.apply)(TagEdit.unapply)
    .verifying("当前类型可选值必填", _.mustHaveValue))

}

private[team] case class TeamSetup(
    name: String,
    province: String,
    city: String,
    description: String,
    open: Int,
    gameId: String,
    move: String
) {

  def isOpen = open == 1

  def trim = copy(
    name = name.trim,
    province = province,
    city = city,
    description = description.trim
  )
}

private[team] case class TeamSetting(
    open: Int,
    tagTip: Int,
    ratingSetting: RatingSetting
) {

  def isOpen = open == 1
  def showTagTip = tagTip == 1

}

private[team] case class TeamEdit(
    name: String,
    province: String,
    city: String,
    description: String,
    logo: Option[String],
    envPicture: Option[List[String]]
) {

  def trim = copy(
    province = province,
    city = city,
    description = description.trim
  )
}

private[team] case class RequestSetup(
    message: String,
    gameId: String,
    move: String
)

private[team] case class CertificationData(
    leader: String,
    businessLicense: String,
    members: String,
    org: String,
    addr: String,
    message: Option[String],
    cellphone: String,
    template: String,
    code: String
)

private[team] case class TagAdd(
    typ: String,
    label: String,
    value: Option[String]
) {

  def realType = Tag.Type(typ)

  def mustHaveValue = value.isDefined == realType.hasValue

}

private[team] case class TagEdit(
    typ: String,
    label: String,
    value: Option[String]
) {
  def realType = Tag.Type(typ)
  def mustHaveValue = value.isDefined == realType.hasValue
}

case class MemberSearch(
    username: Option[String] = None,
    role: Option[String] = None,
    name: Option[String] = None,
    sex: Option[String] = None,
    age: Option[Int] = None,
    level: Option[String] = None,
    fields: List[MemberTag] = List.empty[MemberTag],
    rangeFields: List[RangeMemberTag] = List.empty[RangeMemberTag]
) {

}
object MemberSearch {
  def empty = MemberSearch()
}

private[team] case class MemberAdd(
    mark: Option[String],
    rating: Option[Int],
    fields: List[MemberTag]
) {
}

private[team] case class MemberEdit(
    role: String,
    mark: Option[String],
    fields: List[MemberTag]
) {
  def realRole = Member.Role(role)
}