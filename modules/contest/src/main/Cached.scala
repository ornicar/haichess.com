package lila.contest

import scala.concurrent.duration._
import lila.memo._

private[contest] final class Cached(implicit system: akka.actor.ActorSystem) {

  val nameCache = new Syncache[String, Option[String]](
    name = "contest.name",
    compute = id => ContestRepo byId id map2 { (contest: Contest) => contest.fullName },
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(1 hour),
    logger = logger
  )

  def name(id: String): Option[String] = nameCache sync id

}
