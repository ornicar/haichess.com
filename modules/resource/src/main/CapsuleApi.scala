package lila.resource

import lila.db.dsl._
import reactivemongo.bson.Macros
import lila.db.dsl.bsonArrayToListHandler
import org.joda.time.DateTime
import lila.user.User

final class CapsuleApi(coll: Coll, bus: lila.common.Bus) {

  implicit val stringArrayHandler = bsonArrayToListHandler[String]
  implicit val capsuleHandler = Macros.handler[Capsule]

  def byId(id: Capsule.ID): Fu[Option[Capsule]] = coll.byId[Capsule](id)

  def tags(user: User.ID): Fu[Set[String]] =
    coll.distinct[String, Set]("tags", $doc("createdBy" -> user).some)

  def byIds(ids: List[Capsule.ID]): Fu[List[Capsule]] =
    coll.byIds(ids)

  def list(user: User.ID, enabled: Option[Int] = None, name: Option[String] = None, tags: Option[List[String]] = None): Fu[List[Capsule]] = {
    var doc = $doc("createdBy" -> user)
    enabled.foreach { e =>
      doc = doc ++ $doc("enabled" -> (e == 1))
    }
    name.foreach { n =>
      doc = doc ++ $doc("name" $regex (n, "i"))
    }
    tags.foreach { t =>
      doc = doc ++ $doc("tags" -> $in(t: _*))
    }
    coll.find(doc).sort($sort desc "updatedAt").list(1000)
  }

  def create(user: User.ID, data: DataForm.capsule.CapsuleData): Funit =
    coll.insert(
      Capsule.make(
        user,
        data.name,
        data.desc,
        data.makeTags
      )
    ).void

  def update(id: Capsule.ID, capsule: Capsule, data: DataForm.capsule.CapsuleData): Funit =
    coll.update(
      $id(id),
      capsule.copy(
        name = data.name,
        desc = data.desc,
        tags = data.makeTags
      )
    ).void

  def remove(id: String): Funit = coll.remove($id(id)).void

  def enable(id: String, enabled: Boolean): Funit =
    coll.updateField($id(id), "enabled", enabled).void

  def addPuzzle(capsule: Capsule, idSet: Set[Int]): Funit =
    coll.update(
      $id(capsule.id),
      capsule.copy(
        puzzles = capsule.puzzles ++ idSet,
        puzzlesWithoutRemove = capsule.puzzlesWithoutRemove ++ idSet,
        updatedAt = DateTime.now
      )
    ).void

  def delPuzzle(capsule: Capsule, idSet: Set[Int]): Funit =
    coll.update(
      $id(capsule.id),
      capsule.copy(
        puzzles = capsule.puzzles -- idSet,
        puzzlesWithoutRemove = capsule.puzzlesWithoutRemove -- idSet,
        updatedAt = DateTime.now
      )
    ).void

  def removePuzzle(ids: List[Int]): Funit =
    coll.update($doc("puzzlesWithoutRemove" -> $in(ids: _*)), $pull("puzzlesWithoutRemove" -> $in(ids: _*)), multi = true).void

}
