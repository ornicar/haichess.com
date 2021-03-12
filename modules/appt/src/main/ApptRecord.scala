package lila.appt

import org.joda.time.DateTime
import lila.user.User
import ApptRecord._
import chess.Color

case class ApptRecord(
    id: String,
    time: DateTime,
    message: Option[String],
    current: Boolean,
    whiteStatus: Status,
    blackStatus: Status,
    applyBy: Option[User.ID]
) {

  def isSystem: Boolean = applyBy.isEmpty

  def isConfirmed(color: Color): Boolean =
    if (color == Color.white) whiteStatus == Status.Confirmed else blackStatus == Status.Confirmed

  def allConfirmed: Boolean = whiteStatus == Status.Confirmed && blackStatus == Status.Confirmed

  def withWhiteConfirmed: ApptRecord = copy(whiteStatus = Status.Confirmed)

  def withBlackConfirmed: ApptRecord = copy(blackStatus = Status.Confirmed)

  def withConfirmed(color: chess.Color): ApptRecord =
    if (color == chess.Color.white) withWhiteConfirmed else withBlackConfirmed

}

object ApptRecord {

  def make(
    time: DateTime,
    message: Option[String],
    current: Boolean,
    whiteStatus: Status,
    blackStatus: Status,
    applyBy: Option[User.ID]
  ) = ApptRecord(
    id = time.toString("yyyyMMddHHmm"),
    time = time,
    message = message,
    current = current,
    whiteStatus = whiteStatus,
    blackStatus = blackStatus,
    applyBy = applyBy
  )

  sealed abstract class Status(val id: String, val name: String)
  object Status {
    case object Created extends Status("created", "新建")
    case object Confirmed extends Status("confirmed", "已确认")

    val all = List(Created, Confirmed)
    val byId = all map { v => (v.id, v) } toMap
    def apply(id: String): Status = byId get id err s"Bad Status $id"
  }

}