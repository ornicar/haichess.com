package lila.resource

import lila.user.User
import org.joda.time.DateTime
import ornicar.scalalib.Random
import Capsule._

case class Capsule(
    _id: ID,
    name: String,
    tags: List[String],
    desc: Option[String],
    puzzles: Set[Int],
    puzzlesWithoutRemove: Set[Int],
    enabled: Boolean,
    createdAt: DateTime,
    updatedAt: DateTime,
    createdBy: User.ID,
    updatedBy: User.ID
) {

  def id = _id

  def total = puzzlesWithoutRemove.size

  def hasPuzzle = !puzzlesWithoutRemove.isEmpty

  def isCreator(user: String) = user == createdBy

  def status = if (enabled) "活动" else "锁定"

}

object Capsule {

  type ID = String

  def make(
    user: User.ID,
    name: String,
    desc: Option[String],
    tags: List[String]
  ) = Capsule(
    _id = Random nextString 8,
    name = name,
    tags = tags,
    desc = desc,
    puzzles = Set.empty[Int],
    puzzlesWithoutRemove = Set.empty[Int],
    enabled = true,
    createdAt = DateTime.now,
    updatedAt = DateTime.now,
    createdBy = user,
    updatedBy = user
  )

}
