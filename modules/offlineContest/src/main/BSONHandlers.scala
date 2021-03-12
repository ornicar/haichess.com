package lila.offlineContest

import lila.db.BSON
import lila.db.dsl._
import reactivemongo.bson._
import org.joda.time.DateTime

object BSONHandlers {

  import lila.db.BSON.BSONJodaDateTimeHandler

  implicit val arrayHandler = bsonArrayToListHandler[String]

  implicit val BtsssBSONHandler = new BSONHandler[BSONArray, OffBtsss] {
    private val btssBSONHandler = bsonArrayToListHandler[String]

    def read(doc: BSONArray) = OffBtsss(btssBSONHandler.read(doc).map(OffBtss(_)))

    def write(btsss: OffBtsss) = BSONArray(btsss.list.map(_.id).map(BSONString.apply))
  }

  implicit val contestHandler = new BSON[OffContest] {

    def reads(r: BSON.Reader) = {
      OffContest(
        id = r str "_id",
        name = r str "name",
        groupName = r strO "groupName",
        logo = r strO "logo",
        typ = OffContest.Type(r str "typ"),
        organizer = r str "organizer",
        rule = OffContest.Rule(r str "rule"),
        rounds = r int "rounds",
        swissBtss = r.get[OffBtsss]("swissBtss"),
        roundRobinBtss = r.get[OffBtsss]("roundRobinBtss"),
        nbPlayers = r int "nbPlayers",
        currentRound = r int "currentRound",
        status = OffContest.Status(r int "status"),
        createdBy = r str "createdBy",
        createdAt = r.get[DateTime]("createdAt")
      )
    }

    def writes(w: BSON.Writer, o: OffContest) = $doc(
      "_id" -> o.id,
      "name" -> o.name,
      "groupName" -> o.groupName,
      "logo" -> o.logo,
      "typ" -> o.typ.id,
      "organizer" -> o.organizer,
      "rule" -> o.rule.id,
      "rounds" -> o.rounds,
      "swissBtss" -> o.swissBtss,
      "roundRobinBtss" -> o.roundRobinBtss,
      "nbPlayers" -> o.nbPlayers,
      "currentRound" -> o.currentRound,
      "status" -> o.status.id,
      "createdAt" -> o.createdAt,
      "createdBy" -> o.createdBy
    )
  }

  implicit val roundHandler = new BSON[OffRound] {
    def reads(r: BSON.Reader) = OffRound(
      id = r str "_id",
      no = r int "no",
      contestId = r str "contestId",
      status = OffRound.Status(r int "status"),
      boards = r intO "boards"
    )

    def writes(w: BSON.Writer, r: OffRound) = $doc(
      "_id" -> r.id,
      "no" -> r.no,
      "contestId" -> r.contestId,
      "status" -> r.status.id,
      "boards" -> r.boards
    )
  }

  implicit val playerHandler = new BSON[OffPlayer] {
    def reads(r: BSON.Reader) = OffPlayer(
      id = r str "_id",
      no = r int "no",
      contestId = r str "contestId",
      userId = r str "userId",
      teamRating = r intO "teamRating",
      score = r double "score",
      points = r double "points",
      absent = r bool "absent",
      kick = r bool "kick",
      manualAbsent = r boolD "manualAbsent",
      outcomes = (r strsD "outcomes") map (OffBoard.Outcome(_)),
      external = r boolD "external"
    )

    def writes(w: BSON.Writer, r: OffPlayer) = $doc(
      "_id" -> r.id,
      "no" -> r.no,
      "contestId" -> r.contestId,
      "userId" -> r.userId,
      "teamRating" -> r.teamRating,
      "score" -> r.score,
      "points" -> r.points,
      "absent" -> r.absent,
      "kick" -> r.kick,
      "manualAbsent" -> r.manualAbsent,
      "outcomes" -> r.outcomes.map(_.id),
      "external" -> r.external
    )
  }

  implicit val miniPlayerHandler = new BSON[OffBoard.MiniPlayer] {
    def reads(r: BSON.Reader) = OffBoard.MiniPlayer(
      id = r str "id",
      userId = r str "userId",
      no = r int "no",
      isWinner = r boolO "isWinner"
    )

    def writes(w: BSON.Writer, p: OffBoard.MiniPlayer) =
      $doc(
        "id" -> p.id,
        "userId" -> p.userId,
        "no" -> p.no,
        "isWinner" -> p.isWinner
      )
  }

  implicit val boardHandler = new BSON[OffBoard] {
    def reads(r: BSON.Reader) = OffBoard(
      id = r str "_id",
      no = r int "no",
      contestId = r str "contestId",
      roundId = r str "roundId",
      roundNo = r int "roundNo",
      status = OffBoard.Status(r int "status"),
      whitePlayer = r.get[OffBoard.MiniPlayer]("whitePlayer"),
      blackPlayer = r.get[OffBoard.MiniPlayer]("blackPlayer")
    )

    def writes(w: BSON.Writer, b: OffBoard) = $doc(
      "_id" -> b.id,
      "no" -> b.no,
      "contestId" -> b.contestId,
      "roundId" -> b.roundId,
      "roundNo" -> b.roundNo,
      "status" -> b.status.id,
      "whitePlayer" -> b.whitePlayer,
      "blackPlayer" -> b.blackPlayer
    )
  }

  private implicit val btssBSONHandler = new BSONHandler[BSONString, OffBtss] {
    def read(bsonString: BSONString): OffBtss = OffBtss(bsonString.value)
    def write(b: OffBtss) = BSONString(b.id)
  }
  private implicit val btssScoreBSONHandler = new BSON[OffBtss.BtssScore] {
    def reads(r: BSON.Reader): OffBtss.BtssScore = OffBtss.BtssScore(
      btss = r.get[OffBtss]("btss"),
      score = r double "score"
    )
    def writes(w: BSON.Writer, b: OffBtss.BtssScore) = $doc(
      "btss" -> b.btss,
      "score" -> b.score
    )
  }
  private implicit val btssScoreArrayHandler = lila.db.dsl.bsonArrayToListHandler[OffBtss.BtssScore]

  implicit val scoreSheetHandler = new BSON[OffScoreSheet] {
    def reads(r: BSON.Reader) = OffScoreSheet(
      id = r str "_id",
      contestId = r str "contestId",
      roundNo = r int "roundNo",
      playerUid = r str "playerUid",
      playerNo = r int "playerNo",
      score = r double "score",
      rank = r int "rank",
      btssScores = r.get[List[OffBtss.BtssScore]]("btssScores")
    )

    def writes(w: BSON.Writer, s: OffScoreSheet) = $doc(
      "_id" -> s.id,
      "contestId" -> s.contestId,
      "roundNo" -> s.roundNo,
      "playerUid" -> s.playerUid,
      "playerNo" -> s.playerNo,
      "score" -> s.score,
      "rank" -> s.rank,
      "btssScores" -> btssScoreArrayHandler.write(s.btssScores)
    )
  }

  implicit val forbiddenHandler = Macros.handler[OffForbidden]
}
