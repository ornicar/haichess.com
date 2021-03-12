package lila.home

import lila.rating.{ PerfType, Perf, Glicko }
import lila.user.User

private[home] case class HomeUser(
    id: String,
    username: String,
    lame: Boolean,
    perfMap: HomeUser.PerfMap,
    blocking: Set[String]
) {

  def perfAt(pt: PerfType): HomePerf = perfMap.get(pt.key) | HomePerf.default

  def ratingAt(pt: PerfType): Int = perfAt(pt).rating
}

private[home] object HomeUser {

  type PerfMap = Map[Perf.Key, HomePerf]

  def make(user: User, blocking: Set[User.ID]) = HomeUser(
    id = user.id,
    username = user.username,
    lame = user.lame,
    perfMap = perfMapOf(user.perfs),
    blocking = blocking
  )

  private def perfMapOf(perfs: lila.user.Perfs): PerfMap =
    perfs.perfs.collect {
      case (key, perf) if key != PerfType.Puzzle.key && perf.nonEmpty =>
        key -> HomePerf(perf.intRating, perf.provisional)
    }(scala.collection.breakOut)
}

case class HomePerf(rating: Int, provisional: Boolean)

object HomePerf {

  val default = HomePerf(Glicko.defaultIntRating, true)
}
