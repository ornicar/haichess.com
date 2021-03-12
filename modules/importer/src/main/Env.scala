package lila.importer

import com.typesafe.config.Config

final class Env(
    config: Config,
    scheduler: akka.actor.Scheduler
) {

  private val Delay = config duration "delay"

  val batchMaxSize = 20

  lazy val forms = new DataForm(batchMaxSize)

  lazy val importer = new Importer(Delay, scheduler)

  lazy val puzzleGameImporter = new PuzzleGameImporter(importer)
}

object Env {

  lazy val current = "importer" boot new Env(
    config = lila.common.PlayApp loadConfig "importer",
    scheduler = lila.common.PlayApp.system.scheduler
  )
}
