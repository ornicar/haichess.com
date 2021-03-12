package lila.team

import akka.actor.ActorSelection
import lila.common.MaxPerPage
import lila.db.dsl._
import org.joda.time.DateTime
import lila.common.paginator.Paginator
import lila.db.paginator.Adapter
import lila.team.actorApi.InsertTeam
import lila.user.UserRepo

final class CertificationApi(
    coll: Colls,
    notifier: Notifier,
    indexer: ActorSelection,
    adminUid: String,
    hub: lila.hub.Env
) {

  import BSONHandlers._

  def certificationSend(team: Team, cd: CertificationData): Funit =
    TeamRepo.addCertification(team.id, Certification(
      leader = cd.leader,
      leaderContact = cd.cellphone,
      members = cd.members.toInt,
      org = cd.org,
      addr = cd.addr,
      message = cd.message,
      businessLicense = cd.businessLicense,
      status = Certification.Status.Applying,
      applyAt = DateTime.now
    )) >>- notifier.certificationSend(team, adminUid)

  def modPage(page: Int, status: Certification.Status): Fu[Paginator[Team]] = {
    val adapter = new Adapter[Team](
      collection = coll.team,
      selector = $doc("certification.status" -> status.id),
      projection = $empty,
      sort = $sort desc "certification.applyAt"
    )
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

  def processCertification(team: Team, approve: Boolean, comments: Option[String]): Funit = {
    val status = if (approve) Certification.Status.Approved else Certification.Status.Rejected
    for {
      _ <- TeamRepo.toggleStatus(team.id, status)
      _ <- TeamRepo.setCertComments(team.id, comments)
    } yield {
      notifier.approveSend(team, status) >>- (indexer ! InsertTeam(team))
      hub.bus.publish(lila.hub.actorApi.team.Certify(team.id, team.createdBy, approve), 'teamCertify)
    }
  }

}
