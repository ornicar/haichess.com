package lila.appt

import lila.game.{ Game, PerfPicker }
import lila.user.User
import org.joda.time.DateTime
import chess.{ Color, Speed }
import chess.Clock.{ Config => ClockConfig }
import chess.variant.Variant
import lila.rating.PerfType

case class Appt(
    id: Game.ID,
    position: String,
    variant: Variant,
    rated: Boolean,
    clock: Option[ClockConfig],
    daysPerTurn: Option[Int],
    minDateTime: DateTime,
    maxDateTime: DateTime,
    whitePlayerUid: User.ID,
    blackPlayerUid: User.ID,
    records: List[ApptRecord],
    contest: Option[ApptContest],
    confirmed: Int,
    finalTime: Option[DateTime],
    canceled: Boolean,
    createBy: Option[User.ID],
    createAt: DateTime,
    updateAt: DateTime
) {

  def isConfirmed = confirmed == 1

  def isContest = contest.isDefined

  def isChallenge = source == "challenge"

  def source =
    if (isContest) "contest"
    else "challenge"

  def currentRecord: ApptRecord = records.find(_.current) err "can not find current"

  def currentTime: DateTime = currentRecord.time

  def currentConfirmed: Boolean = currentRecord.allConfirmed

  def time = finalTime err "can not load final time"

  def speed = Speed(clock)

  def perfType: Option[PerfType] = PerfPicker.perfType(speed, variant, none)

  def showClock = {
    clock.map { c =>
      c.show
    } getOrElse {
      daysPerTurn.map { days =>
        s"${days}天"
      }.getOrElse {
        "∞"
      }
    }
  }

  def userColor(user: User.ID): Option[Color] =
    if (whitePlayerUid == user)
      Color.White.some
    else if (blackPlayerUid == user)
      Color.Black.some
    else None

  def opponent(user: User.ID): Option[User.ID] =
    if (whitePlayerUid == user)
      blackPlayerUid.some
    else if (blackPlayerUid == user)
      whitePlayerUid.some
    else None

  def contains(userId: User.ID): Boolean =
    whitePlayerUid == userId || blackPlayerUid == userId

  def withSetRecord(time: DateTime, user: User.ID): Appt =
    copy(
      records = records.map(r =>
        r.copy(
          current = false
        )) :+ ApptRecord.make(
        time = time,
        message = "组织者设置".some,
        current = true,
        whiteStatus = ApptRecord.Status.Confirmed,
        blackStatus = ApptRecord.Status.Confirmed,
        applyBy = user.some
      ),
      confirmed = 1,
      finalTime = time.some,
      updateAt = DateTime.now
    )

  def withAddRecord(color: Color, time: DateTime, message: Option[String], user: User.ID): Appt =
    copy(
      records = records.map(r =>
        r.copy(
          current = false
        )) :+ ApptRecord.make(
        time = time,
        message = message,
        current = true,
        whiteStatus = if (color.white) ApptRecord.Status.Confirmed else ApptRecord.Status.Created,
        blackStatus = if (color.black) ApptRecord.Status.Confirmed else ApptRecord.Status.Created,
        applyBy = user.some
      ),
      updateAt = DateTime.now
    )

  def withRecordConfirmed(color: Color): Appt =
    copy(
      records = records.map { record =>
        if (record.current) record.withConfirmed(color) else record
      },
      updateAt = DateTime.now
    ) |> { appt =>
        if (appt.currentConfirmed) {
          appt.copy(
            confirmed = 1,
            finalTime = currentTime.some
          )
        } else appt
      }

}

object Appt {

  type ID = String

  def make(
    gameId: Game.ID,
    position: String,
    variant: Variant,
    rated: Boolean,
    clock: Option[ClockConfig],
    daysPerTurn: Option[Int],
    minDateTime: DateTime,
    maxDateTime: DateTime,
    whitePlayerUid: User.ID,
    blackPlayerUid: User.ID,
    records: List[ApptRecord],
    contest: Option[ApptContest],
    createBy: Option[User.ID]
  ) = Appt(
    id = gameId,
    position = position,
    variant = variant,
    rated = rated,
    clock = clock,
    daysPerTurn = daysPerTurn,
    minDateTime = minDateTime,
    maxDateTime = maxDateTime,
    whitePlayerUid = whitePlayerUid,
    blackPlayerUid = blackPlayerUid,
    records = records,
    contest = contest,
    confirmed = 0,
    finalTime = None,
    canceled = false,
    createBy = createBy,
    createAt = DateTime.now,
    updateAt = DateTime.now
  )

}
