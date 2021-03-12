package lila.coach

import org.joda.time.{ DateTime, Days }

case class Coach(
    _id: Coach.Id, // user ID
    user: Coach.User,
    certify: Certify,
    available: Coach.Available,
    profile: CoachProfile,
    createdAt: DateTime,
    updatedAt: DateTime
) {

  def id = _id

  def is(user: lila.user.User) = id.value == user.id

  def certified = certify.approved

  def daysOld: Option[Int] = certify.approvedAt.map { d =>
    Days.daysBetween(d, DateTime.now).getDays
  }
}

object Coach {

  def make(user: lila.user.User, certify: Certify) = Coach(
    _id = Id(user.id),
    user = User(user.perfs.bestStandardRating, user.seenAt | user.createdAt),
    certify = certify,
    available = Available(true),
    profile = CoachProfile(),
    createdAt = DateTime.now,
    updatedAt = DateTime.now
  )

  case class WithUser(coach: Coach, user: lila.user.User) {
    def certify = coach.certify
  }

  case class Id(value: String) extends AnyVal with StringValue
  case class Available(value: Boolean) extends AnyVal
  case class User(rating: Int, seenAt: DateTime)

}
