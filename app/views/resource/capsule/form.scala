package views.html.resource.capsule

import play.api.data.Form
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.resource.Capsule
import controllers.routes

object form {

  def create(form: Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = "创建战术题列表",
      moreCss = frag(
        cssTag("capsule")
      ),
      moreJs = frag(
        tagsinputTag,
        jsTag("capsule-form.js")
      )
    ) {
        main(cls := "page-menu")(
          st.aside(cls := "page-menu__menu subnav")(
            views.html.resource.puzzle.menuLinks("capsule")
          ),
          div(cls := "box box-pad")(
            h1("创建战术题列表"),
            postForm(cls := "form3", action := routes.Capsule.create())(
              fm(form)
            )
          )
        )
      }

  def update(form: Form[_], capsule: Capsule)(implicit ctx: Context) =
    views.html.base.layout(
      title = "修改战术题列表",
      moreCss = frag(
        cssTag("capsule")
      ),
      moreJs = frag(
        tagsinputTag,
        jsTag("capsule-form.js")
      )
    ) {
        main(cls := "page-menu")(
          st.aside(cls := "page-menu__menu subnav")(
            views.html.resource.puzzle.menuLinks("capsule")
          ),
          div(cls := "box box-pad")(
            h1("修改战术题列表"),
            postForm(cls := "form3", action := routes.Capsule.update(capsule.id))(
              fm(form)
            )
          )
        )
      }

  def fm(form: Form[_])(implicit ctx: Context) = frag(
    form3.group(form("name"), "名称")(form3.input(_)),
    form3.group(form("tags"), "标签")(form3.input(_)),
    form3.group(form("desc"), "描述")(form3.textarea(_)()),
    form3.actions(
      a(cls := "cancel", href := routes.Capsule.list())("取消"),
      submitButton(cls := "button text", dataIcon := "E")("提交")
    )
  )

}
