package views.html.study

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object clone {

  def apply(s: lila.study.Study)(implicit ctx: Context) =
    views.html.site.message(
      title = s"复制 ${s.name}",
      icon = Some("4"),
      back = false
    ) {
        postForm(action := routes.Study.cloneApply(s.id.value))(
          p("这将使用相同章节创建一个私有研习。"),
          p("您将成为研习的所有者。"),
          p("新创建的研习与原有研习都可以独立编辑，"),
          p("删除其中任何一个 ", strong("不会"), " 影响到另一个。"),
          p(
            submitButton(cls := "submit button large text", dataIcon := "4",
              style := "margin: 30px auto; display: block; font-size: 2em;")("复制研习")
          ),
          p(
            a(href := routes.Study.show(s.id.value), cls := "text", dataIcon := "I")(trans.cancel())
          )
        )
      }
}
