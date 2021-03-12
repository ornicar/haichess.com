package lila.contest

import lila.user.User
import reactivemongo.bson._
import lila.db.dsl._
import lila.game.Game
import scala.collection.breakOut

object PlayerRepo {

  private[contest] lazy val coll = Env.current.playerColl
  import BSONHandlers._

  type ID = String

  def byId(id: Player.ID): Fu[Option[Player]] = coll.byId[Player](id)

  def byIds(ids: List[Player.ID]): Fu[List[Player]] = coll.byIds[Player](ids)

  def byOrderedIds(ids: List[Player.ID]): Fu[List[Player]] =
    coll.byOrderedIds[Player, Player.ID](ids)(_.id)

  def find(contestId: Contest.ID, userId: User.ID): Fu[Option[Player]] =
    byId(Player.makeId(contestId, userId))

  def insert(player: Player): Funit = coll.insert(player).void

  def countByContest(contestId: Contest.ID): Fu[Int] =
    coll.countSel(contestQuery(contestId))

  def getByUserId(userId: User.ID): Fu[List[Player]] =
    coll.find($doc("userId" -> userId)).list[Player]()

  def getByContest(contestId: Contest.ID): Fu[List[Player]] =
    coll.find(contestQuery(contestId)).sort($sort asc "no").list[Player]()

  def remove(id: Player.ID): Funit = coll.remove($id(id)).void

  def kick(id: Player.ID, isOverPairing: Boolean): Funit =
    coll.update(
      $id(id),
      $set(
        "absent" -> true,
        "kick" -> true
      ) ++ (if (!isOverPairing) { $push("outcomes" -> Board.Outcome.Kick.id) } else $empty)
    ).void

  def quit(contestId: Contest.ID, userId: User.ID, isOverPairing: Boolean) =
    coll.update(
      $id(Player.makeId(contestId, userId)),
      $set(
        "absent" -> true,
        "quit" -> true
      ) ++ (if (!isOverPairing) { $push("outcomes" -> Board.Outcome.Quit.id) } else $empty)
    ).void

  def unAbsentByContest(contestId: Contest.ID): Funit =
    coll.update(
      $doc("contestId" -> contestId, "manualAbsent" -> true, "leave" -> false, "quit" -> false, "kick" -> false),
      $set(
        "absent" -> false,
        "manualAbsent" -> false
      ),
      multi = true
    ).void >>
      coll.update(
        $doc("contestId" -> contestId, "manualAbsent" -> true, $or("leave" $eq true, "quit" $eq true, "kick" $eq true)),
        $set(
          "manualAbsent" -> false
        ),
        multi = true
      ).void

  def findNextNo(contestId: Contest.ID): Fu[Int] = {
    coll.find(contestQuery(contestId), $doc("_id" -> false, "no" -> true))
      .sort($sort desc "no")
      .uno[Bdoc] map {
        _ flatMap { doc => doc.getAs[Int]("no") map (1 + _) } getOrElse 1
      }
  }

  def rounds(contest: Contest): Fu[List[(Player, Map[Round.No, Board])]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregateList(
      Match($doc("contestId" -> contest.id)),
      List(
        PipelineOperator(
          $doc(
            "$lookup" -> $doc(
              "from" -> Env.current.settings.CollectionBoard,
              "let" -> $doc("pno" -> "$no"),
              "pipeline" -> $arr(
                $doc(
                  "$match" -> $doc(
                    "$expr" -> $doc(
                      "$and" -> $arr(
                        $doc("$eq" -> $arr("$contestId", contest.id)),
                        $doc("$or" -> $arr(
                          $doc("$eq" -> $arr("$$pno", "$whitePlayer.no")),
                          $doc("$eq" -> $arr("$$pno", "$blackPlayer.no"))
                        ))
                      )
                    )
                  )
                )
              ),
              "as" -> "boards"
            )
          )
        )
      ),
      maxDocs = 1000
    ).map {
        _.flatMap { doc =>
          val result = for {
            player <- playerHandler.readOpt(doc)
            boards <- doc.getAs[List[Board]]("boards")
            boardMap = boards.map { b =>
              b.roundNo -> b
            }.toMap
          } yield (player, boardMap)
          result
        }(breakOut).toList
      }
  }

  def setOutcomes(contestId: Contest.ID, players: List[Player.No], outcome: Board.Outcome, score: Double): Funit =
    coll.update(
      contestQuery(contestId) ++ $doc("no" -> $in(players: _*)),
      $inc(
        "score" -> score,
        "points" -> score
      ) ++ $push("outcomes" -> outcome.id),
      multi = true
    ).void

  def setNo(id: Player.ID, no: Player.No): Funit = coll.updateField($id(id), "no", no).void

  def finishGame(contest: Contest, playerUserId: User.ID, game: Game): Funit = {
    val id = Player.makeId(contest.id, playerUserId)
    byId(id) flatMap {
      case None => fufail(s"can not find player ${id}")
      case Some(p) => {
        coll.update(
          $id(id),
          p.finish(game, contest.canQuitNumber)
        ).void
      }
    }
  }

  def update(player: Player): Funit = {
    coll.update(
      $id(player.id),
      player
    ).void
  }

  def setCancelScore(id: Player.ID): Funit =
    coll.update(
      $id(id),
      $set(
        "cancelled" -> true
      )
    ).void

  def contestQuery(contestId: Contest.ID) = $doc("contestId" -> contestId)

}
