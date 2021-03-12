package lila.clazz

import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime
import lila.puzzle.Puzzle.UserResult
import lila.game.{ Game, GameRepo }
import lila.puzzle.PuzzleRush

import scala.concurrent.duration._
import lila.clazz.HomeworkCommon.HomeworkCommonItemSource
import lila.hub.actorApi.Recall

final class HomeworkSolve(
    coll: Coll,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import BSONHandlers.HomeworkStudentHandler

  private[clazz] val mineAvailableCache = asyncCache.multi[String, List[HomeworkStudent]](
    name = "homework.solve",
    f = mineAvailable,
    expireAfter = _.ExpireAfterWrite(7 days)
  )

  def mineAvailable(userId: User.ID): Fu[List[HomeworkStudent]] =
    coll.find(
      $doc(
        "studentId" -> userId,
        "deadlineAt" $gt DateTime.now
      )
    )
      .sort($sort desc "createdAt")
      .list[HomeworkStudent]()

  def handleCreate(create: StudentHomeworkCreate) = create.homeworks.foreach { homework =>
    mineAvailableCache.get(homework.studentId).map { homeworks =>
      mineAvailableCache.put(homework.studentId, homeworks.filter(_.available) :+ homework)
    }
  }

  def handlePuzzle(res: UserResult): Funit =
    mineAvailableCache.get(res.userId).flatMap { homeworks =>
      var updateHomework = List.empty[String]
      val list = homeworks.filter(_.available).map { h =>
        var homework = h
        if (homework.isCommonContainsPuzzle) {
          homework = homework.copy(
            common = homework.common.map { com =>
              if (com.item.source == HomeworkCommonItemSource.PuzzleItem &&
                !homework.practiceWithEmpty.puzzles.exists(_.puzzle.id == res.puzzleId)) {
                updateHomework = updateHomework :+ homework.id
                com.finishPuzzle(res)
              } else com
            }
          )
        }

        if (homework.isPracticeContainsPuzzle) {
          homework = homework.copy(
            practice = homework.practice.map { prac =>
              prac.copy(
                puzzles = prac.puzzles.map { p =>
                  if (p.puzzle.id == res.puzzleId) {
                    updateHomework = updateHomework :+ homework.id
                    p.finishPuzzle(res)
                  } else p
                }
              )
            }
          )
        }
        homework
      }

      list.filter(c => (c.isCommonContainsPuzzle || c.isPracticeContainsPuzzle) && updateHomework.contains(c.id)).map { homework =>
        coll.update(
          $id(homework.id),
          homework
        )
      }.sequenceFu.void addEffect { _ =>
        mineAvailableCache.put(res.userId, list)
      }
    }

  def handleGame(game: Game) =
    game.userIds.foreach { userId =>
      mineAvailableCache.get(userId).flatMap { homeworks =>
        var updateHomework = List.empty[String]
        val list = homeworks.filter(_.available).map { h =>
          var homework = h
          if (homework.isCommonContainsGame) {
            homework = homework.copy(
              common = homework.common.map { com =>
                if (com.item.source == HomeworkCommonItemSource.GameItem) {
                  updateHomework = updateHomework :+ homework.id
                  com.finishGame(game, userId)
                } else com
              }
            )
          }

          if (game.fromPosition && homework.isPracticeContainsFromPosition) {
            GameRepo.initialFen(game.id).awaitSeconds(3) match {
              case None =>
              case Some(fen) => {
                homework = homework.copy(
                  practice = homework.practice.map { prac =>
                    prac.copy(
                      fromPositions = prac.fromPositions.map { fromPosition =>
                        if (fromPosition.sameFen(fen.value) && fromPosition.sameClock(game.clock.??(_.config.show))) {
                          updateHomework = updateHomework :+ homework.id
                          fromPosition.finishFromPosition(game)
                        } else fromPosition
                      }
                    )
                  }
                )
              }
            }
          }
          homework
        }

        list.filter(c => (c.isCommonContainsGame || c.isPracticeContainsFromPosition) && updateHomework.contains(c.id)).map { homework =>
          coll.update(
            $id(homework.id),
            homework
          )
        }.sequenceFu.void addEffect { _ =>
          mineAvailableCache.put(userId, list)
        }
      }
    }

  def handleRush(rush: PuzzleRush): Funit =
    mineAvailableCache.get(rush.userId).flatMap { homeworks =>
      var updateHomework = List.empty[String]
      val list = homeworks.filter(_.available).map { homework =>
        if (homework.isCommonContainsRush) {
          homework.copy(
            common = homework.common.map { com =>
              if (com.item.source == HomeworkCommonItemSource.RushItem) {
                updateHomework = updateHomework :+ homework.id
                com.finishRush(rush)
              } else com
            }
          )
        } else homework
      }

      list.filter(c => c.isCommonContainsRush && updateHomework.contains(c.id)).map { homework =>
        coll.update(
          $id(homework.id),
          homework
        )
      }.sequenceFu.void addEffect { _ =>
        mineAvailableCache.put(rush.userId, list)
      }
    }

  def handleReplayGame(userId: User.ID, homeworkId: String, studyId: String, chapterId: String): Funit =
    mineAvailableCache.get(userId).flatMap { homeworks =>
      var updateHomework = List.empty[String]
      val list = homeworks.filter(h => h.id == homeworkId && h.available).map { homework =>
        homework.copy(
          practice = homework.practice.map { prac =>
            prac.copy(
              replayGames = prac.replayGames.map { r =>
                if (r.isContains(studyId, chapterId)) {
                  updateHomework = updateHomework :+ homework.id
                  r.finishReplayGame
                } else r
              }
            )
          }
        )
      }

      list.filter(c => c.isPracticeContainsReplayGame && updateHomework.contains(c.id)).map { homework =>
        coll.update(
          $id(homework.id),
          homework
        )
      }.sequenceFu.void addEffect { _ =>
        mineAvailableCache.put(userId, list)
      }
    }

  def handleRecall(recall: Recall): Funit =
    mineAvailableCache.get(recall.userId).flatMap { homeworks =>
      var updateHomework = List.empty[String]
      val list = homeworks.filter(_.available).map { homework =>
        if (homework.isPracticeContainsRecallGame) {
          homework.copy(
            practice = homework.practice.map { prac =>
              prac.copy(
                recallGames = prac.recallGames.map { r =>
                  if (recall.homeworkId.??(_ == r.recallGame.hashMD5)) {
                    updateHomework = updateHomework :+ homework.id
                    r.finishRecallGame(recall.win, recall.turns)
                  } else r
                }
              )
            }
          )
        } else homework
      }

      list.filter(c => c.isPracticeContainsRecallGame && updateHomework.contains(c.id)).map { homework =>
        coll.update(
          $id(homework.id),
          homework
        )
      }.sequenceFu.void addEffect { _ =>
        mineAvailableCache.put(recall.userId, list)
      }
    }

}
