package lila.team

case class RatingSetting(
    open: Boolean,
    defaultRating: Int,
    coachSupport: Boolean,
    turns: Int,
    minutes: Int
)

object RatingSetting {

  def default = new RatingSetting(
    open = false,
    defaultRating = 1500,
    coachSupport = false,
    turns = 10,
    minutes = 10
  )

}