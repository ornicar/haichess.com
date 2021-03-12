package lila.member

import lila.member.MemberCard.CardStatus
import org.joda.time.DateTime

case class MemberCardStatusLog(
    _id: String,
    cardId: String,
    sourceStatus: CardStatus,
    currentStatus: CardStatus,
    createBy: String,
    createAt: DateTime,
    note: Option[String]
) {

  def id = _id

}
