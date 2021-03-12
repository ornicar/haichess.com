package lila.puzzle

import scala.concurrent.duration._
import lila.db.dsl._
import lila.user.User
import Puzzle.{ BSONFields => F }
import org.joda.time.DateTime

final class PuzzleApi(
    puzzleColl: Coll,
    roundColl: Coll,
    voteColl: Coll,
    headColl: Coll,
    taggerColl: Coll,
    puzzleIdMin: PuzzleId,
    puzzleMarkIdMin: PuzzleId,
    asyncCache: lila.memo.AsyncCache.Builder,
    apiToken: String,
    bus: lila.common.Bus
) {

  import Puzzle.puzzleBSONHandler

  val pmim = puzzleMarkIdMin

  object puzzle {

    val projection = $doc(F.taggers -> 0, F.likers -> 0)

    val notImport = F.ipt $exists false

    def find(id: PuzzleId): Fu[Option[Puzzle]] =
      puzzleColl.find($doc(F.id -> id), projection).uno[Puzzle]

    def findMany(ids: List[PuzzleId]): Fu[List[Option[Puzzle]]] =
      puzzleColl.optionsByOrderedIds[Puzzle, PuzzleId](ids)(_.id)

    def findMany2(ids: List[PuzzleId]): Fu[List[Puzzle]] =
      puzzleColl.byOrderedIds[Puzzle, PuzzleId](ids)(_.id)

    def findMany3(ids: List[PuzzleId]): Fu[List[Puzzle]] =
      puzzleColl.find(
        $inIds(ids)
      ).sort($doc(F.rating -> 1)).list()

    def latest(nb: Int): Fu[List[Puzzle]] =
      puzzleColl.find($empty, projection)
        .sort($doc(F.date -> -1))
        .cursor[Puzzle]()
        .gather[List](nb)

    val cachedLastId = asyncCache.single(
      name = "puzzle.lastId",
      f = findNextId map (_ - 1),
      expireAfter = _.ExpireAfterWrite(1 day)
    )

    def findNextId() = {
      puzzleColl.find($empty ++ notImport, $id(true))
        .sort($sort desc "_id")
        .uno[Bdoc] map {
          _ flatMap { doc => doc.getAs[Int]("_id") map (1 +) } getOrElse 1
        }
    }

    def export(nb: Int): Fu[List[Puzzle]] = List(true, false).map { mate =>
      puzzleColl.find($doc(F.mate -> mate), projection)
        .sort($doc(F.voteRatio -> -1))
        .cursor[Puzzle]().gather[List](nb / 2)
    }.sequenceFu.map(_.flatten)

    def disable(id: PuzzleId): Funit =
      puzzleColl.update(
        $id(id),
        $doc("$set" -> $doc(F.vote -> AggregateVote.disable))
      ).void

    def insert(puzzle: Puzzle): Funit = puzzleColl.insert(puzzle).void

    def incLikes(id: PuzzleId, value: Int) =
      puzzleColl.update($id(id), $inc(F.likes -> value)).void

    def incLikesByIds(ids: List[PuzzleId], value: Int) =
      puzzleColl.update($inIds(ids), $inc(F.likes -> value), multi = true).void

  }

  object round {

    def add(a: Round) = roundColl insert a

    def upsert(a: Round) = roundColl.update($id(a.id), a, upsert = true)

    def reset(user: User) = roundColl.remove($doc(
      Round.BSONFields.id $startsWith s"${user.id}:"
    ))
  }

  object vote {

    def value(id: PuzzleId, user: User): Fu[Option[Boolean]] =
      voteColl.primitiveOne[Boolean]($id(Vote.makeId(id, user.id)), "v")

    def find(id: PuzzleId, user: User): Fu[Option[Vote]] = voteColl.byId[Vote](Vote.makeId(id, user.id))

    def update(id: PuzzleId, user: User, v1: Option[Vote], v: Boolean): Fu[(Puzzle, Vote)] = puzzle find id flatMap {
      case None => fufail(s"Can't vote for non existing puzzle ${id}")
      case Some(p1) =>
        val (p2, v2) = v1 match {
          case Some(from) => (
            (p1 withVote (_.change(from.value, v))),
            from.copy(v = v)
          )
          case None => (
            (p1 withVote (_ add v)),
            Vote(Vote.makeId(id, user.id), v)
          )
        }
        voteColl.update(
          $id(v2.id),
          $set("v" -> v),
          upsert = true
        ) zip
          puzzleColl.update(
            $id(p2.id),
            $set(F.vote -> p2.vote)
          ) map {
              case _ => p2 -> v2
            }
    }
  }

  object head {

    def find(user: User): Fu[Option[PuzzleHead]] = headColl.byId[PuzzleHead](user.id)

    def set(h: PuzzleHead) = headColl.update($id(h.id), h, upsert = true) void

    def addNew(user: User, puzzleId: PuzzleId) = set(PuzzleHead(user.id, puzzleId.some, puzzleId))

    def currentPuzzleId(user: User): Fu[Option[PuzzleId]] =
      find(user) map2 { (h: PuzzleHead) =>
        h.current | h.last
      }

    private[puzzle] def solved(user: User, id: PuzzleId): Funit = head find user flatMap { headOption =>
      set {
        PuzzleHead(user.id, none, headOption.fold(id)(head => id atLeast head.last))
      }
    }
  }

  object tagger {

    def toggle(puzzleId: PuzzleId, userId: User.ID): Funit =
      liked(puzzleId, userId) flatMap { e =>
        (if (e) remove(puzzleId, userId) else add(puzzleId, userId, DateTime.now)) inject !e
      } flatMap { liked =>
        puzzle.incLikes(puzzleId, if (liked) 1 else -1)
      }

    def add(puzzleId: PuzzleId, userId: User.ID, date: DateTime): Funit =
      taggerColl.insert($doc(
        "_id" -> PuzzleTagger.makeId(puzzleId, userId),
        "puzzleId" -> puzzleId,
        "user" -> userId,
        "date" -> date
      )).void

    def setTags(puzzleId: PuzzleId, userId: User.ID, tags: List[String]): Funit = {
      val taggerId = PuzzleTagger.makeId(puzzleId, userId)
      taggerColl.update(
        $id(taggerId),
        $set("tags" -> tags)
      ).void
    }

    def liked(puzzleId: PuzzleId, userId: User.ID): Fu[Boolean] =
      taggerColl.exists($id(PuzzleTagger.makeId(puzzleId, userId)))

    def remove(puzzleId: PuzzleId, userId: User.ID): Funit = taggerColl.remove($id(PuzzleTagger.makeId(puzzleId, userId))).void >>-
      bus.publish(lila.hub.actorApi.resource.PuzzleResourceRemove(List(puzzleId)), 'puzzleResourceRemove)

    def removeByPuzzleIdsAndUser(puzzleIds: List[PuzzleId], userId: User.ID): Funit =
      taggerColl.remove($doc("puzzleId" $in puzzleIds, "user" -> userId)) >>
        puzzle.incLikesByIds(puzzleIds, -1).void >>-
        bus.publish(lila.hub.actorApi.resource.PuzzleResourceRemove(puzzleIds), 'puzzleResourceRemove)

    def find(puzzleId: PuzzleId, userInfos: Option[UserInfos]): Fu[Option[PuzzleTagger]] =
      userInfos.fold(fuccess(PuzzleTagger.empty(puzzleId))) { ui =>
        taggerColl.byId(PuzzleTagger.makeId(puzzleId, ui.user.id))(PuzzleTagger.taggerBSONHandler)
      }

    def puzzleIds(condition: Bdoc): Fu[List[PuzzleId]] = taggerColl.find(condition).cursor[Bdoc]()
      .gather[List]().map { _ flatMap { _.getAs[Int]("puzzleId") } }

    def tagsByUser(userId: User.ID): Fu[Set[String]] = taggerColl.distinct[String, Set]("tags", ($doc("user" -> userId)).some)
  }
}
