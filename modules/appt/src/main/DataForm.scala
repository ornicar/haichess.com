package lila.appt

import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._
import lila.common.Form._

object DataForm {

  def add(appt: Appt) = Form(mapping(
    "time" -> timeMapping(appt),
    "message" -> optional(nonEmptyText(minLength = 2, maxLength = 100))
  )(RecordData.apply)(RecordData.unapply)
    .verifying("数据已过期", _ => appt.maxDateTime.isAfterNow)
    .verifying("数据已过期", _ => appt.confirmed == 0))
    .fill(RecordData(appt.currentTime, None))

  def timeMapping(appt: Appt) =
    if (appt.isContest) futureDateTime.verifying(s"日期必须大于当前时间（+${lila.contest.Round.beforeStartMinutes}分钟）", DateTime.now.plusMinutes(lila.contest.Round.beforeStartMinutes - 1).isBefore(_))
    else futureDateTime
}

case class RecordData(time: DateTime, message: Option[String])
