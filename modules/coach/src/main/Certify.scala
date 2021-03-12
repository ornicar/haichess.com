package lila.coach

import org.joda.time.DateTime

case class Certify(
    certNo: String,
    status: Option[Certify.Status] = None,
    applyAt: Option[DateTime] = None,
    approvedAt: Option[DateTime] = None
) {

  import Certify.Status
  def passed = status.??(_.passed)
  def overPassed = status.??(s => s == Status.Passed || s == Status.Applying || s == Status.Approved || s == Status.Rejected)
  def applying = status.??(_.applying)
  def approved = status.??(_.approved)
  def rejected = status.??(_.rejected)
}

object Certify {

  sealed abstract class Status(val id: String, val name: String) {
    def passed = id == Status.Passed.id
    def applying = id == Status.Applying.id
    def approved = id == Status.Approved.id
    def rejected = id == Status.Rejected.id
  }
  object Status {
    case object Passed extends Status("passed", "实名认证通过")
    case object Applying extends Status("applying", "审核中")
    case object Approved extends Status("approved", "已通过")
    case object Rejected extends Status("rejected", "已拒绝")

    def all = List(Passed, Applying, Approved, Rejected)

    def apply(id: String): Status = all.find(_.id == id) getOrElse Applying
  }

}
