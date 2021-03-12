package views.html.clazz.homework

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.resource.Capsule
import lila.clazz.HomeworkCommon.HomeworkCommonItem
import play.api.data.Form
import controllers.routes

object modal {

  val dataAttr = attr("data-attr")

  def itemModal(ids: List[String])(implicit ctx: Context) = frag(
    div(cls := "modal-content modal-item none")(
      h2("公共目标"),
      postForm(cls := "form3")(
        div(cls := "items")(
          ul(
            HomeworkCommonItem.all.filter(_.number) map { item =>
              li(cls := "item")(
                input(tpe := "checkbox", id := item.id, ids.contains(item.id) option checked, dataAttr := s"""{"id":"${item.id}","name":"${item.name}","isNumber":${item.number}}"""),
                nbsp,
                label(`for` := item.id)(item.name)
              )
            }
          ),
          ul(
            HomeworkCommonItem.all.filterNot(_.number) map { item =>
              li(cls := "item")(
                input(tpe := "checkbox", id := item.id, ids.contains(item.id) option checked, dataAttr := s"""{"id":"${item.id}","name":"${item.name}","isNumber":${item.number}}"""),
                nbsp,
                label(`for` := item.id)(item.name)
              )
            }
          )
        ),
        p(cls := "is-gold", dataIcon := "")("最多选择4项"),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("确认")
        )
      )
    )
  )

  def replayGameModal(implicit ctx: Context) = frag(
    div(cls := "modal-content modal-replay none")(
      h2("打谱"),
      postForm(cls := "form3")(
        h3("研习章节链接", nbsp, a(target := "_blank", href := routes.Study.allDefault(1))("打开")),
        input(id := "chapterLink"),
        div(cls := "chapterLinkExample")(
          h3("获取地点"),
          img(src := staticUrl("images/chapterLink.jpg"))
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("确认")
        )
      )
    )
  )

  def capsuleModal(capsules: List[Capsule])(implicit ctx: Context) = frag(
    div(cls := "modal-content modal-capsule none")(
      h2("选择战术题列表"),
      postForm(cls := "form3")(
        div(cls := "capsule-filter")(
          input(tpe := "text", cls := "capsule-filter-search", placeholder := "搜索")
        ),
        div(cls := "capsule-scroll")(
          capsules.isEmpty option div(cls := "no-more")(
            "您还没有创建战术题列表，现在就去", nbsp, a(target := "_blank", href := routes.Capsule.create())("创建"), nbsp, "吧"
          ),
          !capsules.isEmpty option table(cls := "capsule-list")(
            capsules.map { capsule =>
              tr(
                td(
                  input(tpe := "checkbox", id := capsule.id, value := capsule.id),
                  nbsp,
                  label(`for` := capsule.id)(capsule.name)
                ),
                td(capsule.total)
              )
            }
          )
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("确认")
        )
      )
    )
  )

  def recallForm(form: Form[_])(implicit ctx: Context) =
    div(cls := "modal-content modal-recall none")(
      h2("记谱"),
      postForm(cls := "form3")(
        form3.hidden(form("name"), value = "nil".some),
        form3.split(
          form3.group(form("color"), raw("棋色"), half = true)(form3.select(_, lila.recall.DataForm.colorChoices)),
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

}
