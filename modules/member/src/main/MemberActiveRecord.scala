package lila.member

import lila.user.User
import org.joda.time.DateTime

case class MemberActiveRecord(
    _id: String,
    userId: User.ID,
    date: Int,
    puzzle: Option[Int],
    themePuzzle: Option[Int],
    puzzleRush: Option[Int],
    updateAt: DateTime
) {

  def id: String = _id

}

object MemberActiveRecord {

  def makeId(userId: User.ID, date: DateTime) = date.toString("yyyyMMdd") + "@" + userId

  def make(
    userId: User.ID,
    puzzle: Option[Int],
    themePuzzle: Option[Int],
    puzzleRush: Option[Int]
  ) = {
    val now = DateTime.now
    MemberActiveRecord(
      _id = makeId(userId, now),
      userId = userId,
      date = now.toString("yyyyMMdd").toInt,
      puzzle = puzzle,
      themePuzzle = themePuzzle,
      puzzleRush = puzzleRush,
      updateAt = now
    )
  }

}
