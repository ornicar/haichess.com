package lila.recall

import lila.common.Form.stringIn
import lila.study.{ Chapter, Study, StudyApi }
import lila.user.User
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints
import play.api.libs.ws.WS

import scala.util.matching.Regex
import play.api.Play.current

case class DataForm(studyApi: StudyApi) {

  import DataForm._

  def create(user: User) = Form(mapping(
    "name" -> nonEmptyText(minLength = 2, maxLength = 50),
    "color" -> stringIn(colorChoices),
    "turns" -> optional(number(1, 500)),
    "chapter" -> optional(
      nonEmptyText.verifying(Constraints.pattern(regex = chapterRegex, error = "章节URL格式错误"))
        .verifying("您无权访问这个研习", url => chapterTest(url, user).awaitSeconds(3))
    ),
    "game" -> optional(
      nonEmptyText.verifying(Constraints.pattern(regex = gameRegex, error = "对局URL格式错误"))
        .verifying("对局URL无法识别", url => urlTest(url).awaitSeconds(3))
    ),
    "pgn" -> optional(nonEmptyText)
  )(RecallData.apply)(RecallData.unapply).verifying("输入一种PGN获取方式", !_.allEmpty))

  def edit = Form(mapping(
    "name" -> nonEmptyText(minLength = 2, maxLength = 50),
    "color" -> stringIn(colorChoices),
    "turns" -> optional(number(1, 500))
  )(RecallEdit.apply)(RecallEdit.unapply))

  def editOf(recall: Recall) = edit.fill(RecallEdit(
    name = recall.name,
    color = recall.color.map(_.name) | "all",
    turns = recall.turns
  ))

  def urlTest(url: String): Fu[Boolean] = WS.url(url).get().map(r => r.status == 200)

  def chapterTest(url: String, user: User): Fu[Boolean] = {
    studyApi.byId(Study.Id(studyId(url))).map {
      _.?? { study =>
        study.isPublic || study.members.contains(user.id)
      }
    }
  }

}

object DataForm {

  private val ProdGameRegex = """https://haichess\.com/(\w{8})(\w{4})?/?(white|black)?""".r
  private val DevGameRegex = """http://localhost/(\w{8})(\w{4})?/?(white|black)?""".r
  private val ProdChapterRegex = """https://haichess.com/study/(\w{8})/(\w{8})""".r
  private val DevChapterRegex = """http://localhost/study/(\w{8})/(\w{8})""".r

  def finish = Form(tuple("win" -> boolean, "turns" -> number(0, 500)))

  def colorChoices = List("all" -> "全部", "white" -> "白棋", "black" -> "黑棋")

  def isProd = lila.common.PlayApp.isProd

  def gameRegex = if (isProd) ProdGameRegex else DevGameRegex

  def chapterRegex = if (isProd) ProdChapterRegex else DevChapterRegex

  def gameId(url: String) = group(gameRegex, url, 1)

  def studyId(url: String) = group(chapterRegex, url, 1)

  def chapterId(url: String) = group(chapterRegex, url, 2)

  def group(r: Regex, s: String, g: Int): String = {
    val m = r.pattern.matcher(s)
    if (m.find) {
      m.group(g).some
    } else None
  } err s"can not find regex group of $s"

}

case class RecallData(
    name: String,
    color: String,
    turns: Option[Int],
    chapter: Option[String],
    game: Option[String],
    pgn: Option[String]
) {

  def allEmpty = chapter.isEmpty && game.isEmpty && pgn.isEmpty

  def gameId(url: String) = DataForm.gameId(url)

  def studyId(url: String) = DataForm.studyId(url)

  def chapterId(url: String) = DataForm.chapterId(url)

}

case class RecallEdit(name: String, color: String, turns: Option[Int])
