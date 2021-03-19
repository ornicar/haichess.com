package lila.team

import org.joda.time.DateTime
import ornicar.scalalib.Random

case class TeamRating(
    _id: String,
    userId: String,
    diff: Int,
    note: String,
    typ: TeamRating.Typ,
    createAt: DateTime
) {

  def id = _id

}

object TeamRating {

  def make(
    userId: String,
    diff: Int,
    note: String,
    typ: TeamRating.Typ
  ) = TeamRating(
    _id = Random nextString 8,
    userId = userId,
    diff = diff,
    note = note,
    typ = typ,
    createAt = DateTime.now
  )

  sealed class Typ(val id: String, val name: String)
  object Typ {
    case object Game extends Typ("game", "对局")
    case object Contest extends Typ("contest", "线上比赛")
    case object OffContest extends Typ("offContest", "线下比赛")
    case object Setting extends Typ("setting", "管理员设置")

    val all = List(Game, Contest, OffContest, Setting)
    val byId = all map { v => (v.id, v) } toMap
    def apply(id: String): Typ = byId.get(id) err s"Bad Typ $id"
  }

}