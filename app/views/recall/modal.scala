package views.html.recall

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import play.api.data.Form
import lila.recall.{ DataForm, Recall }
import org.joda.time.DateTime
import controllers.routes

object modal {

  def createForm(form: Form[_])(implicit ctx: Context) =
    div(cls := "modal-content recall-create none")(
      h2("新建记谱"),
      postForm(cls := "form3")(
        form3.group(form("name"), raw("名称"))(form3.input2(_, s"记谱 ${DateTime.now.toString("yyyy.MM.dd HH:mm")}".some)),
        form3.split(
          form3.group(form("color"), raw("棋色"), half = true)(form3.select(_, DataForm.colorChoices)),
          form3.group(form("turns"), raw("回合数"), half = true)(form3.input(_, typ = "number")(placeholder := "不填写表示所有回合"))
        ),
        div(cls := "tabs-horiz")(
          span(cls := "pgn active")("PGN"),
          span(cls := "chapter")("研习章节链接"),
          span(cls := "game")("对局链接")
        ),
        div(cls := "tabs-content")(
          div(cls := "pgn active")(
            form3.textarea(form("pgn"))(rows := 5, placeholder := "粘贴PGN文本"),
            form3.group(form("pgnFile"), raw("上传PGN文件"), klass = "upload") { f =>
              form3.file.pgn(f.name)
            }
          ),
          div(cls := "chapter")(
            form3.input(form("chapter"))(placeholder := "章节URL"),
            div("例：https://haichess.com/study/eKF8yUkP/mJyWJTu6")
          ),
          div(cls := "game")(
            form3.input(form("game"))(placeholder := "对局URL"),
            div("例：https://haichess.com/XTtL9RRY"),
            div("或：https://haichess.com/XTtL9RRY/white")
          )
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交")
        )
      )
    )

  def editForm(recall: Recall, goTo: String, form: Form[_])(implicit ctx: Context) =
    div(cls := "modal-content recall-edit none")(
      h2("编辑记谱"),
      postForm(cls := "form3", action := routes.Recall.update(recall.id, goTo))(
        form3.group(form("name"), raw("名称"))(form3.input(_)),
        form3.split(
          form3.group(form("color"), raw("棋色"), half = true)(form3.select(_, DataForm.colorChoices)),
          form3.group(form("turns"), raw("回合数"), half = true)(form3.input(_, typ = "number")(placeholder := "不填写表示所有回合"))
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交")
        )
      )
    )
}
