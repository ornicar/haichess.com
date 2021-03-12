package lila.contest

import chess.Clock.{ Config => ClockConfig }
import chess.variant.Variant
import chess.{ Mode, StartingPosition }
import lila.db.BSON
import lila.db.dsl._
import reactivemongo.bson._
import org.joda.time.DateTime

object BSONHandlers {

  import Condition.BSONHandlers.AllBSONHandler
  import lila.db.BSON.BSONJodaDateTimeHandler

  implicit val arrayHandler = bsonArrayToListHandler[String]

  implicit val ContestClockBSONHandler = new BSONHandler[BSONDocument, ClockConfig] {
    def read(doc: BSONDocument) = ClockConfig(
      doc.getAs[Int]("limit").get,
      doc.getAs[Int]("increment").get
    )

    def write(config: ClockConfig) = BSONDocument(
      "limit" -> config.limitSeconds,
      "increment" -> config.incrementSeconds
    )
  }

  implicit val BtsssBSONHandler = new BSONHandler[BSONArray, Btsss] {
    private val btssBSONHandler = bsonArrayToListHandler[String]

    def read(doc: BSONArray) = Btsss(btssBSONHandler.read(doc).map(Btss(_)))

    def write(btsss: Btsss) = BSONArray(btsss.list.map(_.id).map(BSONString.apply))
  }

  implicit val contestHandler = new BSON[Contest] {

    def reads(r: BSON.Reader) = {
      Contest(
        id = r str "_id",
        name = r str "name",
        groupName = r strO "groupName",
        logo = r strO "logo",
        typ = Contest.Type(r str ("typ")),
        organizer = r str "organizer",
        variant = r.intO("variant").fold[Variant](Variant.default)(Variant.orDefault),
        position = {
          val fen = r.str("position")
          Thematic.byFen(fen) | StartingPosition(
            eco = "",
            name = "",
            fen = fen,
            wikiPath = "",
            moves = "",
            featurable = false
          )
        },
        mode = r.intO("mode") flatMap Mode.apply getOrElse Mode.Rated,
        clock = r.get[ClockConfig]("clock"),
        rule = Contest.Rule(r str ("rule")),
        startsAt = r.get[DateTime]("startsAt"),
        finishAt = r.get[DateTime]("finishAt"),
        deadline = r int "deadline",
        deadlineAt = r.get[DateTime]("deadlineAt"),
        maxPlayers = r int "maxPlayers",
        minPlayers = r int "minPlayers",
        conditions = r.getO[Condition.All]("conditions") getOrElse Condition.All.empty,
        roundSpace = r int "roundSpace",
        rounds = r int "rounds",
        appt = r boolD "appt",
        apptDeadline = r intO "apptDeadline",
        swissBtss = r.get[Btsss]("swissBtss"),
        roundRobinBtss = r.get[Btsss]("roundRobinBtss"),
        canLateMinute = r int "canLateMinute",
        canQuitNumber = r int "canQuitNumber",
        enterApprove = r bool "enterApprove",
        autoPairing = r bool "autoPairing",
        enterCost = (r double "enterCost").toInt,
        hasPrizes = r bool "hasPrizes",
        description = r strO "description",
        attachments = r strO "attachments",
        nbPlayers = r int "nbPlayers",
        allRoundFinished = r boolD "allRoundFinished",
        currentRound = r int "currentRound",
        status = Contest.Status(r int "status"),
        realFinishAt = r.getO[DateTime]("realFinishAt"),
        createdBy = r str "createdBy",
        createdAt = r.get[DateTime]("createdAt")
      )
    }

    def writes(w: BSON.Writer, o: Contest) = $doc(
      "_id" -> o.id,
      "name" -> o.name,
      "groupName" -> o.groupName,
      "logo" -> o.logo,
      "typ" -> o.typ.id,
      "organizer" -> o.organizer,
      "variant" -> o.variant.some.filterNot(_.standard).map(_.id),
      "position" -> o.position.fen,
      "mode" -> o.mode.some.filterNot(_.rated).map(_.id),
      "clock" -> ContestClockBSONHandler.write(o.clock),
      "rule" -> o.rule.id,
      "startsAt" -> o.startsAt,
      "finishAt" -> o.finishAt,
      "deadline" -> o.deadline,
      "deadlineAt" -> o.deadlineAt,
      "maxPlayers" -> o.maxPlayers,
      "minPlayers" -> o.minPlayers,
      "conditions" -> o.conditions.ifNonEmpty,
      "roundSpace" -> o.roundSpace,
      "rounds" -> o.rounds,
      "appt" -> o.appt,
      "apptDeadline" -> o.apptDeadline,
      "swissBtss" -> o.swissBtss,
      "roundRobinBtss" -> o.roundRobinBtss,
      "canLateMinute" -> o.canLateMinute,
      "canQuitNumber" -> o.canQuitNumber,
      "enterApprove" -> o.enterApprove,
      "autoPairing" -> o.autoPairing,
      "enterCost" -> o.enterCost.toDouble,
      "hasPrizes" -> w.bool(o.hasPrizes),
      "description" -> o.description,
      "attachments" -> o.attachments,
      "nbPlayers" -> o.nbPlayers,
      "allRoundFinished" -> o.allRoundFinished,
      "currentRound" -> o.currentRound,
      "status" -> o.status.id,
      "realFinishAt" -> o.realFinishAt,
      "createdAt" -> o.createdAt,
      "createdBy" -> o.createdBy
    )
  }

  implicit val roundHandler = new BSON[Round] {
    def reads(r: BSON.Reader) = Round(
      id = r str "_id",
      no = r int "no",
      contestId = r str "contestId",
      status = Round.Status(r int "status"),
      startsAt = r.get[DateTime]("startsAt"),
      actualStartsAt = r.get[DateTime]("actualStartsAt"),
      finishAt = r.getO[DateTime]("finishAt"),
      boards = r intO "boards"
    )

    def writes(w: BSON.Writer, r: Round) = $doc(
      "_id" -> r.id,
      "no" -> r.no,
      "contestId" -> r.contestId,
      "status" -> r.status.id,
      "startsAt" -> r.startsAt,
      "actualStartsAt" -> r.actualStartsAt,
      "finishAt" -> r.finishAt,
      "boards" -> r.boards
    )
  }

  implicit val requestBSONHandler = new BSON[Request] {
    def reads(r: BSON.Reader) = Request(
      id = r str "_id",
      contestId = r str "contestId",
      contestName = r str "contestName",
      userId = r str "userId",
      message = r str "message",
      status = Request.RequestStatus(r str "status"),
      date = r date "date"
    )

    def writes(w: BSON.Writer, r: Request) = $doc(
      "_id" -> r.id,
      "contestId" -> r.contestId,
      "contestName" -> r.contestName,
      "userId" -> r.userId,
      "message" -> r.message,
      "status" -> r.status.id,
      "date" -> r.date
    )
  }

  implicit val inviteBSONHandler = new BSON[Invite] {
    def reads(r: BSON.Reader) = Invite(
      id = r str "_id",
      contestId = r str "contestId",
      contestName = r str "contestName",
      userId = r str "userId",
      status = Invite.InviteStatus(r str "status"),
      date = r date "date"
    )

    def writes(w: BSON.Writer, i: Invite) = $doc(
      "_id" -> i.id,
      "contestId" -> i.contestId,
      "contestName" -> i.contestName,
      "userId" -> i.userId,
      "status" -> i.status.id,
      "date" -> i.date
    )
  }

  implicit val playerHandler = new BSON[Player] {
    def reads(r: BSON.Reader) = Player(
      id = r str "_id",
      no = r int "no",
      contestId = r str "contestId",
      userId = r str "userId",
      rating = r int "rating",
      provisional = r bool "provisional",
      teamRating = r intO "teamRating",
      score = r double "score",
      points = r double "points",
      absent = r bool "absent",
      leave = r boolD "leave",
      quit = r boolD "quit",
      kick = r boolD "kick",
      manualAbsent = r boolD "manualAbsent",
      cancelled = r boolD "cancelled",
      outcomes = (r strsD "outcomes") map (Board.Outcome(_)),
      external = r boolD "external",
      entryTime = r date "entryTime"
    )

    def writes(w: BSON.Writer, r: Player) = $doc(
      "_id" -> r.id,
      "no" -> r.no,
      "contestId" -> r.contestId,
      "userId" -> r.userId,
      "rating" -> r.rating,
      "provisional" -> r.provisional,
      "teamRating" -> r.teamRating,
      "score" -> r.score,
      "points" -> r.points,
      "absent" -> r.absent,
      "leave" -> r.leave,
      "quit" -> r.quit,
      "kick" -> r.kick,
      "manualAbsent" -> r.manualAbsent,
      "cancelled" -> r.cancelled,
      "outcomes" -> r.outcomes.map(_.id),
      "external" -> r.external,
      "entryTime" -> r.entryTime
    )
  }

  implicit val StatusBSONHandler = new BSONHandler[BSONInteger, chess.Status] {
    def read(bsonInt: BSONInteger): chess.Status = chess.Status(bsonInt.value) err s"No such status: ${bsonInt.value}"
    def write(x: chess.Status) = BSONInteger(x.id)
  }

  implicit val miniPlayerHandler = new BSON[Board.MiniPlayer] {
    def reads(r: BSON.Reader) = Board.MiniPlayer(
      id = r str "id",
      userId = r str "userId",
      no = r int "no",
      isWinner = r boolO "isWinner"
    )

    def writes(w: BSON.Writer, p: Board.MiniPlayer) =
      $doc(
        "id" -> p.id,
        "userId" -> p.userId,
        "no" -> p.no,
        "isWinner" -> p.isWinner
      )
  }

  implicit val boardHandler = new BSON[Board] {
    def reads(r: BSON.Reader) = Board(
      id = r str "_id",
      no = r int "no",
      contestId = r str "contestId",
      roundId = r str "roundId",
      roundNo = r int "roundNo",
      status = r.get[chess.Status]("status"),
      whitePlayer = r.get[Board.MiniPlayer]("whitePlayer"),
      blackPlayer = r.get[Board.MiniPlayer]("blackPlayer"),
      startsAt = r.get[DateTime]("startsAt"),
      appt = r bool "appt",
      apptComplete = r boolD "apptComplete",
      reminded = r boolD "reminded",
      turns = r intO "turns",
      finishAt = r.getO[DateTime]("finishAt")
    )

    def writes(w: BSON.Writer, b: Board) = $doc(
      "_id" -> b.id,
      "no" -> b.no,
      "contestId" -> b.contestId,
      "roundId" -> b.roundId,
      "roundNo" -> b.roundNo,
      "status" -> b.status,
      "whitePlayer" -> b.whitePlayer,
      "blackPlayer" -> b.blackPlayer,
      "startsAt" -> b.startsAt,
      "appt" -> b.appt,
      "apptComplete" -> b.apptComplete,
      "reminded" -> b.reminded,
      "turns" -> b.turns,
      "finishAt" -> b.finishAt
    )
  }

  private implicit val btssBSONHandler = new BSONHandler[BSONString, Btss] {
    def read(bsonString: BSONString): Btss = Btss(bsonString.value)
    def write(b: Btss) = BSONString(b.id)
  }
  private implicit val btssScoreBSONHandler = new BSON[Btss.BtssScore] {
    def reads(r: BSON.Reader): Btss.BtssScore = Btss.BtssScore(
      btss = r.get[Btss]("btss"),
      score = r double "score"
    )
    def writes(w: BSON.Writer, b: Btss.BtssScore) = $doc(
      "btss" -> b.btss,
      "score" -> b.score
    )
  }
  private implicit val btssScoreArrayHandler = lila.db.dsl.bsonArrayToListHandler[Btss.BtssScore]

  implicit val scoreSheetHandler = new BSON[ScoreSheet] {
    def reads(r: BSON.Reader) = ScoreSheet(
      id = r str "_id",
      contestId = r str "contestId",
      roundNo = r int "roundNo",
      playerUid = r str "playerUid",
      playerNo = r int "playerNo",
      score = r double "score",
      rank = r int "rank",
      btssScores = r.get[List[Btss.BtssScore]]("btssScores"),
      cancelled = r boolD "cancelled"
    )

    def writes(w: BSON.Writer, s: ScoreSheet) = $doc(
      "_id" -> s.id,
      "contestId" -> s.contestId,
      "roundNo" -> s.roundNo,
      "playerUid" -> s.playerUid,
      "playerNo" -> s.playerNo,
      "score" -> s.score,
      "rank" -> s.rank,
      "btssScores" -> btssScoreArrayHandler.write(s.btssScores),
      "cancelled" -> s.cancelled
    )
  }

  implicit val forbiddenHandler = Macros.handler[Forbidden]

}
