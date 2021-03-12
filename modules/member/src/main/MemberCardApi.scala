package lila.member

import lila.common.{ Bus, MaxPerPage }
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.hub.actorApi.member.MemberCardUse
import lila.notify.NotifyApi
import lila.user.User
import lila.member.MemberCard.CardStatus
import org.joda.time.DateTime

case class MemberCardApi(coll: Coll, logApi: MemberCardLogApi, statusLogApi: MemberCardStatusLogApi, bus: Bus, notifyApi: NotifyApi) {

  import BSONHandlers.MemberCardBSONHandler

  def byId(id: String): Fu[Option[MemberCard]] = coll.byId(id)

  def existsGoldCard(userId: User.ID): Fu[Boolean] =
    coll.exists(
      $doc(
        "userId" -> userId,
        "level" -> lila.user.MemberLevel.Gold.code,
        "status" -> MemberCard.CardStatus.Create.id
      )
    )

  def existsSilverCard(userId: User.ID): Fu[Boolean] =
    coll.exists(
      $doc(
        "userId" -> userId,
        "level" -> lila.user.MemberLevel.Silver.code,
        "status" -> MemberCard.CardStatus.Create.id
      )
    )

  def mine(userId: User.ID): Fu[List[MemberCard]] = {
    for {
      create <- coll.find(
        $doc(
          "userId" -> userId,
          "status" -> MemberCard.CardStatus.Create.id
        )
      ).sort($sort desc "expireAt").list()
      used <- coll.find(
        $doc(
          "userId" -> userId,
          "status" -> MemberCard.CardStatus.Used.id
        )
      ).sort($sort desc "expireAt").list()
      expired <- coll.find(
        $doc(
          "userId" -> userId,
          "status" -> MemberCard.CardStatus.Expired.id
        )
      ).sort($sort desc "expireAt").list()
    } yield create ++ used ++ expired
  }

  def minePage(userId: User.ID, page: Int, status: Option[MemberCard.CardStatus], level: Option[lila.user.MemberLevel]): Fu[Paginator[MemberCard]] = {
    val adapter = new Adapter[MemberCard](
      collection = coll,
      selector = $doc(
        "userId" -> userId
      ) ++ status.??(s => $doc("status" -> s.id)) ++ level.??(l => $doc("level" -> l.code)),
      projection = $empty,
      sort = $sort desc "expireAt"
    )
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

  def setStatus(card: MemberCard, status: CardStatus): Funit =
    coll.update(
      $id(card.id),
      $set("status" -> status.id)
    ).void >> logApi.setStatus(card.id, status) >> statusLogApi.setLog(
        card = card,
        status = status,
        userId = status match {
          case CardStatus.Used => card.userId
          case CardStatus.Expired => "system"
          case _ => "-"
        },
        note = none
      )

  def findLastBatch(): Fu[Int] = {
    coll.find($empty, $doc("batch" -> true))
      .sort($sort desc "batch")
      .uno[Bdoc] map {
        _ flatMap { doc => doc.getAs[Int]("batch") map (1+) } getOrElse 1
      }
  }

  def addCardByOrder(order: Order): Funit = findLastBatch flatMap { batch =>
    val cards = MemberCard.makeByOrder(order, batch)
    coll.bulkInsert(
      documents = cards.map(MemberCardBSONHandler.write).toStream,
      ordered = true
    ).void
  }

  def addCardBySignup(userId: User.ID): Funit =
    logApi.hasSignupGiven(userId) flatMap { exists =>
      (!exists).?? {
        findLastBatch flatMap { batch =>
          val card = MemberCard.makeBySignup(userId, batch)
          coll.insert(card) >> logApi.setLog(
            userId = userId,
            card = card.copy(userId = "admin"),
            typ = MemberCardLog.Type.SignupGiven,
            note = none
          )
        }
      }
    }

  def give(card: MemberCard, userId: String): Funit =
    coll.update(
      $id(card.id),
      $set("userId" -> userId)
    ).void >> logApi.setLog(
        userId = userId,
        card = card,
        typ = MemberCardLog.Type.CoachOrTeam,
        note = none
      )

  def use(user: User, card: MemberCard): Funit = {
    val oldExpireAt = user.memberOrDefault.levels.get(card.level.code).map(_.expireAt) | DateTime.now
    setStatus(card, CardStatus.Used) >>- bus.publish(
      MemberCardUse(
        userId = card.userId,
        level = card.level.code,
        days = card.days.toDays(oldExpireAt),
        desc = card.desc
      ), 'memberCardUse
    )
  }

  def expiredTesting =
    coll.find(
      $doc(
        "status" -> CardStatus.Create.id,
        "expireAt" $lt DateTime.now
      )
    ).list(100) flatMap { cards =>
        cards.map { card =>
          setStatus(card, CardStatus.Expired)
        }.sequenceFu.void
      }

}
