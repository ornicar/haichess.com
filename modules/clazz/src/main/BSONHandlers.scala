package lila.clazz

import lila.user.User
import lila.db.BSON
import lila.db.dsl._
import reactivemongo.bson.{ BSONArray, BSONBoolean, BSONDocument, BSONElement, BSONHandler, BSONInteger, BSONString, BSONWriter, Macros }
import lila.db.dsl.bsonArrayToListHandler
import chess.Clock.{ Config => ClockConfig }
import BSON.MapDocument.MapHandler
import lila.db.BSON.BSONJodaDateTimeHandler

object BSONHandlers {

  private implicit val weekCourseHandler = Macros.handler[WeekCourse]
  private implicit val trainCourseHandler = Macros.handler[TrainCourse]
  private implicit val weekCourseArrayHandler = bsonArrayToListHandler[WeekCourse]
  private implicit val trainCourseArrayHandler = bsonArrayToListHandler[TrainCourse]
  private implicit val weekClassHandler = Macros.handler[WeekClazz]
  private implicit val trainClassHandler = Macros.handler[TrainClazz]

  implicit val StudentBSONHandler = Macros.handler[Student]
  implicit val StudentBSONWriter = new BSONWriter[Student, Bdoc] {
    def write(x: Student) = StudentBSONHandler write x
  }
  implicit val StudentsBSONHandler = new BSONHandler[Bdoc, Students] {
    private val mapHandler = MapHandler[String, Student]
    def read(b: Bdoc) = Students(mapHandler read b map {
      case (id, student) => id -> student
    })
    def write(x: Students) = BSONDocument(x.students.mapValues(StudentBSONWriter.write))
  }
  implicit val ClazzTypeBSONHandler = new BSONHandler[BSONString, Clazz.ClazzType] {
    def read(b: BSONString) = Clazz.ClazzType.byId get b.value err s"Invalid ClazzType. ${b.value}"
    def write(x: Clazz.ClazzType) = BSONString(x.id)
  }

  private implicit val stringArrayHandler = bsonArrayToListHandler[String]
  private implicit val intCourseArrayHandler = bsonArrayToListHandler[Int]

  implicit val clazzHandler = Macros.handler[Clazz]
  implicit val courseHandler = Macros.handler[Course]

  private implicit val homeworkCommonItemBSONHandler = new BSONHandler[BSONString, HomeworkCommon.HomeworkCommonItem] {
    def read(b: BSONString) = HomeworkCommon.HomeworkCommonItem.byId get b.value err s"Invalid HomeworkCommonItem. ${b.value}"
    def write(x: HomeworkCommon.HomeworkCommonItem) = BSONString(x.id)
  }

  private implicit val homeworkCommonHandler = new BSONHandler[Bdoc, HomeworkCommon] {
    def read(doc: Bdoc) =
      HomeworkCommon(
        doc.elements.map {
          case BSONElement(k, BSONInteger(v)) => HomeworkCommon.HomeworkCommonItem(k) -> v.toInt
        }.toMap
      )

    def write(x: HomeworkCommon) = BSONDocument(x.items.mapKeys(_.id).mapValues(BSONInteger(_)))
  }
  private implicit val clockConfigBSONHandler = new BSONHandler[BSONDocument, ClockConfig] {
    def read(doc: BSONDocument) = ClockConfig(
      doc.getAs[Int]("limit").get,
      doc.getAs[Int]("increment").get
    )

    def write(config: ClockConfig) = BSONDocument(
      "limit" -> config.limitSeconds,
      "increment" -> config.incrementSeconds
    )
  }
  private implicit val colorBSONHandler = new BSONHandler[BSONBoolean, chess.Color] {
    def read(b: BSONBoolean) = chess.Color(b.value)
    def write(c: chess.Color) = BSONBoolean(c.white)
  }
  implicit val HomeworkStatusBSONHandler = new BSONHandler[BSONInteger, Homework.Status] {
    def read(b: BSONInteger) = Homework.Status.byId get b.value err s"Invalid Status ${b.value}"
    def write(x: Homework.Status) = BSONInteger(x.id)
  }

  private implicit val NodeHandler = Macros.handler[Node]
  private implicit val MoveHandler = Macros.handler[Move]
  private implicit val miniPuzzlePracticeHandler = Macros.handler[MiniPuzzle]
  private implicit val miniPuzzleArrayPracticeHandler = bsonArrayToListHandler[MiniPuzzle]
  private implicit val puzzleCapsulePracticeHandler = Macros.handler[PuzzleCapsule]
  private implicit val replayGamePracticeHandler = Macros.handler[ReplayGame]
  private implicit val recallGamePracticeHandler = Macros.handler[RecallGame]
  private implicit val fromPositionPracticeHandler = Macros.handler[FromPosition]
  private implicit val homeworkPracticeHandler = Macros.handler[HomeworkPractice]
  private implicit val studentSettingHandler = Macros.handler[StudentSetting]
  private implicit val StudentSettingsBSONHandler = new BSONHandler[Bdoc, StudentSettings] {
    private val mapHandler = MapHandler[String, StudentSetting]
    def read(b: Bdoc) = StudentSettings(mapHandler read b map {
      case (id, student) => id -> student
    })
    def write(x: StudentSettings) = BSONDocument(x.settings.mapValues(studentSettingHandler.write))
  }
  implicit val homeworkHandler = Macros.handler[Homework]

  private implicit val HomeworkCommonDiffHandler = Macros.handler[HomeworkCommonDiff]
  private implicit val HomeworkCommonResultHandler = Macros.handler[HomeworkCommonResult]
  private implicit val HomeworkCommonWithResultHandler = Macros.handler[HomeworkCommonWithResult]
  private implicit val PuzzleResultHandler = Macros.handler[PuzzleResult]
  private implicit val MiniPuzzleWithResultHandler = Macros.handler[MiniPuzzleWithResult]
  private implicit val ReplayGameResultHandler = Macros.handler[ReplayGameResult]
  private implicit val ReplayGameWithResultHandler = Macros.handler[ReplayGameWithResult]
  private implicit val RecallGameResultHandler = Macros.handler[RecallGameResult]
  private implicit val RecallGameWithResultHandler = Macros.handler[RecallGameWithResult]
  private implicit val FromPositionResultHandler = Macros.handler[FromPositionResult]
  private implicit val FromPositionWithResultHandler = Macros.handler[FromPositionWithResult]
  private implicit val HomeworkPracticeWithResultHandler = Macros.handler[HomeworkPracticeWithResult]
  implicit val HomeworkStudentHandler = Macros.handler[HomeworkStudent]

  private implicit val MoveNumHandler = Macros.handler[MoveNum]
  private implicit val PuzzleReportHandler = Macros.handler[PuzzleReport]
  private implicit val MiniPuzzleWithReportHandler = Macros.handler[MiniPuzzleWithReport]
  private implicit val ReplayGameReportHandler = Macros.handler[ReplayGameReport]
  private implicit val ReplayGameWithReportHandler = Macros.handler[ReplayGameWithReport]
  private implicit val RecallGameReportHandler = Macros.handler[RecallGameReport]
  private implicit val RecallGameWithReportHandler = Macros.handler[RecallGameWithReport]
  private implicit val FromPositionReportHandler = Macros.handler[FromPositionReport]
  private implicit val FromPositionWithReportHandler = Macros.handler[FromPositionWithReport]
  private implicit val HomeworkPracticeReportHandler = Macros.handler[HomeworkPracticeReport]
  private implicit val homeworkCommonWithResultArrayHandler = bsonArrayToListHandler[HomeworkCommonWithResult]
  private implicit val homeworkCommonReportHandler = new BSONHandler[Bdoc, Map[User.ID, List[HomeworkCommonWithResult]]] {
    def read(doc: Bdoc) =
      doc.elements.map {
        case BSONElement(u, array: BSONArray) => u -> homeworkCommonWithResultArrayHandler.read(array)
      }.toMap

    def write(common: Map[User.ID, List[HomeworkCommonWithResult]]) = BSONDocument(common.mapValues(homeworkCommonWithResultArrayHandler.write(_)))
  }
  implicit val HomeworkReportHandler = Macros.handler[HomeworkReport]

}
