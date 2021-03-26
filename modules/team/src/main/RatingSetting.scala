package lila.team

case class RatingSetting(
    open: Boolean,
    defaultRating: Int,
    k: Int,
    coachSupport: Boolean,
    turns: Int,
    minutes: Int
)

object RatingSetting {

  val min = EloRating.min
  val max = EloRating.max
  val defaultRating = EloRating.defaultRating

  def default = new RatingSetting(
    open = false,
    defaultRating = defaultRating,
    k = 20,
    coachSupport = false,
    turns = 10,
    minutes = 20
  )

}