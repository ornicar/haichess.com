package lila.member

import lila.user.User
import org.joda.time.DateTime

case class MemberPointsLog(
    _id: String,
    typ: MemberPointsLog.PointsType,
    userId: User.ID,
    diff: Int,
    createAt: DateTime,
    orderId: Option[String]
) {

  def id = _id

}

object MemberPointsLog {

  sealed abstract class PointsType(val id: String, val name: String, symbol: String)
  object PointsType {
    case object OrderPay extends PointsType("orderPay", "积分支付", "-")
    case object OrderPayRebate extends PointsType("orderPayRebate", "支付返点", "+")
    case object BackendGiven extends PointsType("backendGiven", "后台设置", "+/-")
    val all = List(OrderPay, OrderPayRebate, BackendGiven)
    def apply(id: String): PointsType = all.find(_.id == id) err s"can not find PointsType $id"
  }
}

