package lila.appt

import chess.format.Forsyth
import lila.challenge.Challenge
import lila.common.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.user.User
import lila.game.Game
import org.joda.time.DateTime
import lila.contest.{ BoardRepo, Contest, Round }
import lila.db.paginator.Adapter

final class ApptApi(coll: Coll, bus: lila.common.Bus) {

  import BSONHandlers._

  def byId(id: Game.ID): Fu[Option[Appt]] = coll.byId[Appt](id)

  def createByContest(contest: Contest, round: Round): Funit =
    BoardRepo.getByRound(round.id).flatMap { boards =>
      val appts = boards.map { board =>
        Appt.make(
          gameId = board.id,
          position = contest.position.fen,
          variant = contest.variant,
          rated = contest.mode.rated,
          clock = contest.clock.some,
          daysPerTurn = None,
          minDateTime = round.actualStartsAt,
          maxDateTime = round.actualStartsAt.plusMinutes(contest.roundSpace).minusMinutes(contest.apptDeadline | 0),
          whitePlayerUid = board.whitePlayer.userId,
          blackPlayerUid = board.blackPlayer.userId,
          records = List(
            ApptRecord.make(
              time = round.actualStartsAt,
              message = "系统消息".some,
              current = true,
              whiteStatus = ApptRecord.Status.Created,
              blackStatus = ApptRecord.Status.Created,
              applyBy = None
            )
          ),
          contest = ApptContest.make(contest, round.no, board.no).some,
          createBy = None
        )
      }
      val userIds = appts.flatMap(appt => List(appt.whitePlayerUid, appt.blackPlayerUid))
      bulkInsert(appts) >>- bus.publish(lila.hub.actorApi.home.ReloadAppt(userIds), 'changeAppt)
    }

  def createByChallenge(c: Challenge): Funit = {
    val anonymous = "anonymous"
    val appt = Appt.make(
      gameId = c.id,
      position = c.initialFen.map(_.value) | Forsyth.initial,
      variant = c.variant,
      rated = c.mode.rated,
      clock = c.clock.map(_.config),
      daysPerTurn = c.daysPerTurn,
      minDateTime = DateTime.now,
      maxDateTime = c.expiresAt,
      whitePlayerUid = c.destUserId | anonymous,
      blackPlayerUid = c.challengerUserId | anonymous,
      records = List(
        ApptRecord.make(
          time = c.apptStartsAt.get,
          message = "系统消息".some,
          current = true,
          whiteStatus = ApptRecord.Status.Created,
          blackStatus = ApptRecord.Status.Confirmed,
          applyBy = c.challengerUserId
        )
      ),
      contest = None,
      createBy = c.challengerUserId
    )
    coll.insert(appt).void >>-
      publishCreateCalendar(appt, appt.currentRecord, c.challengerUserId | anonymous) >>-
      bus.publish(lila.hub.actorApi.home.ReloadAppt(List(c.destUserId | anonymous, c.challengerUserId | anonymous)), 'changeAppt)
  }

  def cancel(c: Challenge): Funit = {
    coll.updateField($id(c.id), "canceled", true).void >> {
      byId(c.id) map {
        case Some(appt) => {
          publishRemoveCalendar(appt, appt.currentRecord, appt.whitePlayerUid)
          publishRemoveCalendar(appt, appt.currentRecord, appt.blackPlayerUid)
          bus.publish(lila.hub.actorApi.home.ReloadAppt(List(appt.whitePlayerUid, appt.blackPlayerUid)), 'changeAppt)
        }
        case None =>
      }
    }
  }

  private def bulkInsert(appts: List[Appt]): Funit =
    coll.bulkInsert(
      documents = appts.map(ApptHandler.write).toStream,
      ordered = true
    ).void

  def update(appt: Appt, oldRecord: ApptRecord, user: User.ID): Funit = (!appt.canceled).?? {
    coll.update($id(appt.id), appt).void >>- {
      if (appt.isConfirmed) {
        bus.publish(lila.hub.actorApi.game.ApptCompleteBus(appt.id, user.some, appt.time, appt.source), 'apptCompleteBus)
      } else {
        bus.publish(lila.hub.actorApi.game.ApptProcessBus(appt.id, user, appt.source), 'apptProcessBus)
      }
    } >>- {
      val newRecord = appt.currentRecord
      if (oldRecord.id != newRecord.id) {
        publishRemoveCalendar(appt, oldRecord, user)
        publishRemoveCalendar(appt, oldRecord, appt.opponent(user).err(s"cannot find opponent of ${user}"))
      }
      publishCreateCalendar(appt, newRecord, user)
    } >>- bus.publish(lila.hub.actorApi.home.ReloadAppt(List(user, appt.opponent(user) | "")), 'changeAppt)
  }

  def setTime(contest: Contest, board: lila.contest.Board, time: DateTime): Funit =
    byId(board.id) flatMap {
      case None => funit
      case Some(appt) => (!appt.canceled).?? {
        val oldRecord = appt.currentRecord
        val newAppt = appt.withSetRecord(time, contest.createdBy)
        coll.update($id(appt.id), newAppt).void >>- {
          val newRecord = newAppt.currentRecord
          val userIds = List(newAppt.whitePlayerUid, newAppt.blackPlayerUid)
          userIds.foreach { user =>
            publishRemoveCalendar(newAppt, oldRecord, user)
            publishCreateCalendar(newAppt, newRecord, user)
          }
          bus.publish(lila.hub.actorApi.home.ReloadAppt(userIds), 'changeAppt)
        }
      }
    }

  private def publishRemoveCalendar(appt: Appt, oldRecord: ApptRecord, user: User.ID) =
    bus.publish(
      lila.hub.actorApi.calendar.CalendarRemove(s"${appt.id}@${oldRecord.id}@$user"),
      'calendarRemoveBus
    )

  private def publishCreateCalendar(appt: Appt, newRecord: ApptRecord, user: User.ID) =
    bus.publish(
      lila.hub.actorApi.calendar.CalendarCreate(
        id = s"${appt.id}@${newRecord.id}@$user".some,
        typ = "appt",
        user = user,
        sdt = appt.currentTime,
        edt = appt.maxDateTime,
        content = makeContent(appt, user),
        onlySdt = true,
        link = (appt.contest.map { c => s"/contest/${c.id}" } | s"/${appt.id}").some,
        icon = (appt.contest.map(_ => "赛") | "U").some,
        bg = "#90360e".some
      ),
      'calendarCreateBus
    )

  private def makeContent(appt: Appt, user: User.ID) = {
    if (appt.isContest) {
      appt.contest.map { c =>
        s"${c.name} 第${c.roundNo}轮 #${c.boardNo}"
      } | "比赛"
    } else {
      s"${appt.opponent(user) | "未知棋手"}${System.lineSeparator()}${appt.showClock} • ${if (appt.rated) "有积分" else "无积分"} • ${appt.variant.name}"
    }
  }

  def home(user: User.ID): Fu[List[Appt]] =
    coll.find(
      $doc(
        "whitePlayerUid" -> user,
        "confirmed" -> 0,
        "canceled" -> false,
        "maxDateTime" $gt DateTime.now,
        "records" $elemMatch $doc(
          "current" -> true,
          "whiteStatus" -> "created"
        )
      )
    ).sort($doc("updateAt" -> -1)).list[Appt](3).flatMap { list1 =>
        coll.find(
          $doc(
            "blackPlayerUid" -> user,
            "confirmed" -> 0,
            "canceled" -> false,
            "maxDateTime" $gt DateTime.now,
            "records" $elemMatch $doc(
              "current" -> true,
              "blackStatus" -> "created"
            )
          )
        ).sort($doc("updateAt" -> -1)).list[Appt](3).map { list2 =>
            (list1 ++ list2).sortBy(_.updateAt).reverse.take(3)
          }
      }

  def page(user: User.ID, page: Int): Fu[Paginator[Appt]] = {
    val adapter = new Adapter[Appt](
      collection = coll,
      selector = $or(
        $doc("whitePlayerUid" -> user),
        $doc("blackPlayerUid" -> user)
      ) ++ $doc("canceled" -> false),
      projection = $empty,
      sort = $sort desc "updateAt"
    )
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = MaxPerPage(15)
    )
  }

}
