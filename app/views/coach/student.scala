package views.html.coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.coach.{ Student, StudentWithUser }
import controllers.routes

object student {

  def applying(list: List[Student])(implicit ctx: Context) = views.html.base.layout(
    title = "学员申请",
    moreJs = frag(
      jsTag("coach.student.js")
    ),
    moreCss = cssTag("coach")
  ) {
      main(cls := "page-menu students")(
        st.aside(cls := "page-menu__menu subnav")(
          a(cls := "applying active", href := routes.Coach.applyingStuList())("学员申请"),
          a(cls := "approved", href := routes.Coach.approvedStuList())("学员列表")
        ),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(
            h1("学员申请"),
            div(cls := "box__top__actions")
          ),
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th("账号"),
                th("申请时间"),
                th("操作")
              )
            ),
            tbody(
              if (list.isEmpty) {
                tr(cls := "empty")(
                  td(colspan := 4)("没有更多申请")
                )
              } else {
                list.map { stu =>
                  tr(
                    td(userIdLink(stu.studentId.some)),
                    td(momentFromNow(stu.createAt)),
                    td(
                      a(cls := "button button-empty button-green stu-approve", dataHref := routes.Coach.studentApprove(stu.id))("接受"),
                      a(cls := "button button-empty button-red stu-decline", dataHref := routes.Coach.studentDecline(stu.id))("拒绝")
                    )
                  )
                }
              }
            )
          )
        )
      )
    }

  def approved(list: List[StudentWithUser], q: String = "")(implicit ctx: Context) = views.html.base.layout(
    title = "学员列表",
    moreJs = frag(
      jsTag("coach.student.js")
    ),
    moreCss = cssTag("coach")
  ) {
      main(cls := "page-menu students")(
        st.aside(cls := "page-menu__menu subnav")(
          a(cls := "applying", href := routes.Coach.applyingStuList)("学员申请"),
          a(cls := "approved active", href := routes.Coach.approvedStuList())("学员列表")
        ),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(
            h1("学员列表"),
            div(cls := "box__top__actions")(
              form(cls := "search", method := "GET", action := routes.Coach.approvedStuList())(
                input(placeholder := "搜索", tpe := "text", name := "q", value := q)
              )
            )
          ),
          table(cls := "slist slist-pad")(
            thead(
              tr(
                th("账号"),
                th("姓名"),
                th("级别"),
                th("加入时间"),
                th("操作")
              )
            ),
            tbody(
              if (list.isEmpty) {
                tr(cls := "empty")(
                  td(colspan := 4)("没有更多学员")
                )
              } else {
                list.map { data =>
                  tr(
                    td(userLink(data.user)),
                    td(data.user.realNameOrUsername),
                    td(data.user.profileOrDefault.currentLevel.label),
                    td(data.student.approvedAt.map(momentFromNow(_))),
                    td(
                      a(cls := "button button-empty", href := "/inbox/new?user=" + data.student.studentId)("发消息"),
                      a(cls := "button button-empty button-red stu-remove", dataHref := routes.Coach.studentRemove(data.student.id))("移除")
                    )
                  )
                }
              }
            )
          )
        )
      )
    }

}
