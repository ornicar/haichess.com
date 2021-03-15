package lila.team

case class RatingSetting(
    open: Boolean,
    defaultRating: Int,
    coachSupport: Boolean,
    turns: Int,
    minutes: Int
)

object RatingSetting {

  val min = 600
  val max = 2800
  val defaultRating = 1500

  def default = new RatingSetting(
    open = false,
    defaultRating = defaultRating,
    coachSupport = false,
    turns = 10,
    minutes = 10
  )

}