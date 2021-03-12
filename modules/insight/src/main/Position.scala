package lila.insight

sealed trait Position {

  def tellNumber: String

  def name = toString.toLowerCase
}

object Position {

  case object Game extends Position {

    val tellNumber = "对局数"
  }

  case object Move extends Position {

    val tellNumber = "步数"
  }
}
