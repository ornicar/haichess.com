package lila.setup

import chess.format.FEN
import chess.variant.Variant
import lila.common.Form.{ futureDateTime, numberIn }
import lila.lobby.Color
import lila.user.UserContext
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

private[setup] final class FormFactory(
    anonConfigRepo: AnonConfigRepo,
    userConfigRepo: UserConfigRepo
) {

  import Mappings._

  def filterFilled(implicit ctx: UserContext): Fu[(Form[FilterConfig], FilterConfig)] =
    filterConfig map { f => filter(ctx).fill(f) -> f }

  def filter(ctx: UserContext) = Form(
    mapping(
      "variant" -> list(variantWithVariants),
      "mode" -> list(rawMode(withRated = true)),
      "speed" -> list(speed),
      "ratingRange" -> ratingRange
    )(FilterConfig.<<)(_.>>)
  )

  def filterConfig(implicit ctx: UserContext): Fu[FilterConfig] = savedConfig map (_.filter)

  def aiFilled(fen: Option[FEN])(implicit ctx: UserContext): Fu[Form[AiConfig]] =
    aiConfig map { config =>
      ai(ctx) fill fen.fold(config) { f =>
        config.copy(fen = f.some, variant = chess.variant.FromPosition)
      }
    }

  def ai(ctx: UserContext) = Form(
    mapping(
      "variant" -> aiVariants,
      "timeMode" -> timeMode,
      "time" -> time,
      "increment" -> increment,
      "days" -> days,
      "level" -> level,
      "color" -> color,
      "fen" -> fen
    )(AiConfig.<<)(_.>>)
      .verifying("invalidFen", _.validFen)
      .verifying("Can't play that time control from a position", _.timeControlFromPosition)
  )

  def aiConfig(implicit ctx: UserContext): Fu[AiConfig] = fuccess(AiConfig.default) // savedConfig map (_.ai)

  def friendFilled(fen: Option[FEN], limit: Option[Int], increment: Option[Int])(implicit ctx: UserContext): Fu[Form[FriendConfig]] =
    friendConfig map { config =>
      friend(ctx) fill fen.fold(config) { f =>
        config.copy(
          fen = f.some,
          variant = chess.variant.FromPosition
        ) |> { fc =>
          fc.copy(
            time = limit.map(_ / 60d) | fc.time,
            increment = increment | fc.increment
          )
        }
      }
    }

  def friend(ctx: UserContext) = Form(
    mapping(
      "variant" -> variantWithFenAndVariants,
      "timeMode" -> timeMode,
      "time" -> time,
      "increment" -> increment,
      "days" -> days,
      "mode" -> mode(withRated = ctx.isAuth),
      "color" -> color,
      "fen" -> fen,
      "appt" -> numberIn(booleanChoices),
      "apptStartsAt" -> optional(futureDateTime.verifying("开始时间在两周内", (DateTime.now plusWeeks 2).isAfter(_))),
      "apptMessage" -> optional(nonEmptyText(minLength = 2, maxLength = 100))
    )(FriendConfig.<<)(_.>>)
      .verifying("Invalid clock", _.validClock)
      .verifying("invalidFen", _.validFen)
  )

  val booleanChoices = Seq((0 -> "否"), (1 -> "是"))

  def friendConfig(implicit ctx: UserContext): Fu[FriendConfig] = fuccess(FriendConfig.default) //savedConfig map (_.friend)

  def hookFilled(timeModeString: Option[String])(implicit ctx: UserContext): Fu[Form[HookConfig]] =
    hookConfig map (_ withTimeModeString timeModeString) map hook(ctx).fill

  def hook(ctx: UserContext) = Form(
    mapping(
      "variant" -> variantWithVariants,
      "timeMode" -> timeMode,
      "time" -> time,
      "increment" -> increment,
      "days" -> days,
      "mode" -> mode(ctx.isAuth),
      "ratingRange" -> optional(ratingRange),
      "color" -> color
    )(HookConfig.<<)(_.>>)
      .verifying("Invalid clock", _.validClock)
      .verifying("Can't create rated unlimited in lobby", _.noRatedUnlimited)
  )

  def hookConfig(implicit ctx: UserContext): Fu[HookConfig] = savedConfig map (_.hook)

  lazy val api = Form(
    mapping(
      "variant" -> optional(text.verifying(Variant.byKey.contains _)),
      "clock" -> optional(mapping(
        "limit" -> number.verifying(ApiConfig.clockLimitSeconds.contains _),
        "increment" -> increment
      )(chess.Clock.Config.apply)(chess.Clock.Config.unapply)),
      "days" -> optional(days),
      "rated" -> boolean,
      "color" -> optional(color),
      "fen" -> fen
    )(ApiConfig.<<)(_.>>).verifying("invalidFen", _.validFen)
  )

  def savedConfig(implicit ctx: UserContext): Fu[UserConfig] =
    ctx.me.fold(anonConfigRepo config ctx.req)(userConfigRepo.config)
}
