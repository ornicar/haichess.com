package lila.setup

import chess.Mode
import chess.format.FEN
import lila.lobby.Color
import lila.rating.PerfType
import lila.game.PerfPicker
import org.joda.time.DateTime

case class FriendConfig(
    variant: chess.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Int,
    days: Int,
    mode: Mode,
    color: Color,
    fen: Option[FEN] = None,
    appt: Boolean = false,
    apptStartsAt: Option[DateTime] = None,
    apptMessage: Option[String] = None
) extends HumanConfig with Positional {

  val strictFen = false

  def >> = (variant.id, timeMode.id, time, increment, days, mode.id.some, color.name, fen.map(_.value), if (appt) 1 else 0, apptStartsAt, apptMessage).some

  def isPersistent = timeMode == TimeMode.Unlimited || timeMode == TimeMode.Correspondence

  def perfType: Option[PerfType] = PerfPicker.perfType(chess.Speed(makeClock), variant, makeDaysPerTurn)
}

object FriendConfig extends BaseHumanConfig {

  def <<(v: Int, tm: Int, t: Double, i: Int, d: Int, m: Option[Int], c: String,
    fen: Option[String], appt: Int, apptStartsAt: Option[DateTime], apptMessage: Option[String]) =
    new FriendConfig(
      variant = chess.variant.Variant(v) err "Invalid game variant " + v,
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      days = d,
      mode = m.fold(Mode.default)(Mode.orDefault),
      color = Color(c) err "Invalid color " + c,
      fen = fen map FEN,
      appt = (appt == 1),
      apptStartsAt = apptStartsAt,
      apptMessage = apptMessage
    )

  val default = FriendConfig(
    variant = variantDefault,
    timeMode = TimeMode.RealTime,
    time = 10d,
    increment = 0,
    days = 2,
    mode = Mode.default,
    color = Color.default
  )

  import lila.db.BSON
  import lila.db.dsl._
  import lila.game.BSONHandlers.FENBSONHandler

  private[setup] implicit val friendConfigBSONHandler = new BSON[FriendConfig] {

    override val logMalformed = false

    def reads(r: BSON.Reader): FriendConfig = FriendConfig(
      variant = chess.variant.Variant orDefault (r int "v"),
      timeMode = TimeMode orDefault (r int "tm"),
      time = r double "t",
      increment = r int "i",
      days = r int "d",
      mode = Mode orDefault (r int "m"),
      color = Color.White,
      fen = r.getO[FEN]("f") filter (_.value.nonEmpty),
      appt = r boolD "ap",
      apptStartsAt = r dateO "aps",
      apptMessage = r strO "apm"
    )

    def writes(w: BSON.Writer, o: FriendConfig) = $doc(
      "v" -> o.variant.id,
      "tm" -> o.timeMode.id,
      "t" -> o.time,
      "i" -> o.increment,
      "d" -> o.days,
      "m" -> o.mode.id,
      "f" -> o.fen,
      "ap" -> o.appt,
      "aps" -> o.apptStartsAt,
      "apm" -> o.apptMessage
    )
  }
}
