package lila.clazz

import HomeworkCommon._
import lila.puzzle.Puzzle.UserResult
import lila.puzzle.PuzzleRush
import lila.game.Game
import lila.user.User

case class HomeworkCommon(items: Map[HomeworkCommonItem, Int]) {

  def hasContent = items.nonEmpty

  def itemList = items.keySet.toList

}

object HomeworkCommon {

  val maxItem = 4

  sealed abstract class HomeworkCommonItem(val id: String, val name: String, val number: Boolean, val source: HomeworkCommonItemSource)
  object HomeworkCommonItem {
    import HomeworkCommonItemSource._
    case object PuzzleRating extends HomeworkCommonItem("puzzleRating", "战术题分数", false, PuzzleItem)
    case object UltraBulletRating extends HomeworkCommonItem("ultraBulletRating", "闪电棋分数", false, GameItem)
    case object BulletRating extends HomeworkCommonItem("bulletRating", "超快棋分数", false, GameItem)
    case object BlitzRating extends HomeworkCommonItem("blitzRating", "快棋分数", false, GameItem)
    case object RapidRating extends HomeworkCommonItem("rapidRating", "中速棋分数", false, GameItem)
    case object ClassicalRating extends HomeworkCommonItem("classicalRating", "慢棋分数", false, GameItem)

    case object PuzzleNumber extends HomeworkCommonItem("puzzleNumber", "战术题数量", true, PuzzleItem)
    case object UltraBulletNumber extends HomeworkCommonItem("ultraBulletNumber", "闪电棋数量", true, GameItem)
    case object BulletNumber extends HomeworkCommonItem("bulletNumber", "超快棋数量", true, GameItem)
    case object BlitzNumber extends HomeworkCommonItem("blitzNumber", "快棋数量", true, GameItem)
    case object RapidNumber extends HomeworkCommonItem("rapidNumber", "中速棋数量", true, GameItem)
    case object ClassicalNumber extends HomeworkCommonItem("classicalNumber", "慢棋数量", true, GameItem)

    case object PuzzleRush3Number extends HomeworkCommonItem("threeMinutesNumber", "战术冲刺3分钟", true, RushItem)
    case object PuzzleRush5Number extends HomeworkCommonItem("fiveMinutesNumber", "战术冲刺5分钟", true, RushItem)

    val all = List(
      PuzzleRating, UltraBulletRating, BulletRating, BlitzRating, RapidRating, ClassicalRating,
      PuzzleNumber, UltraBulletNumber, BulletNumber, BlitzNumber, RapidNumber, ClassicalNumber,
      PuzzleRush3Number, PuzzleRush5Number
    )

    def byId = all map { v => (v.id, v) } toMap
    def selects = all.map { v => (v.id, v.name) }
    def apply(id: String): HomeworkCommonItem = byId get id err s"Bad HomeworkCommonItem $id"

  }

  sealed trait HomeworkCommonItemSource
  object HomeworkCommonItemSource {
    case object GameItem extends HomeworkCommonItemSource
    case object PuzzleItem extends HomeworkCommonItemSource
    case object RushItem extends HomeworkCommonItemSource
  }
}

case class HomeworkCommonDiff(id: String, diff: Int)
case class HomeworkCommonResult(num: Int, rating: Int, nowRating: Int, diffs: List[HomeworkCommonDiff])
case class HomeworkCommonWithResult(item: HomeworkCommonItem, num: Int, result: Option[HomeworkCommonResult]) {

  def finishPuzzle(res: UserResult): HomeworkCommonWithResult = copy(
    result = result.fold(
      HomeworkCommonResult(
        num = if (res.result.win) 1 else 0,
        rating = res.rating._1,
        nowRating = res.rating._2,
        diffs = List(
          HomeworkCommonDiff(
            id = res.puzzleId.toString,
            diff = res.rating._2 - res.rating._1
          )
        )
      )
    ) { old =>
        old.copy(
          num = if (res.result.win) old.num + 1 else old.num,
          nowRating = res.rating._2,
          diffs = old.diffs :+ HomeworkCommonDiff(
            id = res.puzzleId.toString,
            diff = res.rating._2 - res.rating._1
          )
        )
      }.some
  )

  def finishRush(rush: PuzzleRush): HomeworkCommonWithResult =
    if (item.id == s"${rush.mode.id}Number") {
      copy(
        result = result.fold(
          HomeworkCommonResult(
            num = 1,
            rating = 0,
            nowRating = 0,
            diffs = List(
              HomeworkCommonDiff(
                id = rush.id,
                diff = 0
              )
            )
          )
        ) { old =>
            old.copy(
              num = old.num + 1,
              nowRating = 0,
              diffs = old.diffs :+ HomeworkCommonDiff(
                id = rush.id,
                diff = 0
              )
            )
          }.some
      )
    } else this

  def finishGame(game: Game, userId: User.ID): HomeworkCommonWithResult = {
    if (item.id == s"${game.speed.key}Rating" || item.id == s"${game.speed.key}Number") {
      val diff = game.playerByUserId(userId).map(p => p.ratingDiff | 0) | 0
      val rating = game.playerByUserId(userId).map(p => p.rating | 0) | 0
      val nowRating = rating + diff
      copy(
        result = result.fold(
          HomeworkCommonResult(
            num = 1,
            rating = rating,
            nowRating = nowRating,
            diffs = List(
              HomeworkCommonDiff(
                id = game.id,
                diff = diff
              )
            )
          )
        ) { old =>
            old.copy(
              num = old.num + 1,
              nowRating = nowRating,
              diffs = old.diffs :+ HomeworkCommonDiff(
                id = game.id,
                diff = diff
              )
            )
          }.some
      )
    } else this
  }

  import HomeworkCommonItem._
  def link = item match {
    case PuzzleRating | PuzzleNumber => "/training"
    case UltraBulletRating | BulletRating | BlitzRating | RapidRating | ClassicalRating | UltraBulletNumber | BulletNumber | BlitzNumber | RapidNumber | ClassicalNumber => "/lobby#friend"
    case PuzzleRush3Number | PuzzleRush5Number => "/training/rush"
  }

  def targetFormat =
    if (item.number) num.toString
    else s"+$num"

  def currentResult: Option[Int] =
    if (item.number) result.map(_.num)
    else result.map(r => r.nowRating - r.rating)

  def isComplete = currentResult.??(_ >= num)

}
