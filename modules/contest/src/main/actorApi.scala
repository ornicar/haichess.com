package lila.contest
package actorApi

import org.joda.time.DateTime

case class ContestRoundPublish(contest: Contest, round: Round)
case class ContestBoardSetTime(contest: Contest, board: Board, time: DateTime)
