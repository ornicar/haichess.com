package lila.clazz

import chess.format.Forsyth
import chess.{ Clock, Color }
import play.api.data._
import play.api.data.Forms._
import lila.common.Form._
import org.joda.time.DateTime
import lila.user.User

class HomeworkForm {

  import HomeworkForm._

  def createOf(homework: Homework) = create fill HomeworkData(
    method = "",
    deadlineAt = homework.deadlineAt,
    summary = homework.summary,
    prepare = homework.prepare,
    common = homework.common.map { c =>
      c.items.map {
        case (k, v) => HomeworkCommonItemData(k.id, v)
      }.toList
    },
    practice = homework.practice.map { p =>
      HomeworkPracticeData(
        capsules = p.capsules.map { capsule =>
          PuzzleCapsuleData(
            id = capsule.id,
            name = capsule.name,
            puzzles = capsule.puzzles.map { puzzle =>
              MiniPuzzleData(
                id = puzzle.id,
                fen = puzzle.fen,
                color = puzzle.color.name,
                lastMove = puzzle.lastMove,
                lines = puzzle.lines
              )
            }
          )
        },
        replayGames = p.replayGames.map { replayGame =>
          ReplayGameData(
            chapterLink = replayGame.chapterLink,
            name = replayGame.name,
            root = replayGame.root,
            moves = replayGame.moves
          )
        },
        recallGames = p.recallGames.map { recallGame =>
          RecallGameData(
            root = recallGame.root,
            pgn = recallGame.pgn,
            turns = recallGame.turns,
            color = recallGame.color.map(_.name),
            title = recallGame.title
          )
        },
        fromPositions = p.fromPositions.map { fromPosition =>
          FromPositionData(
            fen = fromPosition.fen,
            clockTime = fromPosition.clock.limitInMinutes,
            clockIncrement = fromPosition.clock.incrementSeconds,
            num = fromPosition.num
          )
        }
      )
    }
  )

  def create = Form(mapping(
    "method" -> nonEmptyText,
    "deadlineAt" -> optional(futureDateTime),
    "summary" -> optional(nonEmptyText),
    "prepare" -> optional(nonEmptyText),
    "common" -> optional(list(mapping(
      "item" -> stringIn(HomeworkCommon.HomeworkCommonItem.selects),
      "num" -> number(min = 1, max = 3000)
    )(HomeworkCommonItemData.apply)(HomeworkCommonItemData.unapply))),
    "practice" -> optional(mapping(
      "capsules" -> list(mapping(
        "id" -> nonEmptyText,
        "name" -> nonEmptyText,
        "puzzles" -> list(mapping(
          "id" -> number(min = 10000, max = 99999999),
          "fen" -> nonEmptyText,
          "color" -> nonEmptyText,
          "lastMove" -> optional(nonEmptyText),
          "lines" -> nonEmptyText
        )(MiniPuzzleData.apply)(MiniPuzzleData.unapply))
      )(PuzzleCapsuleData.apply)(PuzzleCapsuleData.unapply)),
      "replayGames" -> list(mapping(
        "chapterLink" -> nonEmptyText,
        "name" -> nonEmptyText,
        "root" -> nonEmptyText,
        "moves" -> list(mapping(
          "index" -> number(min = 1, max = 500),
          "white" -> optional(mapping(
            "san" -> nonEmptyText,
            "uci" -> nonEmptyText,
            "fen" -> nonEmptyText
          )(Node.apply)(Node.unapply)),
          "black" -> optional(mapping(
            "san" -> nonEmptyText,
            "uci" -> nonEmptyText,
            "fen" -> nonEmptyText
          )(Node.apply)(Node.unapply))
        )(Move.apply)(Move.unapply))
      )(ReplayGameData.apply)(ReplayGameData.unapply)),
      "recallGames" -> list(mapping(
        "root" -> nonEmptyText,
        "pgn" -> nonEmptyText,
        "turns" -> optional(number(min = 1, max = 500)),
        "color" -> optional(nonEmptyText),
        "title" -> optional(nonEmptyText)
      )(RecallGameData.apply)(RecallGameData.unapply)),
      "fromPositions" -> list(mapping(
        "fen" -> nonEmptyText,
        "clockTime" -> numberInDouble(clockTimeChoices),
        "clockIncrement" -> numberIn(clockIncrementChoices),
        "num" -> number(min = 1, max = 100)
      )(FromPositionData.apply)(FromPositionData.unapply))
    )(HomeworkPracticeData.apply)(HomeworkPracticeData.unapply))
  )(HomeworkData.apply)(HomeworkData.unapply).verifying("课后练不能为空", _.hasContent))
}

object HomeworkForm {

  val clockTimes: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ (2d to 7d by 1d) ++ (10d to 30d by 5d) ++ (40d to 60d by 10d)

  private def formatLimit(l: Double) =
    chess.Clock.Config(l * 60 toInt, 0).limitString + {
      if (l <= 1) " 分钟" else " 分钟"
    }

  val clockTimeChoices = optionsDouble(clockTimes, formatLimit)
  val clockIncrements = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val clockIncrementChoices = options(clockIncrements, "%d 秒")
}

case class HomeworkData(
    method: String,
    deadlineAt: Option[DateTime],
    summary: Option[String],
    prepare: Option[String],
    common: Option[List[HomeworkCommonItemData]],
    practice: Option[HomeworkPracticeData]
) {

  def hasContent = summary.nonEmpty || prepare.nonEmpty || common.??(_.nonEmpty) || practice.??(_.hasContent)

  def clockConfig(clockTime: Double, clockIncrement: Int) = chess.Clock.Config((clockTime * 60).toInt, clockIncrement)

  def toHomework(course: Course, user: User.ID) = Homework.make(
    clazzId = course.clazz,
    courseId = course.id,
    index = course.index,
    deadlineAt = deadlineAt,
    summary = summary,
    prepare = prepare,
    common = common.map { itemDatas =>
      HomeworkCommon(
        items = itemDatas.map { d =>
          HomeworkCommon.HomeworkCommonItem(d.item) -> d.num
        }.toMap
      )
    },
    practice = practice.map { p =>
      HomeworkPractice(
        capsules = p.capsules.map { c =>
          PuzzleCapsule(
            id = c.id,
            name = c.name,
            puzzles = c.puzzles.map { puzzle =>
              MiniPuzzle(
                id = puzzle.id,
                fen = puzzle.fen,
                color = Color(puzzle.color) err "error color",
                lastMove = puzzle.lastMove,
                lines = puzzle.lines
              )
            }
          )
        },
        replayGames = p.replayGames.map { r =>
          ReplayGame(
            chapterLink = r.chapterLink,
            name = r.name,
            root = r.root,
            moves = r.moves
          )
        },
        recallGames = p.recallGames.map { r =>
          RecallGame(
            root = r.root,
            pgn = r.pgn,
            turns = r.turns,
            color = r.color.map(Color(_) err "error color"),
            title = r.title
          )
        },
        fromPositions = p.fromPositions.map { f =>
          FromPosition(
            fen = Forsyth >> chess.Game(chess.variant.FromPosition.some, f.fen.some),
            clock = clockConfig(f.clockTime, f.clockIncrement),
            num = f.num
          )
        }
      )
    },
    students = StudentSettings(settings = Map.empty[String, StudentSetting]),
    user = user
  )

}

case class HomeworkCommonItemData(item: String, num: Int)

case class HomeworkPracticeData(
    capsules: List[PuzzleCapsuleData],
    replayGames: List[ReplayGameData],
    recallGames: List[RecallGameData],
    fromPositions: List[FromPositionData]
) {
  def hasContent = capsules.nonEmpty || replayGames.nonEmpty || recallGames.nonEmpty || fromPositions.nonEmpty
}

case class PuzzleCapsuleData(id: String, name: String, puzzles: List[MiniPuzzleData])

case class MiniPuzzleData(id: Int, fen: String, color: String, lastMove: Option[String], lines: String)

case class ReplayGameData(chapterLink: String, name: String, root: String, moves: List[Move])

case class RecallGameData(root: String, pgn: String, turns: Option[Int], color: Option[String], title: Option[String])

case class FromPositionData(fen: String, clockTime: Double, clockIncrement: Int, num: Int)
