package lila.member

import lila.db.dsl._
import org.joda.time.DateTime

case class OrderPayApi(coll: Coll) {

  import BSONHandlers.AlipayDataWithTimeBSONHandler
  import BSONHandlers.OrderPayBSONHandler

  def addPayData(order: Order): Funit = coll.insert(
    OrderPay(
      _id = order.id,
      payWay = order.payWay,
      alipayData = List.empty,
      createAt = order.createAt
    )
  ).void

  def setAlipayData(order: Order, alipayData: AlipayData): Funit =
    coll.update(
      $id(order.id),
      $addToSet("alipayData" -> AlipayDataWithTimeBSONHandler.write(AlipayDataWithTime(alipayData, DateTime.now)))
    ).void

}
