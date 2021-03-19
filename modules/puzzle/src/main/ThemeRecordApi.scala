package lila.puzzle

import lila.user.User
import lila.db.dsl._
import reactivemongo.bson.Macros

private[puzzle] final class ThemeRecordApi(coll: Coll) {

  private[puzzle] implicit val ThemeRecordBSONHandler = Macros.handler[ThemeRecord]

  def byId(id: String): Fu[Option[ThemeRecord]] = coll.byId(id)

  def lastId(id: String): Fu[Int] = byId(id).map { tr =>
    tr.fold(100000)(_.puzzleId)
  }

  def upsert(
    userId: User.ID,
    puzzleId: PuzzleId
  ): Funit = {
    val record = ThemeRecord.make(userId, puzzleId)
    coll.update($id(userId), record, upsert = true).void
    /*    val emptys = List(
      "showDrawer=true",
      "showDrawer=false",
      "ratingMin=&ratingMax=&stepsMin=&stepsMax=",
      "ratingMin=&ratingMax=&stepsMin=&stepsMax=&showDrawer=true",
      "ratingMin=&ratingMax=&stepsMin=&stepsMax=&showDrawer=false"
    )
    !emptys.contains(queryString) ?? {
      val record = ThemeRecord.make(userId, puzzleId, queryString)
      coll.update($id(record.id), record, upsert = true).void
    }*/
  }

  def remove(id: String, userId: User.ID): Funit = coll.remove($doc(
    "_id" -> id,
    "userId" -> userId
  )).void

  def list(userId: User.ID): Fu[List[ThemeRecord]] =
    coll.find($doc("userId" -> userId)).sort($doc("updateAt" -> -1)).list(5)

}
