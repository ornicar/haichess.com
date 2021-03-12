package lila.team

import org.joda.time.DateTime
import lila.user.User

case class Tag(
    _id: String,
    team: String,
    field: String,
    label: String,
    value: Option[String],
    typ: Tag.Type,
    editable: Boolean,
    createAt: DateTime,
    createBy: User.ID
) {

  def toChoice = value.err(s"Bad TagValue, type: ${typ.id}").replaceAll("，", ",").split(",").toList map { v => (v.trim -> v.trim) }

}

object Tag {

  val oldDefault = List("tag_name", "tag_birthyear", "tag_birthday", "tag_sex", "tag_level")

  /* def defaults(team: String, userId: User.ID) = List(
    nameTag(team, userId),
    birthyearTag(team, userId),
    birthdayTag(team, userId),
    sexTag(team, userId),
    level(team, userId)
  )

  def nameTag(team: String, userId: User.ID) = make(team = team, field = "tag_name", label = "姓名", typ = Tag.Type.Text, editable = false, userId = userId)
  def birthyearTag(team: String, userId: User.ID) = make(team = team, field = "tag_birthyear", label = "出生年", typ = Tag.Type.Number, editable = false, userId = userId)
  def birthdayTag(team: String, userId: User.ID) = make(team = team, field = "tag_birthday", label = "生日", typ = Tag.Type.Date, editable = false, userId = userId)
  def sexTag(team: String, userId: User.ID) = make(team = team, field = "tag_sex", label = "性别", value = "男,女".some, typ = Tag.Type.SingleChoice, editable = false, userId = userId)
  def level(team: String, userId: User.ID) = make(team = team, field = "tag_level", label = "级别", value = "棋协大师,棋协候补大师,棋协一级棋士,棋协二级棋士,棋协三级棋士,棋协四级棋士,棋协五级棋士,棋协六级棋士,棋协七级棋士,棋协八级棋士,棋协九级棋士,棋协十级棋士,棋协十一级棋士,棋协十二级棋士,棋协十三级棋士,棋协十四级棋士,棋协十五级棋士,无定级"some, typ = Tag.Type.SingleChoice, editable = false, userId = userId)*/

  def make(
    team: String,
    field: String = s"tag_${makeField}",
    label: String,
    value: Option[String] = None,
    typ: Tag.Type,
    editable: Boolean = true,
    userId: User.ID
  ) = Tag(
    _id = ornicar.scalalib.Random nextString 8,
    team = team,
    field = field,
    label = label,
    value = value,
    typ = typ,
    editable = editable,
    createAt = DateTime.now,
    createBy = userId
  )

  val random = new java.util.Random
  def makeField: String = {
    def nextAlphaNum: Char = {
      val chars = "abcdefghijklmnopqrstuvwxyz"
      chars charAt (random nextInt chars.length)
    }
    Stream continually nextAlphaNum
  }.take(6).mkString

  sealed abstract class Type(val id: String, val name: String, val hasValue: Boolean, val range: Boolean)
  object Type {
    case object Text extends Type("text", "文本", false, false)
    case object Number extends Type("number", "数字", false, true)
    case object Date extends Type("date", "日期", false, true)
    case object SingleChoice extends Type("single_choice", "单选", true, false)
    //case object MultipleChoice extends Type("multiple_choice", "多选", true)

    val all = List(Text, Number, Date, SingleChoice)
    def apply(id: String) = all.find(_.id == id) err s"Bad TagType $id"
    def keySet = all.map(_.id).toSet
    def list = all.map { r => (r.id -> r.name) }
    def byId = all.map { x => x.id -> x }.toMap
  }

}