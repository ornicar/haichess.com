package lila.clazz

import lila.common.Form.ISODate
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import org.joda.time.format.DateTimeFormat

class CourseForm(val api: CourseApi) {

  def updateForm = Form(mapping(
    "date" -> ISODate.isoDate,
    "timeBegin" -> nonEmptyText,
    "timeEnd" -> nonEmptyText
  )(UpdateData.apply)(UpdateData.unapply).verifying("开始时间必须小于结束时间，并且大于当前时间", _.validTime))

}

case class UpdateData(date: DateTime, timeBegin: String, timeEnd: String) {

  private val datePattern = "yyyy-MM-dd"
  private val dateTimeFormatter = DateTimeFormat forPattern s"$datePattern HH:mm"

  def validTime =
    timeEnd.compareTo(timeBegin) > 0 && dateTimeFormatter.parseDateTime(date.toString(datePattern) + " " + timeBegin).isAfterNow

}
