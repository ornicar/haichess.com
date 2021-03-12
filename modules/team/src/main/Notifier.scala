package lila.team

import lila.notify.Notification.Notifies
import lila.notify.TeamJoined.{ Id => TJId, Name => TJName }
import lila.notify.TeamMadeOwner.{ Id => TMOId, Name => TMOName }
import lila.notify.{ InvitedToTeam, Notification, NotifyApi, TeamApply, TeamApproved, TeamJoined, TeamMadeOwner }
import lila.user.User

private[team] final class Notifier(notifyApi: NotifyApi) {

  def acceptRequest(team: Team, request: Request) = {
    val notificationContent = TeamJoined(TJId(team.id), TJName(team.name))
    val notification = Notification.make(Notifies(request.user), notificationContent)
    notifyApi.addNotification(notification)
  }

  def madeOwner(team: Team, newOwner: String) = {
    val notificationContent = TeamMadeOwner(TMOId(team.id), TMOName(team.name))
    val notification = Notification.make(Notifies(newOwner), notificationContent)
    notifyApi.addNotification(notification)
  }

  def certificationSend(team: Team, adminUid: User.ID) = {
    notifyApi.addNotification(
      Notification.make(Notification.Notifies(adminUid), TeamApply(team.createdBy, team.id))
    )
  }

  def approveSend(team: Team, status: Certification.Status) = {
    notifyApi.addNotification(
      Notification.make(Notification.Notifies(team.createdBy), TeamApproved(team.id, status.id))
    )
  }

  def inviteSend(team: Team, coachId: User.ID): Funit = {
    val notificationContent = InvitedToTeam(
      InvitedToTeam.InvitedBy(team.createdBy),
      InvitedToTeam.TeamName(team.name),
      InvitedToTeam.TeamId(team.id)
    )
    notifyApi.addNotification(
      Notification.make(Notification.Notifies(coachId), notificationContent)
    )
  }

}
