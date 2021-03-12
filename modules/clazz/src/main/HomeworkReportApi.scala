package lila.clazz

import lila.db.dsl._
import org.joda.time.DateTime
import lila.user.User

final class HomeworkReportApi(coll: Coll, clazzApi: ClazzApi, homeworkApi: HomeworkApi, stuApi: HomeworkStudentApi) {

  import BSONHandlers.HomeworkReportHandler

  def byId(id: String): Fu[Option[HomeworkReport]] = coll.byId[HomeworkReport](id)

  def schedulerRefresh: Funit = homeworkApi.deadlines.map { homeworks =>
    homeworks.foreach { homework =>
      clazzApi.byId(homework.clazzId).map {
        case None => funit
        case Some(clazz) => refreshReport(homework, clazz)
      }
    }
  }

  def refreshReport(homework: Homework, clazz: Clazz): Funit =
    stuApi.findByCourse(homework.clazzId, homework.courseId) flatMap { studentHomeworks =>
      val practice = homework.practiceWithEmpty
      val report = HomeworkReport(
        _id = homework.id,
        num = studentHomeworks.size,
        common = calcCommon(studentHomeworks),
        practice = HomeworkPracticeReport(
          puzzles = practice.puzzles.map { puzzle =>
            val completeRate = calcPuzzleTryRate(studentHomeworks, puzzle)
            val rightRate = calcPuzzleRightRate(studentHomeworks, puzzle)
            val firstMoveRightRate = calcPuzzleFirstMoveRightRate(studentHomeworks, puzzle)
            val rightMoveDistribute = calcRightMoveDistribute(studentHomeworks, puzzle)
            val wrongMoveDistribute = calcWrongMoveDistribute(studentHomeworks, puzzle)
            MiniPuzzleWithReport(
              puzzle = puzzle,
              report = PuzzleReport(
                completeRate = completeRate,
                rightRate = rightRate,
                firstMoveRightRate = firstMoveRightRate,
                rightMoveDistribute = rightMoveDistribute,
                wrongMoveDistribute = wrongMoveDistribute
              )
            )
          },
          replayGames = practice.replayGames.map { replayGame =>
            ReplayGameWithReport(
              replayGame = replayGame,
              report = ReplayGameReport(
                complete = studentHomeworks.count(_.practiceWithEmpty.findReplayGame(replayGame).isComplete)
              )
            )
          },
          recallGames = practice.recallGames.map { recallGame =>
            RecallGameWithReport(
              recallGame = recallGame,
              report = calcRecallDistribute(studentHomeworks, recallGame)
            )
          },
          fromPositions = practice.fromPositions.map { fromPosition =>
            FromPositionWithReport(
              fromPosition = fromPosition,
              report = calcFromPositionsDistribute(studentHomeworks, fromPosition)
            )
          }
        ),
        updateAt = DateTime.now,
        createAt = DateTime.now,
        createBy = homework.createdBy
      )

      coll.update(
        $id(homework.id),
        report,
        upsert = true
      ).void
    }

  private def calcCommon(studentHomeworks: List[HomeworkStudent]): Map[User.ID, List[HomeworkCommonWithResult]] = {
    studentHomeworks.map { studentHomework =>
      studentHomework.studentId -> studentHomework.common
    }.toMap
  }

  // 完成率
  private def calcPuzzleTryRate(studentHomeworks: List[HomeworkStudent], puzzle: MiniPuzzle): Double = {
    val tryCount = studentHomeworks.count(_.practiceWithEmpty.findPuzzle(puzzle).isTry)
    round2(tryCount, studentHomeworks.size)
  }

  // 正确率
  private def calcPuzzleRightRate(studentHomeworks: List[HomeworkStudent], puzzle: MiniPuzzle): Double = {
    val tryCount = studentHomeworks.count(_.practiceWithEmpty.findPuzzle(puzzle).isTry)
    val rightCount = studentHomeworks.count(_.practiceWithEmpty.findPuzzle(puzzle).isComplete)
    if (tryCount == 0) 0
    else round2(rightCount, tryCount)

  }

  // 首次正确率
  private def calcPuzzleFirstMoveRightRate(studentHomeworks: List[HomeworkStudent], puzzle: MiniPuzzle): Double = {
    val tryCount = studentHomeworks.count(_.practiceWithEmpty.findPuzzle(puzzle).isTry)
    val firstRightCount = studentHomeworks.count(_.practiceWithEmpty.findPuzzle(puzzle).isFirstComplete)
    if (tryCount == 0) 0
    else round2(firstRightCount, tryCount)
  }

  // 第一步 正确UCI分布
  private def calcRightMoveDistribute(studentHomeworks: List[HomeworkStudent], puzzle: MiniPuzzle): List[MoveNum] = {
    val firstRightMoveSet = puzzle.firstRightMoveSet
    studentHomeworks.map { studentHomework =>
      val pwr = studentHomework.practiceWithEmpty.findPuzzle(puzzle)
      pwr.firstMoveSan.replace("+", "").replace("#", "") -> firstRightMoveSet.contains(pwr.firstMoveUci)
    }.filter(_._2)
      .map(_._1)
      .groupBy(m => m)
      .map {
        case (m, list) => MoveNum(m, list.size)
      }.toList
  }

  // 第一步 错误UCI分布
  private def calcWrongMoveDistribute(studentHomeworks: List[HomeworkStudent], puzzle: MiniPuzzle): List[MoveNum] = {
    val firstRightMoveSet = puzzle.firstRightMoveSet
    studentHomeworks.map { studentHomework =>
      val firstMove = studentHomework.practiceWithEmpty.findPuzzle(puzzle).firstMoveUci
      val pwr = studentHomework.practiceWithEmpty.findPuzzle(puzzle)
      pwr.firstMoveSan.replace("+", "").replace("#", "") -> firstRightMoveSet.contains(pwr.firstMoveUci)
    }.filterNot(_._2)
      .map(_._1)
      .filterNot(_ == "")
      .groupBy(m => m)
      .map {
        case (m, list) => MoveNum(m, list.size)
      }.toList
  }

  private def calcRecallDistribute(studentHomeworks: List[HomeworkStudent], recallGame: RecallGame): List[RecallGameReport] = {
    studentHomeworks
      .map { _.practiceWithEmpty.findRecallGame(recallGame).turns }
      .groupBy(c => c)
      .map {
        case (m, list) => RecallGameReport(m, list.size)
      }.toList
  }.sortBy(_.turns)

  private def calcFromPositionsDistribute(studentHomeworks: List[HomeworkStudent], fromPositions: FromPosition): List[FromPositionReport] = {
    studentHomeworks
      .map { _.practiceWithEmpty.findFromPosition(fromPositions).completeSize }
      .groupBy(c => c)
      .map {
        case (m, list) => FromPositionReport(m, list.size)
      }.toList
  }.sortBy(_.rounds)

  private def round2(divided: Int, divisor: Int) = Math.round(divided * 1000 / divisor) / 10.0

}
