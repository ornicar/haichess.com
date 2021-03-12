package views.html.resource.capsule

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.resource.Capsule
import play.api.data.Form
import controllers.routes

object list {

  def apply(form: Form[_], capsules: List[Capsule], tags: Set[String])(implicit ctx: Context) = views.html.base.layout(
    title = "战术题列表",
    moreJs = frag(
      infiniteScrollTag,
      jsTag("capsule-list.js")
    ),
    moreCss = cssTag("capsule")
  ) {
      main(cls := "page-menu")(
        st.aside(cls := "page-menu__menu subnav")(
          views.html.resource.puzzle.menuLinks("capsule")
        ),
        div(cls := "box")(
          st.form(
            cls := "search_form",
            action := s"${routes.Capsule.list()}#results",
            method := "GET"
          )(
              table(
                !tags.isEmpty option tr(
                  td(colspan := 6)(
                    form3.tags(form, "tags", tags)
                  )
                ),
                tr(
                  th(label("状态：")),
                  td(
                    form3.select(form("enabled"), lila.resource.DataForm.capsule.enabledSelect, "".some)
                  ),
                  th(label("名称：")),
                  td(
                    form3.input(form("name"))
                  ),
                  th(
                    submitButton(cls := "button")("搜索")
                  ),
                  th(
                    a(cls := "button button-green", style := "display:inline-block", dataIcon := "O", href := routes.Capsule.createForm)
                  )
                )
              )
            ),
          table(cls := "slist")(
            thead(
              tr(
                th("名称"),
                th("题目数量"),
                th("状态"),
                th("更新时间"),
                th("操作")
              )
            ),
            if (capsules.size > 0) {
              tbody(
                capsules.map { capsule =>
                  tr(
                    td(capsule.name),
                    td(capsule.total),
                    td(capsule.status),
                    td(momentFromNow(capsule.updatedAt)),
                    td(style := "display:flex;")(
                      !capsule.hasPuzzle option button(cls := "button button-empty disabled")("做题"),
                      capsule.hasPuzzle option a(cls := "button button-empty", href := routes.Puzzle.capsulePuzzle(capsule.id))("做题"),
                      a(cls := "button button-empty", href := routes.Capsule.updateForm(capsule.id))("修改"),
                      a(cls := "button button-empty", href := routes.Resource.puzzleCapsule(capsule.id))("查看"),
                      postForm(action := routes.Capsule.remove(capsule.id))(
                        submitButton(cls := "button button-empty button-red confirm")("删除")
                      ),
                      postForm(action := routes.Capsule.enable(capsule.id))(
                        capsule.enabled option button(cls := "button button-empty button-red confirm", name := "enabled", value := 0)("锁定"),
                        !capsule.enabled option button(cls := "button button-empty button-green confirm", name := "enabled", value := 1)("解锁")
                      )
                    )
                  )
                }
              )
            } else {
              tbody(
                tr(
                  td(colspan := 5)("暂无记录")
                )
              )
            }
          )
        )
      )
    }

}
