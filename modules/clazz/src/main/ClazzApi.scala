package lila.clazz

import lila.db.dsl._
import lila.team.TeamApi
import lila.user.{ User, UserRepo }
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument

final class ClazzApi(coll: Coll, courseApi: CourseApi, teamApi: TeamApi) {

  import BSONHandlers.clazzHandler

  def undeleted = $or(
    "deleted" $exists false, $doc("deleted" -> false)
  )

  def current(user: User): Fu[List[Clazz.ClazzWithTeam]] =
    list(
      user.id,
      $or(
        $doc("weekClazz.dateEnd" -> $gte(DateTime.now()), "stopped" -> false),
        $doc("trainClazz.dateEnd" -> $gte(DateTime.now()), "stopped" -> false)
      )
    )

  def history(user: User): Fu[List[Clazz.ClazzWithTeam]] =
    list(
      user.id,
      $or(
        $doc("weekClazz.dateEnd" -> $lt(DateTime.now())),
        $doc("trainClazz.dateEnd" -> $lt(DateTime.now())),
        $doc("stopped" -> true)
      )
    )

  def mine(userId: User.ID): Fu[List[Clazz]] =
    coll.list[Clazz](
      $doc(
        $or(
          $doc("coach" -> userId),
          $doc(s"students.$userId" $exists true, s"students.$userId.status" -> Student.InviteStatus.Joined.id)
        ),
        $or(
          $doc("weekClazz.dateEnd" -> $gte(DateTime.now()), "stopped" -> false),
          $doc("trainClazz.dateEnd" -> $gte(DateTime.now()), "stopped" -> false)
        ),
        "stopped" -> false
      ) ++ undeleted
    )

  def list(userId: User.ID, dateCondition: BSONDocument): Fu[List[Clazz.ClazzWithTeam]] =
    coll.find(
      $or(
        $doc("coach" -> userId),
        $doc(s"students.$userId" $exists true, s"students.$userId.status" -> Student.InviteStatus.Joined.id)
      ) ++ undeleted ++ dateCondition
    ).sort($sort desc "createdAt").list[Clazz]() flatMap { clazzs =>
        teamApi.teamOptionFromSecondary(clazzs.map(_.teamOrDefault)) map { teams =>
          clazzs zip teams collect {
            case (clazz, teamOption) => Clazz.ClazzWithTeam(clazz, teamOption)
          }
        }
      }

  def byIdWithCoach(id: Clazz.ID): Fu[Option[Clazz.ClazzWithCoach]] = coll.byId[Clazz](id) flatMap {
    _.?? { clazz =>
      UserRepo.byId(clazz.coach) map {
        _.?? { Clazz.ClazzWithCoach(clazz, _) }.some
      }
    }
  }

  def byId(id: Clazz.ID): Fu[Option[Clazz]] = coll.byId[Clazz](id)

  def byIds(ids: List[Clazz.ID]): Fu[List[Clazz]] = coll.byIds[Clazz](ids)

  def create(clazz: Clazz): Funit =
    coll.insert(clazz).void >> {
      val courseList = clazz.toCourse(clazz._id, clazz.coach)
      courseApi.bulkInsert(courseList)
    } >> clazz.team.?? { teamId =>
      teamApi.addClazz(teamId, clazz._id)
    }

  def nameExists(user: User, name: String, id: Option[Clazz.ID]): Fu[Boolean] =
    coll exists $doc("name" -> name, "coach" -> user.id) ++ id.fold($doc()) { _id =>
      $doc("_id" $ne _id)
    }

  def byCourseList(courseList: List[Course]): Fu[List[Clazz]] =
    coll.byIds[Clazz](courseList.map(_.clazz))

  def updateWeekCourseLastDate(id: Clazz.ID, lastCourseTime: DateTime): Funit =
    coll.update(
      $id(id),
      $set("weekClazz.dateEnd" -> lastCourseTime, "updatedAt" -> DateTime.now)
    ).void

  def update(updater: Clazz): Funit =
    coll.update(
      $id(updater._id),
      updater
    ).void

  def stop(id: String): Funit = coll.update(
    $id(id),
    $set("stopped" -> true)
  ).void

  def delete(clazz: Clazz): Funit = coll.update(
    $id(clazz.id),
    $set("deleted" -> true)
  ) >> courseApi.deleteByClazz(clazz.id) >> clazz.team.?? { teamId =>
      teamApi.removeClazz(teamId, clazz.id)
    }

  def clazzUser(userId: User.ID): Fu[ClazzUser] = coll.list[Clazz](
    $or(
      $doc("coach" -> userId),
      $doc(s"students.$userId" $exists true, s"students.$userId.status" -> Student.InviteStatus.Joined.id)
    )
  ) map { list =>
      ClazzUser(
        classmate = list.foldLeft(Set.empty[User.ID]) {
          case (set, clazz) => set ++ (clazz.students | Students.empty).students.keySet
        },
        coach = list.foldLeft(Set.empty[User.ID]) {
          case (set, clazz) => set ++ Set(clazz.coach)
        }
      )
    }

  def myTeamClazz(userId: String, teamId: String): Fu[List[String]] =
    coll.distinct[String, List](
      "_id",
      ($doc(s"students.$userId" $exists true, s"students.$userId.status" -> Student.InviteStatus.Joined.id) ++ undeleted).some
    ).map(_.toList)

}
