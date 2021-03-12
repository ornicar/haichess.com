package lila.member

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime

case class MemberActiveRecordApi(coll: Coll) {

  import BSONHandlers.MemberRecRecordBSONHandler

  def isPuzzleContinue(user: User): Fu[Boolean] = {
    if (user.isCoachOrTeam) fuccess(true)
    else {
      val maxCount = user.memberLevel.permissions.puzzle
      byUserId(user.id) map {
        case None => 0
        case Some(r) => r.puzzle | 0
      } map { _ < maxCount }
    }
  }

  def isThemePuzzleContinue(user: User): Fu[Boolean] = {
    if (user.isCoachOrTeam) fuccess(true)
    else {
      val maxCount = user.memberLevel.permissions.themePuzzle
      byUserId(user.id) map {
        case None => 0
        case Some(r) => r.themePuzzle | 0
      } map { _ < maxCount }
    }
  }

  def isPuzzleRushContinue(user: User): Fu[Boolean] = {
    if (user.isCoachOrTeam) fuccess(true)
    else {
      val maxCount = user.memberLevel.permissions.puzzleRush
      byUserId(user.id) map {
        case None => 0
        case Some(r) => r.puzzleRush | 0
      } map { _ < maxCount }
    }
  }

  def byUserId(userId: User.ID): Fu[Option[MemberActiveRecord]] =
    coll.byId[MemberActiveRecord](MemberActiveRecord.makeId(userId, DateTime.now))

  def updateRecord(
    userId: User.ID,
    puzzle: Boolean = false,
    themePuzzle: Boolean = false,
    puzzleRush: Boolean = false
  ): Funit = {
    val now = DateTime.now
    val id = MemberActiveRecord.makeId(userId, now)
    coll.update(
      $id(id),
      $set(
        "userId" -> userId,
        "date" -> now.toString("yyyyMMdd").toInt,
        "updateAt" -> now
      ) ++ puzzle.?? { $inc("puzzle" -> 1) } ++ themePuzzle.?? { $inc("themePuzzle" -> 1) } ++ puzzleRush.?? { $inc("puzzleRush" -> 1) },
      upsert = true
    ).void
  }

}
