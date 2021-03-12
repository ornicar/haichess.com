package lila.puzzle

import lila.user.User
import PuzzleRush.{ BSONFields => F }
import akka.actor.ActorSystem
import lila.common.MaxPerPage
import org.joda.time.DateTime
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.hub.actorApi.puzzle.StartPuzzleRush

import scala.concurrent.duration._

private[puzzle] final class PuzzleRushApi(
    puzzleRushColl: Coll,
    puzzleRoundApi: PuzzleRoundApi,
    finisher: Finisher,
    system: ActorSystem
) {

  import PuzzleRush.PuzzleRushBSONHandler

  def byId(id: PuzzleRush.ID) = puzzleRushColl.byId(id)

  def page(userId: User.ID, page: Int, mode: PuzzleRush.Mode, order: PuzzleRush.Order): Fu[Paginator[PuzzleRush]] = {
    val adapter = new Adapter[PuzzleRush](
      collection = puzzleRushColl,
      selector = $doc(
        F.userId -> userId,
        F.mode -> mode.id,
        F.status $gte PuzzleRush.Status.Timeout.id
      ),
      projection = $empty,
      sort = $sort desc order.key
    )
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

  def rankList(mode: PuzzleRush.Mode, userId: User.ID): Fu[List[PuzzleRush]] = puzzleRushColl.find(
    $doc(
      F.userId -> userId,
      F.mode -> mode.id,
      F.status $gte PuzzleRush.Status.Timeout.id
    )
  ).sort($doc(F.win -> -1, F.endTime -> -1)).list(20)

  def todayRankList(mode: PuzzleRush.Mode, day: DateTime, userId: User.ID): Fu[List[PuzzleRush]] = puzzleRushColl.find(
    $doc(
      F.userId -> userId,
      F.mode -> mode.id,
      F.status $gte PuzzleRush.Status.Timeout.id,
      F.endTime $gte day.withTimeAtStartOfDay(),
      F.endTime $lte day.withTime(23, 59, 59, 999)
    )
  ).sort($doc(F.win -> -1, F.endTime -> -1)).list(20)

  def seasonRankList(mode: PuzzleRush.Mode, season: Int, userId: User.ID): Fu[List[PuzzleRush]] = puzzleRushColl.find(
    $doc(
      F.userId -> userId,
      F.mode -> mode.id,
      F.status $gte PuzzleRush.Status.Timeout.id,
      F.season -> season
    )
  ).sort($doc(F.win -> -1, F.endTime -> -1)).list(20)

  def startedList(me: User): Fu[List[PuzzleRush]] = puzzleRushColl.find(
    $doc(
      F.userId -> me.id,
      F.status -> PuzzleRush.Status.Started.id
    )
  ).sort($sort desc F.createTime).list()

  def create(puzzleRush: PuzzleRush): Fu[PuzzleRush] =
    puzzleRushColl.insert(puzzleRush).inject(puzzleRush) >>- system.lilaBus.publish(StartPuzzleRush(puzzleRush.id, puzzleRush.userId), 'startPuzzleRush)

  def begin(rush: PuzzleRush): Funit =
    puzzleRushColl.update(
      $id(rush.id),
      $set(
        F.startTime -> DateTime.now,
        F.status -> PuzzleRush.Status.Started.id
      )
    ).map { _ =>
        system.lilaBus.publish(rush, 'beginRush);
      }.void

  def finish(rush: PuzzleRush, status: PuzzleRush.Status): Fu[PuzzleRush.Result] =
    puzzleRoundApi.rushRounds(rush.id) flatMap { rounds =>
      val endTime = DateTime.now
      val result = PuzzleRush.Result(rush.copy(status = status, endTime = endTime.some), rounds)
      puzzleRushColl.update(
        $id(rush.id),
        $set(
          F.endTime -> endTime,
          F.status -> status.id,
          F.result -> PuzzleRush.ResultBSONHandler.write(result)
        )
      ).map { _ =>
          system.lilaBus.publish(rush.copy(endTime = endTime.some, status = status, result = result.some), 'finishRush);
        } inject (result)
    }

  def round(rushId: PuzzleRush.ID, puzzle: Puzzle, user: User, result: Result, seconds: Int, lines: List[ResultNode], timeout: Option[Boolean], mobile: Boolean): Funit = {
    finisher.incPuzzleAttempts(puzzle)
    val puzzleRating = puzzle.perf.intRating
    val userRating = user.perfs.puzzle.intRating
    system.lilaBus.publish(
      Puzzle.UserResult(
        puzzle.id,
        user.id,
        result,
        userRating -> userRating,
        puzzleRating -> puzzleRating,
        seconds = seconds,
        lines = lines,
        timeout = timeout,
        source = "rush",
        rushId = rushId.some
      ), 'finishPuzzle
    )
    funit
  }

  def scheduleFinish(rush: PuzzleRush) =
    if (rush.mode != PuzzleRush.Mode.Survival) {
      system.scheduler.scheduleOnce(rush.clockFiniteDuration + (3 seconds)) {
        byId(rush.id) foreach {
          _.filter(_.outOfTime) foreach { rush =>
            finish(rush, PuzzleRush.Status.Timeout)
          }
        }
      }
    }

}
