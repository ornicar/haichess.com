package lila.notify

import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._
import lila.db.{ dsl, BSON }
import lila.notify.InvitedToStudy.{ StudyName, InvitedBy, StudyId }
import lila.notify.InvitedToClazz.{ ClazzName, InvitedBy => ClazzInvitedBy, ClazzId }
import lila.notify.InvitedToTeam.{ TeamName, InvitedBy => TeamInvitedBy, TeamId }
import lila.notify.MentionedInThread._
import lila.notify.Notification._
import reactivemongo.bson._

private object BSONHandlers {

  implicit val NotifiesHandler = stringAnyValHandler[Notifies](_.value, Notifies.apply)

  implicit val MentionByHandler = stringAnyValHandler[MentionedBy](_.value, MentionedBy.apply)
  implicit val TopicHandler = stringAnyValHandler[Topic](_.value, Topic.apply)
  implicit val TopicIdHandler = stringAnyValHandler[TopicId](_.value, TopicId.apply)
  implicit val CategoryHandler = stringAnyValHandler[Category](_.value, Category.apply)
  implicit val PostIdHandler = stringAnyValHandler[PostId](_.value, PostId.apply)

  implicit val InvitedToStudyByHandler = stringAnyValHandler[InvitedBy](_.value, InvitedBy.apply)
  implicit val StudyNameHandler = stringAnyValHandler[StudyName](_.value, StudyName.apply)
  implicit val StudyIdHandler = stringAnyValHandler[StudyId](_.value, StudyId.apply)
  implicit val ReadHandler = booleanAnyValHandler[NotificationRead](_.value, NotificationRead.apply)

  import PrivateMessage._
  implicit val PMThreadHandler = Macros.handler[Thread]
  implicit val PMSenderIdHandler = stringAnyValHandler[SenderId](_.value, SenderId.apply)
  implicit val PMTextHandler = stringAnyValHandler[Text](_.value, Text.apply)
  implicit val PrivateMessageHandler = Macros.handler[PrivateMessage]

  implicit val TeamIdHandler = stringAnyValHandler[TeamJoined.Id](_.value, TeamJoined.Id.apply)
  implicit val TeamNameHandler = stringAnyValHandler[TeamJoined.Name](_.value, TeamJoined.Name.apply)
  implicit val TeamJoinedHandler = Macros.handler[TeamJoined]

  implicit val TeamMadeOwnerIdHandler = stringAnyValHandler[TeamMadeOwner.Id](_.value, TeamMadeOwner.Id.apply)
  implicit val TeamMadeOwnerNameHandler = stringAnyValHandler[TeamMadeOwner.Name](_.value, TeamMadeOwner.Name.apply)
  implicit val TeamMadeOwnerHandler = Macros.handler[TeamMadeOwner]

  implicit val GameEndGameIdHandler = stringAnyValHandler[GameEnd.GameId](_.value, GameEnd.GameId.apply)
  implicit val GameEndOpponentHandler = stringAnyValHandler[GameEnd.OpponentId](_.value, GameEnd.OpponentId.apply)
  implicit val GameEndWinHandler = booleanAnyValHandler[GameEnd.Win](_.value, GameEnd.Win.apply)
  implicit val GameEndHandler = Macros.handler[GameEnd]

  implicit val TitledTournamentInvitationHandler = Macros.handler[TitledTournamentInvitation]

  implicit val PlanStartHandler = Macros.handler[PlanStart]
  implicit val PlanExpireHandler = Macros.handler[PlanExpire]

  implicit val RatingRefundHandler = Macros.handler[RatingRefund]
  implicit val CorresAlarmHandler = Macros.handler[CorresAlarm]
  implicit val IrwinDoneHandler = Macros.handler[IrwinDone]
  implicit val CoachApplyHandler = Macros.handler[CoachApply]
  implicit val CoachApprovedHandler = Macros.handler[CoachApproved]
  implicit val TeamApplyHandler = Macros.handler[TeamApply]
  implicit val TeamApprovedHandler = Macros.handler[TeamApproved]
  implicit val GenericLinkHandler = Macros.handler[GenericLink]

  implicit val InvitedToClazzByHandler = stringAnyValHandler[ClazzInvitedBy](_.value, ClazzInvitedBy.apply)
  implicit val ClazzNameHandler = stringAnyValHandler[ClazzName](_.value, ClazzName.apply)
  implicit val ClazzIdHandler = stringAnyValHandler[ClazzId](_.value, ClazzId.apply)

  implicit val InvitedToTeamByHandler = stringAnyValHandler[TeamInvitedBy](_.value, TeamInvitedBy.apply)
  implicit val TeamInviteNameHandler = stringAnyValHandler[TeamName](_.value, TeamName.apply)
  implicit val TeamInviteIdHandler = stringAnyValHandler[TeamId](_.value, TeamId.apply)

  implicit val ColorBSONHandler = new BSONHandler[BSONBoolean, chess.Color] {
    def read(b: BSONBoolean) = chess.Color(b.value)
    def write(c: chess.Color) = BSONBoolean(c.white)
  }

  implicit val NotificationContentHandler = new BSON[NotificationContent] {

    private def writeNotificationContent(notificationContent: NotificationContent) = {
      notificationContent match {
        case MentionedInThread(mentionedBy, topic, topicId, category, postId) =>
          $doc("mentionedBy" -> mentionedBy, "topic" -> topic, "topicId" -> topicId, "category" -> category, "postId" -> postId)
        case InvitedToStudy(invitedBy, studyName, studyId) =>
          $doc("invitedBy" -> invitedBy, "studyName" -> studyName, "studyId" -> studyId)
        case p: PrivateMessage => PrivateMessageHandler.write(p)
        case t: TeamJoined => TeamJoinedHandler.write(t)
        case o: TeamMadeOwner => TeamMadeOwnerHandler.write(o)
        case LimitedTournamentInvitation => $empty
        case x: TitledTournamentInvitation => TitledTournamentInvitationHandler.write(x)
        case x: GameEnd => GameEndHandler.write(x)
        case x: PlanStart => PlanStartHandler.write(x)
        case x: PlanExpire => PlanExpireHandler.write(x)
        case x: RatingRefund => RatingRefundHandler.write(x)
        case ReportedBanned => $empty
        case CoachReview => $empty
        case x: CorresAlarm => CorresAlarmHandler.write(x)
        case x: IrwinDone => IrwinDoneHandler.write(x)
        case x: CoachApply => CoachApplyHandler.write(x)
        case x: CoachApproved => CoachApprovedHandler.write(x)
        case x: TeamApply => TeamApplyHandler.write(x)
        case x: TeamApproved => TeamApprovedHandler.write(x)
        case x: GenericLink => GenericLinkHandler.write(x)
        case InvitedToClazz(invitedBy, clazzName, clazzId) =>
          $doc("invitedBy" -> invitedBy, "clazzName" -> clazzName, "clazzId" -> clazzId)
        case InvitedToTeam(invitedBy, teamName, teamId) =>
          $doc("invitedBy" -> invitedBy, "teamName" -> teamName, "teamId" -> teamId)
      }
    } ++ $doc("type" -> notificationContent.key)

    private def readMentionedNotification(reader: Reader): MentionedInThread = {
      val mentionedBy = reader.get[MentionedBy]("mentionedBy")
      val topic = reader.get[Topic]("topic")
      val topicId = reader.get[TopicId]("topicId")
      val category = reader.get[Category]("category")
      val postNumber = reader.get[PostId]("postId")

      MentionedInThread(mentionedBy, topic, topicId, category, postNumber)
    }

    private def readInvitedStudyNotification(reader: Reader): NotificationContent = {
      val invitedBy = reader.get[InvitedBy]("invitedBy")
      val studyName = reader.get[StudyName]("studyName")
      val studyId = reader.get[StudyId]("studyId")

      InvitedToStudy(invitedBy, studyName, studyId)
    }

    private def readInvitedClazzNotification(reader: Reader): NotificationContent = {
      val invitedBy = reader.get[ClazzInvitedBy]("invitedBy")
      val clazzName = reader.get[ClazzName]("clazzName")
      val clazzId = reader.get[ClazzId]("clazzId")

      InvitedToClazz(invitedBy, clazzName, clazzId)
    }

    private def readInvitedTeamNotification(reader: Reader): NotificationContent = {
      val invitedBy = reader.get[TeamInvitedBy]("invitedBy")
      val teamName = reader.get[TeamName]("teamName")
      val teamId = reader.get[TeamId]("teamId")

      InvitedToTeam(invitedBy, teamName, teamId)
    }

    def reads(reader: Reader): NotificationContent = reader.str("type") match {
      case "mention" => readMentionedNotification(reader)
      case "invitedStudy" => readInvitedStudyNotification(reader)
      case "privateMessage" => PrivateMessageHandler read reader.doc
      case "teamJoined" => TeamJoinedHandler read reader.doc
      case "teamMadeOwner" => TeamMadeOwnerHandler read reader.doc
      case "u" => LimitedTournamentInvitation
      case "titledTourney" => TitledTournamentInvitationHandler read reader.doc
      case "gameEnd" => GameEndHandler read reader.doc
      case "planStart" => PlanStartHandler read reader.doc
      case "planExpire" => PlanExpireHandler read reader.doc
      case "ratingRefund" => RatingRefundHandler read reader.doc
      case "reportedBanned" => ReportedBanned
      case "coachReview" => CoachReview
      case "corresAlarm" => CorresAlarmHandler read reader.doc
      case "irwinDone" => IrwinDoneHandler read reader.doc
      case "coachApply" => CoachApplyHandler read reader.doc
      case "coachApproved" => CoachApprovedHandler read reader.doc
      case "teamApply" => TeamApplyHandler read reader.doc
      case "teamApproved" => TeamApprovedHandler read reader.doc
      case "genericLink" => GenericLinkHandler read reader.doc
      case "invitedClazz" => readInvitedClazzNotification(reader)
      case "inviteTeam" => readInvitedTeamNotification(reader)
    }

    def writes(writer: Writer, n: NotificationContent): dsl.Bdoc = writeNotificationContent(n)
  }

  implicit val NotificationBSONHandler = Macros.handler[Notification]
}
