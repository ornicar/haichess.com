package lila.puzzle

import org.joda.time.DateTime

case class PuzzleErrors(rating: Int, time: DateTime)

object PuzzleErrors {

  def make(rating: Option[Int], time: Option[DateTime]): Option[PuzzleErrors] =
    (rating |@| time).tupled.map {
      case (r, t) => PuzzleErrors(r, t)
    }

}
