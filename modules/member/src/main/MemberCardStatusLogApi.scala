package lila.member

import lila.db.dsl.Coll
import lila.member.MemberCard.CardStatus
import lila.user.User
import org.joda.time.DateTime
import ornicar.scalalib.Random

case class MemberCardStatusLogApi(coll: Coll) {

  import BSONHandlers.MemberCardStatusLogBSONHandler

  def setLog(
    card: MemberCard,
    status: CardStatus,
    userId: User.ID,
    note: Option[String]
  ): Funit = coll.insert(
    MemberCardStatusLog(
      _id = Random nextString 8,
      cardId = card.id,
      sourceStatus = card.status,
      currentStatus = status,
      createBy = userId,
      createAt = DateTime.now,
      note = note
    )
  ).void

}
