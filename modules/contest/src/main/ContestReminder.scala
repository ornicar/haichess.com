package lila.contest

import lila.hub.actorApi.socket.SendTo
import lila.socket.Socket.makeMessage
import play.api.libs.json.Json
import lila.user.User

private final class ContestReminder(bus: lila.common.Bus) {

  def apply(info: Board.FullInfo, userId: User.ID) =

    bus.publish(SendTo(userId, makeMessage(
      "contestReminder",
      Json.obj(
        "contestId" -> info.contest.id,
        "roundId" -> info.round.id,
        "gameId" -> info.board.id,
        "color" -> info.board.colorOfById(userId).name,
        "name" -> info.fullName
      )
    )), 'socketUsers)

}
