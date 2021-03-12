package views.html
package practice

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object index {

  def apply(data: lila.practice.UserPractice)(implicit ctx: Context) = views.html.base.layout(
    title = "国际象棋练习",
    moreCss = cssTag("practice.index"),
    moreJs = embedJsUnsafe(s"""$$('.do-reset').on('click', function() {
if (confirm('您将丢失您的练习进度')) this.parentNode.submit();
});"""),
    openGraph = lila.app.ui.OpenGraph(
      title = "国际象棋练习",
      description = "学习如何掌握最常见的棋位",
      url = s"$netBaseUrl${routes.Practice.index}"
    ).some
  ) {
      main(cls := "page-menu")(
        st.aside(cls := "page-menu__menu practice-side")(
          i(cls := "fat"),
          h1("练习"),
          h2("使您的棋艺更完美"),
          div(cls := "progress")(
            div(cls := "text")("进度: ", data.progressPercent, "%"),
            div(cls := "bar", style := s"width: ${data.progressPercent}%")
          ),
          postForm(action := routes.Practice.reset)(
            if (ctx.isAuth) (data.nbDoneChapters > 0) option a(cls := "do-reset")("重置进度")
            else a(href := routes.Auth.signup)("登录保存进度")
          )
        ),
        div(cls := "page-menu__content practice-app")(
          data.structure.sections.map { section =>
            st.section(
              h2(section.name),
              div(cls := "studies")(
                section.studies.map { stud =>
                  val prog = data.progressOn(stud.id)
                  a(
                    cls := s"study ${if (prog.complete) "done" else "ongoing"}",
                    href := routes.Practice.show(section.id, stud.slug, stud.id.value)
                  )(
                      ctx.isAuth option span(cls := "ribbon-wrapper")(
                        span(cls := "ribbon")(prog.done, " / ", prog.total)
                      ),
                      i(cls := s"${stud.id}"),
                      span(cls := "text")(
                        h3(stud.name),
                        em(stud.desc)
                      )
                    )
                }
              )
            )
          }
        )
      )
    }
}
