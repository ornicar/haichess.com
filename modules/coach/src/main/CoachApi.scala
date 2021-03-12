package lila.coach

import lila.db.Photographer
import lila.db.dsl._
import org.joda.time.DateTime
import lila.security.Granter
import lila.user.{ User, UserRepo }

final class CoachApi(coachColl: Coll, photographer: Photographer) {

  import BsonHandlers._

  def byId(id: Coach.Id): Fu[Option[Coach]] = coachColl.byId[Coach](id.value)

  def byId2(id: Coach.Id): Fu[Option[Coach.WithUser]] =
    UserRepo.byId(id.value) flatMap {
      _ ?? { user =>
        coachColl.byId[Coach](id.value) map2 withUser(user)
      }
    }

  def find(username: String): Fu[Option[Coach.WithUser]] =
    UserRepo named username flatMap { _ ?? find }

  def find(user: User): Fu[Option[Coach.WithUser]] = Granter(_.Coach)(user) ?? {
    byId(Coach.Id(user.id)) map2 withUser(user)
  }

  def findNoGranter(user: User): Fu[Option[Coach.WithUser]] = {
    byId(Coach.Id(user.id)) map2 withUser(user)
  }

  def update(c: Coach.WithUser, data: CoachProfileForm.Data): Funit =
    coachColl.update(
      $id(c.coach.id),
      data(c.coach),
      upsert = true
    ).void

  def remove(userId: User.ID): Funit =
    coachColl.updateField($id(userId), "available", false).void

  def uploadPicture(id: String, picture: Photographer.Uploaded) = {
    photographer(id, picture, false)
  }

  def setSeenAt(user: User): Funit =
    Granter(_.Coach)(user).?? {
      coachColl.update(
        $id(user.id),
        $set("user.seenAt" -> DateTime.now)
      ).void
    }

  def setRating(userPre: User): Funit =
    Granter(_.Coach)(userPre) ?? {
      UserRepo.byId(userPre.id) flatMap {
        _ ?? { user =>
          coachColl.update($id(user.id), $set("user.rating" -> user.perfs.bestStandardRating)).void
        }
      }
    }

  private def withUser(user: User)(coach: Coach) = Coach.WithUser(coach, user)

}
