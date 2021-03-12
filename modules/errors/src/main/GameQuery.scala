package lila.errors

import org.joda.time.DateTime

case class GameQuery(
    gameAtMin: Option[DateTime] = None,
    gameAtMax: Option[DateTime] = None,
    color: Option[List[String]] = None,
    opponent: Option[String] = None,
    phase: Option[String] = None,
    judgement: Option[String] = None,
    eco: Option[String] = None
)
