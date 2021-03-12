package lila.resource

case class Sorting(field: String, order: Int)

object Sorting {

  val fields = List(
    "rating" -> "难度"
  )

  val orders = List(
    1 -> "正序",
    -1 -> "倒序"
  )

  val default = Sorting("rating", 1)
}
