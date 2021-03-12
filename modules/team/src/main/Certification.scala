package lila.team

import org.joda.time.DateTime

case class Certification(
    leader: String,
    leaderContact: String,
    businessLicense: String,
    members: Int,
    org: String,
    addr: String,
    message: Option[String],
    status: Certification.Status,
    applyAt: DateTime,
    processComments: Option[String] = None,
    processAt: Option[DateTime] = None
)

object Certification {

  sealed abstract class Status(val id: String, val name: String) {
    def applying = id == Status.Applying.id
    def approved = id == Status.Approved.id
    def rejected = id == Status.Rejected.id
  }

  object Status {
    case object Applying extends Status("applying", "审核中")
    case object Approved extends Status("approved", "已通过")
    case object Rejected extends Status("rejected", "已拒绝")

    def all = List(Applying, Approved, Rejected)
    def apply(id: String): Status = all.find(_.id == id) getOrElse Applying
  }
}