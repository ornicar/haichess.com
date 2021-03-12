package lila.contest

import ornicar.scalalib.Random

case class Forbidden(
    _id: String,
    name: String,
    contestId: String,
    playerIds: List[String]
) {

  def id = _id

  def withPlayer(players: List[PlayerWithUser]) =
    playerIds.map { playerId =>
      (playerId, players.find(p => p.playerId == playerId) err s"can not find player $playerId")
    }

  def pairs = playerIds.flatMap { p1 =>
    playerIds.map { p2 =>
      p1 -> p2
    }
  } filterNot (p => p._1 == p._2) map {
    case (p1, p2) => if (p1 > p2) p2 -> p1 else p1 -> p2
  } toSet

}

object Forbidden {

  def make(
    name: String,
    contestId: String,
    playerIds: List[String]
  ): Forbidden = Forbidden(
    _id = Random nextString 8,
    name = name,
    contestId = contestId,
    playerIds = playerIds
  )

  case class WithUser(forbidden: Forbidden, players: List[(String, PlayerWithUser)])

}
