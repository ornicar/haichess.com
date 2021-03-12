package lila.app
package mashup

import scala.concurrent.duration._
import lila.forum.MiniForumPost
import lila.team.{ Invite, InviteRepo, InviteWithUser, MemberRepo, RequestRepo, RequestWithUser, Team, TeamApi }
import lila.tournament.{ Tournament, TournamentRepo }
import lila.contest.{ Contest, ContestRepo }
import lila.user.{ User, UserRepo }

case class TeamInfo(
    mine: Boolean,
    createdByMe: Boolean,
    requestedByMe: Boolean,
    invite: Option[Invite],
    requests: List[RequestWithUser],
    invites: List[InviteWithUser],
    bestUserIds: List[User.ID],
    coachIds: List[User.ID],
    toints: Int,
    forumNbPosts: Int,
    forumPosts: List[MiniForumPost],
    tours: List[TeamInfo.AnyTour]
) {

  import TeamInfo._

  def hasRequests = requests.nonEmpty

  def userIds = bestUserIds ::: forumPosts.flatMap(_.userId)

  lazy val featuredTours: List[AnyTour] = {
    val (enterable, finished) = tours.partition(_.isEnterable) match {
      case (e, f) => e.sortBy(_.startsAt).take(5) -> f.sortBy(-_.startsAt.getSeconds).take(5)
    }
    enterable ::: finished.take(5 - enterable.size)
  }
}

object TeamInfo {
  def anyTour(tour: Tournament) = AnyTour(Left(tour))
  def anyTour(contest: Contest) = AnyTour(Right(contest))

  case class AnyTour(any: Either[Tournament, Contest]) extends AnyVal {
    def isEnterable = any.fold(_.isEnterable, _.isEnterable)
    def startsAt = any.fold(_.startsAt, _.startsAt)
    def isNowOrSoon = any.fold(_.isNowOrSoon, _.isNowOrSoon)
    def nbPlayers = any.fold(_.nbPlayers, _.nbPlayers)
  }
}

final class TeamInfoApi(
    api: TeamApi,
    getForumNbPosts: String => Fu[Int],
    getForumPosts: String => Fu[List[MiniForumPost]],
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import TeamInfo._

  private case class Cachable(bestUserIds: List[User.ID], toints: Int)

  private def fetchCachable(id: String): Fu[Cachable] = for {
    userIds ← (MemberRepo userIdsByTeam id)
    bestUserIds ← UserRepo.ratedIdsByIdsSortRating(userIds, 10)
    toints ← UserRepo.idsSumToints(userIds)
  } yield Cachable(bestUserIds, toints)

  private val cache = asyncCache.multi[String, Cachable](
    name = "teamInfo",
    f = fetchCachable,
    expireAfter = _.ExpireAfterWrite(10 minutes)
  )

  /*  def tournaments(team: Team, nb: Int): Fu[List[AnyTour]] =
    for {
      tours <- TournamentRepo.visibleInTeam(team.id, nb)
      contests <- ContestRepo.visibleInTeam(team.id, nb)
    } yield {
      tours.map(anyTour) ::: contests.map(anyTour)
    }.sortBy(-_.startsAt.getSeconds)*/

  def apply(team: Team, me: Option[User]): Fu[TeamInfo] = for {
    requests ← (team.enabled && me.??(m => team.isCreator(m.id))) ?? api.requestsWithUsers(team)
    invites ← (team.enabled && me.??(m => team.isCreator(m.id))) ?? api.invitesWithUsers(team)
    mine <- me.??(m => api.belongsTo(team.id, m.id))
    requestedByMe ← !mine ?? me.??(m => RequestRepo.exists(team.id, m.id))
    invite <- me.??(m => InviteRepo.find(team.id, m.id))
    cachable <- cache get team.id
    coachIds <- MemberRepo.byRole(team.id, lila.team.Member.Role.Coach)
    forumNbPosts ← getForumNbPosts(team.id)
    forumPosts ← getForumPosts(team.id)
    tours <- TournamentRepo.visibleInTeam(team.id, 10)
    contests <- ContestRepo.visibleInTeam(team.id, 10)
  } yield TeamInfo(
    mine = mine,
    createdByMe = ~me.map(m => team.isCreator(m.id)),
    requestedByMe = requestedByMe,
    invite = invite,
    requests = requests,
    invites = invites,
    bestUserIds = cachable.bestUserIds,
    coachIds = coachIds.toList,
    toints = cachable.toints,
    forumNbPosts = forumNbPosts,
    forumPosts = forumPosts,
    tours = tours.map(anyTour) ::: contests.map(anyTour)
  )
}
