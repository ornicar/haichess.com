package lila.insight

import org.joda.time.DateTime

case class DateRange(min: DateTime, max: DateTime)

case class Period(days: Int) {
  def max = DateTime.now
  def min = max minusDays days

  override def toString = days match {
    case 1 => "最近24小时"
    case d if d < 14 => s"最近${d}天"
    case d if d == 14 => s"最近2周"
    case d if d < 30 => s"最近${d / 7}周"
    case d if d == 30 => s"最近1个月"
    case d if d < 365 => s"最近${d / 30}月"
    case d if d == 365 => s"最近1年"
    case d => s"最近${d / 365}年"
  }
}

object Period {

  val selector: List[Period] = List(
    1, 2, 7,
    15, 30, 60, 182,
    365 /*, 365 * 2, 365 * 3, 365 * 5, 365 * 10*/
  ) map Period.apply
}
