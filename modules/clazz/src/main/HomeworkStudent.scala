package lila.clazz

import lila.user.User
import lila.game.Game
import org.joda.time.DateTime
import HomeworkStudent._
import HomeworkCommon.HomeworkCommonItemSource._

case class HomeworkStudent(
    _id: ID,
    clazzId: String,
    courseId: String,
    index: Int,
    studentId: User.ID,
    deadlineAt: DateTime,
    summary: Option[String],
    prepare: Option[String],
    comment: Option[String],
    common: List[HomeworkCommonWithResult],
    practice: Option[HomeworkPracticeWithResult],
    createdAt: DateTime,
    createdBy: User.ID
) {

  def id: ID = _id

  def available = deadlineAt.isAfter(DateTime.now)

  def deadline = deadlineAt.toString("yyyy-MM-dd HH:mm")

  def isCreator(userId: User.ID) = userId == createdBy

  def belongTo(userId: User.ID) = userId == studentId

  def practiceWithEmpty = practice | HomeworkPracticeWithResult.empty

  def puzzles = practiceWithEmpty.puzzles
  def replayGames = practiceWithEmpty.replayGames
  def recallGames = practiceWithEmpty.recallGames
  def fromPositions = practiceWithEmpty.fromPositions

  def isCommonContainsPuzzle = common.exists(com => com.item.source == PuzzleItem)
  def isCommonContainsRush = common.exists(com => com.item.source == RushItem)
  def isCommonContainsGame = common.exists(com => com.item.source == GameItem)

  def isPracticeContainsPuzzle = practiceWithEmpty.puzzles.nonEmpty
  def isPracticeContainsReplayGame = practiceWithEmpty.replayGames.nonEmpty
  def isPracticeContainsRecallGame = practiceWithEmpty.recallGames.nonEmpty
  def isPracticeContainsFromPosition = practiceWithEmpty.fromPositions.nonEmpty

  def count = common.size + practice.??(_.count)

  def finishCount = common.count(_.isComplete) + practice.??(_.finishCount)

  def finishRate = s"${Math.round(finishCount.toDouble / count.toDouble * 100)}%"

}

object HomeworkStudent {

  type ID = String

  def makeId(homeworkId: Homework.ID, userId: User.ID) = s"$homeworkId@$userId"

  def byHomework(homework: Homework, userId: User.ID) = HomeworkStudent(
    _id = makeId(homework.id, userId),
    clazzId = homework.clazzId,
    courseId = homework.courseId,
    index = homework.index,
    studentId = userId,
    deadlineAt = homework.deadlineAt err s"cant find deadline of ${homework.id}",
    summary = homework.summary,
    prepare = homework.prepare,
    comment = homework.students.comment(userId),
    common = homework.students.common(userId).fold(homework.common)(_.some).map { com =>
      com.items.map {
        case (item, num) => HomeworkCommonWithResult(item, num, None)
      }.toList
    } | List.empty[HomeworkCommonWithResult],
    practice = homework.practice.map { pra =>
      HomeworkPracticeWithResult(
        puzzles = pra.puzzles.map { puzzle =>
          MiniPuzzleWithResult(
            puzzle = puzzle,
            result = None
          )
        },
        replayGames = pra.replayGames.map { replayGame =>
          ReplayGameWithResult(
            replayGame,
            None
          )
        },
        recallGames = pra.recallGames.map { recallGame =>
          RecallGameWithResult(
            recallGame,
            None
          )
        },
        fromPositions = pra.fromPositions.map { fromPosition =>
          FromPositionWithResult(
            fromPosition,
            None
          )
        }
      )
    },
    createdAt = DateTime.now,
    createdBy = homework.createdBy
  )

}

case class HomeworkStudentFullInfo(homework: HomeworkStudent, clazz: Clazz, course: Course)
