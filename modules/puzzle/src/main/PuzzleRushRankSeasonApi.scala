package lila.puzzle

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime
import scala.concurrent.duration._

private[puzzle] final class PuzzleRushRankSeasonApi(
    puzzleRushRankSeasonColl: Coll,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import PuzzleRushRankSeason.PuzzleRushRankMonthBSONHandler

  def id(mode: PuzzleRush.Mode, season: Int, userId: User.ID) = s"$season@${mode.id}@$userId"

  def byId(rush: PuzzleRush): Fu[Option[PuzzleRushRankSeason]] =
    puzzleRushRankSeasonColl.byId(PuzzleRushRankSeason.makeId(rush))

  def createBySub(rush: PuzzleRush): Funit = byId(rush) flatMap {
    case None => create(rush)
    case Some(r) => if (rush.result.??(_.win) > r.win) {
      create(rush)
    } else funit
  }

  def create(rush: PuzzleRush): Funit =
    puzzleRushRankSeasonColl.update(
      $id(PuzzleRushRankSeason.makeId(rush)),
      PuzzleRushRankSeason.make(rush),
      upsert = true
    ).void

  def userRank(mode: PuzzleRush.Mode, season: Int, userId: User.ID) =
    puzzleRushRankSeasonColl.byId(id(mode, season, userId)) flatMap { rank =>
      rank.fold(fuccess(-1, -1)) { r =>
        userRankNo(mode, season, r.updateTime, r.win).map { no =>
          ((no + 1), r.win)
        }
      }
    }

  def userRankNo(mode: PuzzleRush.Mode, season: Int, updateTime: DateTime, win: Int): Fu[Int] = puzzleRushRankSeasonColl.countSel(
    $doc(
      "mode" -> mode.id,
      "season" -> season,
      "win" $gt win
    )
  ) zip puzzleRushRankSeasonColl.countSel(
      $doc(
        "mode" -> mode.id,
        "season" -> season,
        "win" -> win,
        "updateTime" $gt updateTime
      )
    ) map (gtAndEq => gtAndEq._1 + gtAndEq._2)

  def rankList(mode: PuzzleRush.Mode, season: Int, userIds: Option[List[User.ID]] = None): Fu[List[PuzzleRushRankSeason]] =
    puzzleRushRankSeasonColl.find(
      $doc(
        "mode" -> mode.id,
        "season" -> season
      ) ++ userIds.?? { uids =>
          $doc("userId" -> $in(uids: _*))
        }
    )
      .sort($doc("win" -> -1, "updateTime" -> -1))
      .list[PuzzleRushRankSeason](20)

  def seasonRankList(mode: PuzzleRush.Mode, season: Int): Fu[List[(User.ID, Int)]] = {
    puzzleRushRankSeasonColl.find(
      $doc(
        "mode" -> mode.id,
        "season" -> season,
        "win" $gt 0
      ), $doc("userId" -> 1, "win" -> 1)
    ).sort($doc("win" -> -1)).list[Bdoc](5).map(_.flatMap { obj =>
        obj.getAs[String]("userId") flatMap { id =>
          obj.getAs[Int]("win") map {
            id -> _
          }
        }
      })
  }

  import PuzzleRush.Mode._
  private val seasonRankTop5Cache = asyncCache.single[List[(PuzzleRush.Mode, List[(User.ID, Int)])]](
    name = "puzzleRush.seasonRankTop5",
    f = {
      val season = PuzzleRush.makeSeason
      for {
        threeMinutes <- seasonRankList(ThreeMinutes, season)
        fiveMinutes <- seasonRankList(FiveMinutes, season)
        survival <- seasonRankList(Survival, season)
      } yield List(ThreeMinutes -> threeMinutes, FiveMinutes -> fiveMinutes, Survival -> survival)
    },
    expireAfter = _.ExpireAfterWrite(30 minute)
  )

  def seasonRankTop5: Fu[List[(PuzzleRush.Mode, List[(User.ID, Int)])]] = seasonRankTop5Cache.get

}
