package lila.member

import org.joda.time.DateTime

case class OrderStatusLog(
    _id: String, // ID
    orderNo: String, // 订单编号
    sourceStatus: OrderStatus, // 原始状态
    currentStatus: OrderStatus, // 目前状态
    createBy: String, // 操作人
    createAt: DateTime, // 操作时间
    note: Option[String] // 备注
) {

  def id = _id

}
