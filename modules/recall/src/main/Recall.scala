package lila.recall

import lila.game.Game
import lila.user.User
import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Recall(
    _id: Recall.ID,
    name: String,
    gameId: Game.ID,
    turns: Option[Int],
    color: Option[chess.Color],
    readonly: Boolean,
    deleted: Boolean,
    createBy: User.ID,
    createAt: DateTime
) {

  def id = _id

  def isCreator(user: String) = user == createBy

}

object Recall {

  type ID = String

  def make(
    name: String,
    gameId: Game.ID,
    turns: Option[Int],
    color: Option[chess.Color],
    userId: User.ID,
    readonly: Boolean = false
  ) = Recall(
    _id = Random nextString 8,
    name = name,
    gameId = gameId,
    turns = turns,
    color = color,
    readonly = readonly,
    deleted = false,
    createBy = userId,
    createAt = DateTime.now
  )

  def makeSyntheticRecall =
    Recall(
      _id = "synthetic",
      name = "记谱",
      gameId = "synthetic",
      turns = None,
      color = None,
      readonly = false,
      deleted = false,
      createBy = User.lichessId,
      createAt = DateTime.now
    )

  def makeTemporaryRecall(turns: Option[Int], color: Option[String], title: Option[String]) =
    Recall(
      _id = "temporary",
      name = title | "记谱",
      gameId = "temporary",
      turns = turns,
      color = color.??(chess.Color(_)),
      readonly = true,
      deleted = true,
      createBy = User.lichessId,
      createAt = DateTime.now
    )

}
