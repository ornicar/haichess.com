package lila.appt

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    hub: lila.hub.Env,
    db: lila.db.Env
) {

  val CollectionAppt = config getString "collection.appt"

  private lazy val apptColl = db(CollectionAppt)

  lazy val api = new ApptApi(apptColl, hub.bus)
  lazy val jsonView = new JsonView

  //lilaBus.publish(Event.Create(challenge), 'challenge)
  system.lilaBus.subscribeFun(
    'contestRoundPublish,
    'contestBoardSetTime,
    'challenge
  ) {
      case lila.contest.actorApi.ContestRoundPublish(contest, round) => if (contest.appt) api.createByContest(contest, round)
      case lila.contest.actorApi.ContestBoardSetTime(contest, board, time) => if (contest.appt) api.setTime(contest, board, time)
      case lila.challenge.Event.Create(challenge) => if (challenge.appt && !challenge.openDest) api.createByChallenge(challenge)
      case lila.challenge.Event.Canceled(challenge) => if (challenge.appt) api.cancel(challenge)
      //case lila.challenge.Event.Accept(challenge, joinerId: Option[String]) => if (challenge.appt) joinerId.??(api.accept(challenge.id, _))
      case lila.challenge.Event.Declined(challenge, joinerId: String) => if (challenge.appt) api.cancel(challenge)
    }

}

object Env {

  lazy val current: Env = "appt" boot new Env(
    config = lila.common.PlayApp loadConfig "appt",
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current
  )
}
