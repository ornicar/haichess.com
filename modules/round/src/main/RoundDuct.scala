package lila.round

import akka.actor._
import akka.pattern.ask
import org.joda.time.DateTime
import ornicar.scalalib.Zero
import scala.concurrent.duration._

import actorApi._, round._
import chess.Color
import lila.game.{ Game, Progress, Pov, Event, Source, Player => GamePlayer }
import lila.hub.actorApi.DeployPost
import lila.hub.actorApi.map._
import lila.hub.actorApi.round.{ FishnetPlay, BotPlay, RematchYes, RematchNo, Abort, Resign }
import lila.hub.Duct
import lila.socket.UserLagCache
import makeTimeout.large

private[round] final class RoundDuct(
    dependencies: RoundDuct.Dependencies,
    gameId: Game.ID
)(implicit proxy: GameProxy) extends Duct {

  import RoundDuct._
  import dependencies._

  private[this] var takebackSituation: Option[TakebackSituation] = None

  def getGame: Fu[Option[Game]] = proxy.game

  val process: Duct.ReceiveAsync = {

    case p: HumanPlay =>
      handleHumanPlay(p) { pov =>
        if (pov.game.outoftime(withGrace = true)) finisher.outOfTime(pov.game)
        else {
          recordLag(pov)
          player.human(p, this)(pov)
        }
      } >>- {
        p.trace.finish()
        lila.mon.round.move.full.count()
      }

    case p: BotPlay =>
      handleBotPlay(p) { pov =>
        if (pov.game.outoftime(withGrace = true)) finisher.outOfTime(pov.game)
        else player.bot(p, this)(pov)
      }

    case FishnetPlay(uci, currentFen) => handle { game =>
      player.fishnet(game, uci, currentFen, this)
    } >>- lila.mon.round.move.full.count()

    case Abort(playerId) => handle(playerId) { pov =>
      pov.game.abortable ?? finisher.abort(pov)
    }

    case Resign(playerId) => handle(playerId) { pov =>
      pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
    }

    case ResignAi => handleAi(proxy.game) { pov =>
      pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
    }

    case GoBerserk(color) => handle(color) { pov =>
      pov.game.goBerserk(color) ?? { progress =>
        proxy.save(progress) >> proxy.persist(_ goBerserk pov) inject progress.events
      }
    }

    case ResignForce(playerId) => handle(playerId) { pov =>
      (pov.game.resignable && !pov.game.hasAi && pov.game.hasClock && !pov.isMyTurn) ?? {
        pov.forceResignable ?? {
          socketMap.ask[Boolean](pov.gameId)(IsGone(!pov.color, _)) flatMap {
            case true if !pov.game.variant.insufficientWinningMaterial(pov.game.board, pov.color) => finisher.rageQuit(pov.game, Some(pov.color))
            case true => finisher.rageQuit(pov.game, None)
            case _ => fuccess(List(Event.Reload))
          }
        }
      }
    }

    case DrawForce(playerId) => handle(playerId) { pov =>
      (pov.game.drawable && !pov.game.hasAi && pov.game.hasClock) ?? {
        socketMap.ask[Boolean](pov.gameId)(IsGone(!pov.color, _)) flatMap {
          case true => finisher.rageQuit(pov.game, None)
          case _ => fuccess(List(Event.Reload))
        }
      }
    }

    // checks if any player can safely (grace) be flagged
    case QuietFlag => handle { game =>
      game.outoftime(withGrace = true) ?? finisher.outOfTime(game)
    }

    // flags a specific player, possibly without grace if self
    case ClientFlag(color, from) => handle { game =>
      (game.turnColor == color) ?? {
        val toSelf = from has game.player(color).id
        game.outoftime(withGrace = !toSelf) ?? finisher.outOfTime(game)
      }
    }

    // exceptionally we don't block nor publish events
    // if the game is abandoned, then nobody is around to see it
    case Abandon => fuccess {
      proxy withGame { game =>
        game.abandoned ?? {
          if (game.abortable) finisher.other(game, _.Aborted, None)
          else finisher.other(game, _.Resign, Some(!game.player.color))
        }
      }
    }

    case DrawYes(playerRef) => handle(playerRef)(drawer.yes)
    case DrawNo(playerRef) => handle(playerRef)(drawer.no)
    case DrawClaim(playerId) => handle(playerId)(drawer.claim)
    case Cheat(color) => handle { game =>
      (game.playable && !game.imported) ?? {
        finisher.other(game, _.Cheat, Some(!color))
      }
    }
    case TooManyPlies => handle(drawer force _)

    case Threefold => proxy withGame { game =>
      drawer autoThreefold game map {
        _ foreach { pov =>
          this ! DrawClaim(pov.player.id)
        }
      }
    }

    case HoldAlert(playerId, mean, sd, ip) => handle(playerId) { pov =>
      !pov.player.hasHoldAlert ?? {
        lila.log("cheat").info(s"hold alert $ip https://haichess.com/${pov.gameId}/${pov.color.name}#${pov.game.turns} ${pov.player.userId | "anon"} mean: $mean SD: $sd")
        lila.mon.cheat.holdAlert()
        val alert = GamePlayer.HoldAlert(ply = pov.game.turns, mean = mean, sd = sd)
        proxy.persistAndSet(
          _.setHoldAlert(pov, alert),
          _.setHoldAlert(pov.color, alert)
        ) inject List.empty[Event]
      }
    }

    case RematchYes(playerRef) => handle(playerRef)(rematcher.yes)
    case RematchNo(playerRef) => handle(playerRef)(rematcher.no)

    case TakebackYes(playerRef) => handle(playerRef) { pov =>
      takebacker.yes(~takebackSituation)(pov) map {
        case (events, situation) =>
          takebackSituation = situation.some
          events
      }
    }
    case TakebackNo(playerRef) => handle(playerRef) { pov =>
      takebacker.no(~takebackSituation)(pov) map {
        case (events, situation) =>
          takebackSituation = situation.some
          events
      }
    }

    case Moretime(playerRef) => handle(playerRef) { pov =>
      moretimer(pov) flatMap {
        _ ?? { progress =>
          proxy save progress inject progress.events
        }
      }
    }

    case ForecastPlay(lastMove) => handle { game =>
      forecastApi.nextMove(game, lastMove) map { mOpt =>
        mOpt foreach { move =>
          this ! HumanPlay(game.player.id, move, false)
        }
        Nil
      }
    }

    case DeployPost => handle { game =>
      game.playable ?? {
        val freeTime = 20.seconds
        messenger.system(game, (_.untranslated("Haichess系统维护，很抱歉给您带来不便！")))
        val progress = moretimer.give(game, Color.all, freeTime)
        proxy save progress inject progress.events
      }
    }

    case AbortForMaintenance => handle { game =>
      messenger.system(game, (_.untranslated("对局因服务器维护而中止。很抱歉给您带来不便！")))
      game.playable ?? finisher.other(game, _.Aborted, None)
    }

    case AbortForce => handle { game =>
      game.playable ?? finisher.other(game, _.Aborted, None)
    }

    case NoStart => handle { game =>
      game.timeBeforeExpiration.exists(_.centis == 0) ?? finisher.noStart(game)
    }

    case ContestRoundStart(movedAt: DateTime) => handle { game =>
      game.contestRoundStart(movedAt) ?? { progress =>
        proxy.save(progress).map { _ =>
          lila.round.Env.current.scheduleExpiration(progress.game)
          lila.round.Env.current.onStart(game.id)
          progress.events
        }
      }
    }

    case ApptRoundStart(movedAt: DateTime) => handle { game =>
      game.apptStart(movedAt) ?? { progress =>
        proxy.save(progress).map { _ =>
          lila.round.Env.current.scheduleExpiration(progress.game)
          lila.round.Env.current.onStart(game.id)
          progress.events
        }
      }
    }
  }

  private[this] def recordLag(pov: Pov) =
    if ((pov.game.playedTurns & 30) == 10) {
      // Triggers every 32 moves starting on ply 10.
      // i.e. 10, 11, 42, 43, 74, 75, ...
      for {
        user <- pov.player.userId
        clock <- pov.game.clock
        lag <- clock.lag(pov.color).lagMean
      } UserLagCache.put(user, lag)
    }

  private[this] def handle[A](op: Game => Fu[Events]): Funit =
    handleGame(proxy.game)(op)

  private[this] def handle(playerId: String)(op: Pov => Fu[Events]): Funit =
    handlePov(proxy playerPov playerId)(op)

  private[this] def handleHumanPlay(p: HumanPlay)(op: Pov => Fu[Events]): Funit =
    handlePov {
      p.trace.segment("fetch", "db") {
        proxy playerPov p.playerId
      }
    }(op)

  private[this] def handleBotPlay(p: BotPlay)(op: Pov => Fu[Events]): Funit =
    handlePov(proxy playerPov p.playerId)(op)

  private[this] def handle(color: Color)(op: Pov => Fu[Events]): Funit =
    handlePov(proxy pov color)(op)

  private[this] def handlePov(pov: Fu[Option[Pov]])(op: Pov => Fu[Events]): Funit = publish {
    pov flatten "pov not found" flatMap { p =>
      if (p.player.isAi) fufail(s"player $p can't play AI") else op(p)
    }
  } recover errorHandler("handlePov")

  private[this] def handleAi(game: Fu[Option[Game]])(op: Pov => Fu[Events]): Funit = publish {
    game.map(_.flatMap(_.aiPov)) flatten "pov not found" flatMap op
  } recover errorHandler("handleAi")

  private[this] def handleGame(game: Fu[Option[Game]])(op: Game => Fu[Events]): Funit = publish {
    game flatten "game not found" flatMap op
  } recover errorHandler("handleGame")

  private[this] def publish[A](op: Fu[Events]): Funit = op.map { events =>
    if (events.nonEmpty) {
      socketMap.tell(gameId, EventList(events))
      if (events exists {
        case e: Event.Move => e.threefold
        case _ => false
      }) this ! Threefold
    }
  }

  private[this] def errorHandler(name: String): PartialFunction[Throwable, Unit] = {
    case e: ClientError =>
      logger.info(s"Round client error $name: ${e.getMessage}")
      lila.mon.round.error.client()
    case e: FishnetError =>
      logger.info(s"Round fishnet error $name: ${e.getMessage}")
      lila.mon.round.error.fishnet()
    case e: Exception => logger.warn(s"$name: ${e.getMessage}")
  }
}

object RoundDuct {

  private[round] case class Dependencies(
      messenger: Messenger,
      takebacker: Takebacker,
      moretimer: Moretimer,
      finisher: Finisher,
      rematcher: Rematcher,
      player: Player,
      drawer: Drawer,
      forecastApi: ForecastApi,
      socketMap: SocketMap
  )

  private[round] case class TakebackSituation(nbDeclined: Int, lastDeclined: Option[DateTime]) {

    def decline = TakebackSituation(nbDeclined + 1, DateTime.now.some)

    def delaySeconds = (math.pow(nbDeclined min 10, 2) * 10).toInt

    def offerable = lastDeclined.fold(true) { _ isBefore DateTime.now.minusSeconds(delaySeconds) }

    def reset = takebackSituationZero.zero
  }

  private[round] implicit val takebackSituationZero: Zero[TakebackSituation] =
    Zero.instance(TakebackSituation(0, none))
}
