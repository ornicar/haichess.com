package lila.notify

import lila.common.LightUser
import play.api.libs.json._

final class JSONHandlers(getLightUser: LightUser.GetterSync) {

  implicit val privateMessageThreadWrites = Json.writes[PrivateMessage.Thread]

  implicit val notificationWrites: Writes[Notification] = new Writes[Notification] {

    private def writeBody(notificationContent: NotificationContent) = {
      notificationContent match {
        case MentionedInThread(mentionedBy, topic, _, category, postId) => Json.obj(
          "mentionedBy" -> getLightUser(mentionedBy.value),
          "topic" -> topic.value, "category" -> category.value,
          "postId" -> postId.value
        )
        case InvitedToStudy(invitedBy, studyName, studyId) => Json.obj(
          "invitedBy" -> getLightUser(invitedBy.value),
          "studyName" -> studyName.value,
          "studyId" -> studyId.value
        )
        case PrivateMessage(senderId, thread, text) => Json.obj(
          "sender" -> getLightUser(senderId.value),
          "thread" -> privateMessageThreadWrites.writes(thread),
          "text" -> text.value
        )
        case TeamJoined(id, name) => Json.obj(
          "id" -> id.value,
          "name" -> name.value
        )
        case TeamMadeOwner(id, name) => Json.obj(
          "id" -> id.value,
          "name" -> name.value
        )
        case LimitedTournamentInvitation | ReportedBanned | CoachReview => Json.obj()
        case TitledTournamentInvitation(id, text) => Json.obj(
          "id" -> id,
          "text" -> text
        )
        case GameEnd(gameId, opponentId, win) => Json.obj(
          "id" -> gameId.value,
          "opponent" -> opponentId.map(_.value).flatMap(getLightUser),
          "win" -> win.map(_.value)
        )
        case _: PlanStart => Json.obj()
        case _: PlanExpire => Json.obj()
        case RatingRefund(perf, points) => Json.obj(
          "perf" -> perf,
          "points" -> points
        )
        case CorresAlarm(gameId, opponent) => Json.obj(
          "id" -> gameId,
          "op" -> opponent
        )
        case IrwinDone(userId) => Json.obj(
          "user" -> getLightUser(userId)
        )
        case CoachApply(userId) => Json.obj(
          "user" -> getLightUser(userId)
        )
        case CoachApproved(userId, status) => Json.obj(
          "user" -> getLightUser(userId),
          "status" -> status
        )
        case TeamApply(userId, teamId) => Json.obj(
          "user" -> getLightUser(userId),
          "teamId" -> teamId
        )
        case TeamApproved(teamId, status) => Json.obj(
          "teamId" -> teamId,
          "status" -> status
        )
        case GenericLink(url, title, text, icon) => Json.obj(
          "url" -> url,
          "title" -> title,
          "text" -> text,
          "icon" -> icon
        )
        case InvitedToClazz(invitedBy, clazzName, clazzId) => Json.obj(
          "invitedBy" -> getLightUser(invitedBy.value),
          "clazzName" -> clazzName.value,
          "clazzId" -> clazzId.value
        )
        case InvitedToTeam(invitedBy, teamName, teamId) => Json.obj(
          "invitedBy" -> getLightUser(invitedBy.value),
          "teamName" -> teamName.value,
          "teamId" -> teamId.value
        )
      }
    }

    def writes(notification: Notification) = Json.obj(
      "content" -> writeBody(notification.content),
      "type" -> notification.content.key,
      "read" -> notification.read.value,
      "date" -> notification.createdAt
    )
  }

  import lila.common.paginator.PaginatorJson._
  implicit val unreadWrites = Writes[Notification.UnreadCount] { v => JsNumber(v.value) }
  implicit val andUnreadWrites: Writes[Notification.AndUnread] = Json.writes[Notification.AndUnread]

  implicit val newNotificationWrites: Writes[NewNotification] = new Writes[NewNotification] {

    def writes(newNotification: NewNotification) = Json.obj(
      "notification" -> newNotification.notification,
      "unread" -> newNotification.unreadNotifications
    )
  }
}

