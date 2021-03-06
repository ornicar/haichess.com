package lila.team

import lila.common.paginator.Paginator
import reactivemongo.bson._
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.{ User, UserRepo }
import reactivemongo.api.ReadPreference
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ GroupField, Match, Project, SumValue }

object MemberRepo {

  // dirty
  private val coll = Env.current.colls.member

  import BSONHandlers._

  type ID = String

  private type NbMembers = Int

  def byId(teamId: ID, userId: ID) = {
    coll.byId[Member](Member.makeId(teamId, userId))
  }

  def memberFromSecondary(teamId: Seq[String], userId: String): Fu[List[Member]] =
    coll.byOrderedIds[Member, String](
      teamId.map(Member.makeId(_, userId)),
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def memberFromSecondary(teamId: String, userIds: Seq[String]): Fu[List[Member]] =
    coll.byOrderedIds[Member, String](
      userIds.map(Member.makeId(teamId, _)),
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def memberOptionFromSecondary(teamId: String, userIds: Seq[String]): Fu[List[Option[Member]]] =
    coll.optionsByOrderedIds[Member, String](
      userIds.map(Member.makeId(teamId, _)),
      readPreference = ReadPreference.secondaryPreferred
    )(_.id)

  def memberByTeam(teamId: String): Fu[List[Member]] =
    coll.list[Member](teamQuery(teamId))

  def userIdsByTeam(teamId: ID): Fu[Set[ID]] =
    coll.distinct[String, Set]("user", $doc("team" -> teamId).some)

  def teamIdsByUser(userId: ID): Fu[Set[ID]] =
    coll.distinct[String, Set]("team", $doc("user" -> userId).some)

  def ownerTeamIdsByUser(userId: ID): Fu[Set[ID]] =
    coll.distinct[String, Set]("team", $doc("role" -> "owner", "user" -> userId).some)

  def teamOwner(teamId: ID): Fu[Option[Member]] =
    coll.uno[Member]($doc("role" -> "owner", "team" -> teamId))

  def teamOwners(teamIds: Set[ID]): Fu[Set[ID]] =
    coll.distinct[String, Set]("user", $doc("role" -> "owner", "team" $in teamIds).some)

  def teamMembers(teamIds: Set[ID]): Fu[Set[ID]] =
    coll.distinct[String, Set]("user", $doc("role" -> "trainee", "team" $in teamIds).some)

  def teamIdsByMember(userId: ID): Fu[Set[ID]] =
    coll.distinct[String, Set]("team", $doc("user" -> userId, "role" -> "trainee").some)

  def removeByteam(teamId: ID): Funit =
    coll.remove(teamQuery(teamId)).void

  def removeByUser(userId: ID): Funit =
    coll.remove(userQuery(userId)).void

  def exists(teamId: ID, userId: ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def add(
    teamId: String,
    userId: String,
    role: Member.Role = Member.Role.Trainee,
    tags: Option[MemberTags] = None,
    mark: Option[String] = None,
    rating: Option[Int] = None,
    clazzIds: Option[List[String]] = None
  ): Funit =
    coll.insert(Member.make(team = teamId, user = userId, role = role, tags = tags, mark = mark, rating = rating, clazzIds = clazzIds)).void

  def remove(teamId: String, userId: String): Funit =
    coll.remove(selectId(teamId, userId)).void

  def countByTeam(teamId: String): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def byRole(teamId: ID, role: Member.Role): Fu[Set[ID]] =
    coll.distinct[String, Set]("user", (teamQuery(teamId) ++ roleQuery(role)).some)

  def setRole(teamId: ID, userId: ID, role: Member.Role): Funit = coll.update(
    selectId(teamId, userId),
    $set(
      "role" -> role.id
    )
  ).void

  def updateMember(member: Member) = coll.update($id(member.id), member)

  def initMembersRating(teamId: ID, defaultRating: Int): Funit =
    coll.update(
      teamQuery(teamId) ++ $doc("rating" $exists false),
      $set(
        "rating" -> EloRating(defaultRating, 0)
      ),
      multi = true
    ).void

  def removeTag(teamId: ID, field: String) = coll.update(
    teamQuery(teamId),
    $unset(s"tags.$field")
  )

  def memberWithUser(id: ID): Fu[Option[MemberWithUser]] = for {
    memberOption <- coll.byId[Member](id)
    userOption <- memberOption.??(m => UserRepo.byId(m.user))
  } yield {
    memberOption |@| userOption apply {
      case (member, user) => MemberWithUser(member, user)
    }
  }

  def addClazz(teamId: String, userId: ID, clazzId: String): Funit =
    coll.update(
      selectId(teamId, userId),
      $addToSet("clazzIds" -> clazzId)
    ).void

  def removeClazz(teamId: String, userId: ID, clazzId: String): Funit =
    coll.update(
      selectId(teamId, userId),
      $pull("clazzIds" -> clazzId)
    ).void

  def ratingPage(teamId: String, page: Int, q: Option[String], clazzId: Option[String]): Fu[Paginator[MemberWithUser]] = {
    var doc = $doc("team" -> teamId)
    clazzId.foreach(c =>
      doc = doc ++ $doc("clazzIds" -> c))
    q.foreach(q =>
      doc = doc ++ $or($doc("user" $regex (q, "i")), $doc("mark" $regex (q, "i"))))

    val adapter = new Adapter[Member](
      collection = coll,
      selector = doc,
      projection = $empty,
      sort = $doc("rating.r" -> -1)
    ) mapFutureList withUsers
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = lila.common.MaxPerPage(30)
    )
  }

  private def withUsers(members: Seq[Member]): Fu[Seq[MemberWithUser]] =
    UserRepo.withColl {
      _.byOrderedIds[User, User.ID](members.map(_.user))(_.id)
    } map { users =>
      members zip users collect {
        case (coach, user) => MemberWithUser(coach, user)
      }
    }

  def ratingDistribution(teamId: String, clazzId: Option[String]) = {
    coll.aggregateList(
      Match($doc("team" -> teamId) ++ clazzId.??(c => $doc("clazzIds" -> c))),
      List(
        Project($doc(
          "_id" -> false,
          "r" -> $doc(
            "$subtract" -> $arr("$rating.r", $doc("$mod" -> $arr("$rating.r", EloRating.group)))
          )
        )),
        GroupField("r")("nb" -> SumValue(1))
      ),
      maxDocs = Int.MaxValue,
      ReadPreference.secondaryPreferred
    ).map { res =>
        val hash: Map[Int, NbMembers] = res.flatMap { obj =>
          for {
            rating <- obj.getAs[Double]("_id").map(_.toInt)
            nb <- obj.getAs[NbMembers]("nb")
          } yield rating -> nb
        }(scala.collection.breakOut)
        (EloRating.min to EloRating.max by EloRating.group).map { r =>
          hash.getOrElse(r, 0)
        }.toList
      }
  }

  def selectId(teamId: ID, userId: ID) = $id(Member.makeId(teamId, userId))
  def teamQuery(teamId: ID) = $doc("team" -> teamId)
  def userQuery(userId: ID) = $doc("user" -> userId)
  def roleQuery(role: Member.Role) = $doc("role" -> role.id)
}
