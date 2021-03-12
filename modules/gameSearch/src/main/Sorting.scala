package lila.gameSearch

case class Sorting(f: String, order: String)

object Sorting {

  val fields = List(
    Fields.date -> "日期",
    Fields.turns -> "步数",
    Fields.averageRating -> "积分"
  )

  val orders = List(
    "desc" -> "倒序",
    "asc" -> "正序"
  )

  val default = Sorting(Fields.date, "desc")
}
