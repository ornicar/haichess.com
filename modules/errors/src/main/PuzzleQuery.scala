package lila.errors

import org.joda.time.DateTime

case class PuzzleQuery(
    ratingMin: Option[Int] = None,
    ratingMax: Option[Int] = None,
    depthMin: Option[Int] = None,
    depthMax: Option[Int] = None,
    color: Option[List[String]] = None,
    rating: Option[Int] = None,
    time: Option[DateTime] = None
)
