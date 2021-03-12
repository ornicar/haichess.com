package lila.puzzle

import play.api.libs.json._
import lila.common.LightUser

final class UserJsonView(
    isOnline: lila.user.User.ID => Boolean,
    lightUser: LightUser.GetterSync
) {

  def user(userId: String) =
    lightUser(userId).map { l =>
      LightUser.lightUserWrites.writes(l) ++ Json.obj(
        "online" -> isOnline(userId),
        "username" -> l.name // for mobile app BC
      )
    }
}
