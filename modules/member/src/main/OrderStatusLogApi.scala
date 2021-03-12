package lila.member

import lila.db.dsl.Coll
import lila.user.User
import org.joda.time.DateTime
import ornicar.scalalib.Random

case class OrderStatusLogApi(coll: Coll) {

  import BSONHandlers.OrderStatusLogBSONHandler

  def setLog(
    order: Order,
    status: OrderStatus,
    userId: User.ID,
    note: Option[String]
  ): Funit = coll.insert(
    OrderStatusLog(
      _id = Random nextString 8,
      orderNo = order.id,
      sourceStatus = order.status,
      currentStatus = status,
      createBy = userId,
      createAt = DateTime.now,
      note = note
    )
  ).void

}
