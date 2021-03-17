package lila.team

/**
 * Approach:
 * P1: Probability of winning of player with rating2
 * P2: Probability of winning of player with rating1.
 *
 * P1 = (1.0 / (1.0 + pow(10, ((rating1 – rating2) / 400))));
 * P2 = (1.0 / (1.0 + pow(10, ((rating2 – rating1) / 400))));
 * Obviously, P1 + P2 = 1.
 *
 * The rating of player is updated using the formula given below :-
 *
 * rating1 = rating1 + K*(Actual Score – Expected score);
 *
 * https://blog.csdn.net/destruction666/article/details/7597348
 * https://www.geeksforgeeks.org/elo-rating-algorithm
 */
case class EloRating(rating: Double, games: Int) {

  def intValue = rating.intValue

  // reference FIDE rule
  def k: Int = {
    if (games <= 30) 25
    else {
      if (rating < 2400) 15
      else 10
    }
  }

  // Probability
  def p(opponentRating: Double): Double = {
    1.0 / (1.0 + math.pow(10, (opponentRating - rating) / 400))
  }

  // Actual Score
  def ac(win: Option[Boolean]): Double = win.fold(0.5) { w =>
    if (w) 1.0 else 0.0
  }

  def calc(opponentRating: Double, win: Option[Boolean]): EloRating = {
    val r = rating + k * (ac(win) - p(opponentRating))
    EloRating(math.max(r, EloRating.min), games + 1)
  }

}

object EloRating {

  val min = 600
  val max = 2800
  val defaultRating = 1500

  def default = EloRating(defaultRating, 0)

}
