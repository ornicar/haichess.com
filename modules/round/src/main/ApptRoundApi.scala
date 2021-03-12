package lila.round

import lila.game.GameRepo
import lila.hub.DuctMap
import org.joda.time.DateTime
import lila.hub.actorApi.socket.SendTo
import lila.socket.Socket.makeMessage
import play.api.libs.json.Json
import lila.user.User
import lila.game.Game
import lila.round.actorApi.round.ApptRoundStart

final class ApptRoundApi(roundMap: DuctMap[RoundDuct], bus: lila.common.Bus) {

  def launch: Funit = {
    GameRepo.apptPending.flatMap { list =>
      val gameIds = list.map(_.id)
      val now = DateTime.now
      GameRepo.gameLaunchBatch(gameIds).map { _ =>
        gameIds.foreach { gameId =>
          roundMap.tell(gameId, ApptRoundStart(now))
        }
      }
    }
  }

  def remind: Funit =
    GameRepo.apptRemind map { list =>
      list.foreach { game =>
        game.userIds.foreach { userId =>
          GameRepo.setApptRemind(game.id) >>- publishRemind(userId, game)
        }
      }
    }

  def publishRemind(userId: User.ID, game: Game): Unit = {
    bus.publish(SendTo(userId, makeMessage(
      "gameApptReminder",
      Json.obj(
        "gameId" -> game.id,
        "opponent" -> game.opponentByUserId(userId).map(_.userId),
        "color" -> game.playerByUserId(userId).map(_.color.name)
      )
    )), 'socketUsers)
  }

}
