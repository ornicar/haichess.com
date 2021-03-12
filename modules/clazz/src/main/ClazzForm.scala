package lila.clazz

import lila.common.Form._
import lila.user.User
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import lila.clazz.Clazz.ClazzType
import play.api.data.validation.Constraints

class ClazzForm(val api: ClazzApi) {

  import ClazzForm._

  def create(user: User, id: Option[Clazz.ID]) = Form(mapping(
    "name" -> clazzNameField(user, id),
    "color" -> nonEmptyText(minLength = 7, maxLength = 7),
    "team" -> optional(text(minLength = 6, maxLength = 6)),
    "clazzType" -> stringIn(clazzTypeChoices),
    "weekClazz" -> optional(mapping(
      "dateStart" -> ISODate.isoDate,
      "dateEnd" -> ISODate.isoDate,
      "times" -> number(min = 1, max = 52),
      "weekCourse" -> list(mapping(
        "week" -> number,
        "timeBegin" -> nonEmptyText,
        "timeEnd" -> nonEmptyText
      )(WeekCourse.apply)(WeekCourse.unapply))
    )(WeekClazz.apply)(WeekClazz.unapply)
      .verifying("没有设置课程", !_.weekCourse.isEmpty)
      .verifying("上课时间存在冲突", _.valid)),
    "trainClazz" -> optional(mapping(
      "dateStart" -> ISODate.isoDate,
      "dateEnd" -> ISODate.isoDate,
      "times" -> number(min = 1, max = 52),
      "trainCourse" -> list(mapping(
        "dateStart" -> ISODate.isoDate,
        "dateEnd" -> ISODate.isoDate,
        "timeBegin" -> nonEmptyText,
        "timeEnd" -> nonEmptyText
      )(TrainCourse.apply)(TrainCourse.unapply))
    )(TrainClazz.apply)(TrainClazz.unapply)
      .verifying("没有设置课程", !_.trainCourse.isEmpty)
      .verifying("上课时间存在冲突", _.valid))
  )(CreateData.apply)(CreateData.unapply)
    .verifying("开始日期必须与选定上课时间一致", _.validFirstDay))

  val clazzNameConstraints = Seq(
    Constraints nonEmpty,
    Constraints minLength 2,
    Constraints maxLength 30
  )

  def clazzNameField(user: User, id: Option[Clazz.ID]) = text.verifying(clazzNameConstraints: _*)
  //.verifying("班级名称重复", name => !api.nameExists(user, name, id).awaitSeconds(3))
}

object ClazzForm {

  val clazzTypeChoices = Seq(("week" -> "周定时"), ("train" -> "集训"))

  case class CreateData(
      name: String,
      color: String,
      team: Option[String],
      clazzType: String,
      weekClazz: Option[WeekClazz],
      trainClazz: Option[TrainClazz]
  ) {
    //clazzType.copy(dateEnd = clazzType.getDateEnd)
    def toClazz(user: User): Clazz = {
      val ct = ClazzType(clazzType)
      Clazz.make(
        user = user,
        name = name,
        color = color,
        team = team,
        clazzType = ct,
        weekClazz = ct match {
          case ClazzType.Week => weekClazz.map { wc =>
            wc.copy(
              dateEnd = wc.getDateEnd
            )
          }
          case ClazzType.Train => None
        },
        trainClazz = ct match {
          case ClazzType.Week => None
          case ClazzType.Train => trainClazz.map { tc =>
            val course = tc.toCourseFromTrain("", "")
            tc.copy(
              dateStart = course.head.date,
              dateEnd = course.reverse.head.date,
              times = course.size
            )
          }
        }
      )
    }

    def withUpdate(clazz: Clazz): Clazz = clazz.copy(
      name = name,
      color = color,
      team = team,
      clazzType = ClazzType(clazzType),
      weekClazz = weekClazz,
      trainClazz = trainClazz,
      updatedAt = DateTime.now()
    )

    def validFirstDay = clazzType === "train" || weekClazz.??(_.validFirstDay)

  }

  object CreateData {

    def default = new CreateData(
      name = "",
      color = "#4d4d4d",
      team = None,
      clazzType = "week",
      weekClazz = WeekClazz(
        dateStart = DateTime.now(),
        dateEnd = DateTime.now(),
        times = 10,
        weekCourse = List(
          WeekCourse(
            week = 1,
            timeBegin = "16:00",
            timeEnd = "18:00"
          )
        )
      ).some,
      trainClazz = TrainClazz(
        dateStart = DateTime.now(),
        dateEnd = DateTime.now(),
        times = 10,
        trainCourse = List(
          TrainCourse(
            dateStart = DateTime.now(),
            dateEnd = DateTime.now(),
            timeBegin = "16:00",
            timeEnd = "18:00"
          )
        )
      ).some
    )

    def byClazz(clazz: Clazz) = CreateData(
      name = clazz.name,
      color = clazz.color,
      team = clazz.team,
      clazzType = clazz.clazzType.id,
      weekClazz = clazz.weekClazz,
      trainClazz = clazz.trainClazz
    )

  }

}
