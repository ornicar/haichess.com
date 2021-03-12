package views.html.clazz

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.{ Clazz, CourseWithHomework, Student }
import lila.clazz.Clazz.ClazzWithCoach
import lila.user.User
import lila.team.Team
import controllers.routes

object detail {

  private val dataTab = attr("data-tab")

  def apply(
    clazzWithCoach: ClazzWithCoach,
    users: List[User],
    team: Option[Team],
    courseHomeworks: List[CourseWithHomework],
    error: Option[String] = None
  )(implicit ctx: Context) = {
    val clazz = clazzWithCoach.clazz
    views.html.base.layout(
      title = "班级详情",
      moreJs = jsTag("clazz.detail.js"),
      moreCss = cssTag("clazz")
    ) {
        val list =
          if (ctx.me.get.id == clazz.coach) {
            clazz.studentList
          } else {
            clazz.joinedStudentList
          }
        main(cls := "box box-pad page-small detail")(
          bits.clazzInfo(clazzWithCoach, team),
          div(cls := "tabs relations")(
            div(cls := "header")(
              div(dataTab := "students", cls := "active")("学员"),
              div(dataTab := "homeworks")("课后练")
            ),
            div(cls := "panels")(
              div(cls := "students active")(
                ctx.me.??(u => u.id === clazz.coach && !clazz.stopped) option
                  postForm(cls := "invite", action := routes.Student.invite(clazz._id))(
                    div(cls := "user-invite")(
                      label(`for` := "username")("邀请学员："),
                      input(cls := "user-autocomplete", id := "username", name := "username",
                        placeholder := "用户名", autofocus, required, dataTag := "span"),
                      submitButton(cls := "button", dataIcon := "E")
                    ),
                    error.map {
                      badTag(_)
                    }
                  ),
                div(
                  list.filterNot(_._2.joined).nonEmpty && clazz.isCoach(ctx.me) option frag(
                    table(cls := "slist")(
                      thead(
                        tr(
                          th("账号"),
                          th("姓名"),
                          th("级别"),
                          th("状态"),
                          th("操作")
                        )
                      ),
                      tbody(
                        list.filterNot(_._2.joined).sortBy(_._2.statusObject.sort) map {
                          case (userId, s) => studentTr(clazz, userId, s, users.find(_.id == userId))
                        }
                      )
                    ),
                    br
                  ),
                  table(cls := "slist")(
                    thead(
                      tr(
                        th("账号"),
                        th("姓名"),
                        th("级别"),
                        (ctx.me.get.id == clazz.coach) option th("状态"),
                        (ctx.me.get.id == clazz.coach) option th("操作")
                      )
                    ),
                    tbody(
                      if (list.isEmpty) {
                        tr(cls := "empty")(
                          td(colspan := (if (ctx.me.get.id == clazz.coach) 5 else 3))("还没有学员")
                        )
                      } else {
                        list.filter(_._2.joined).map {
                          case (userId, s) => studentTr(clazz, userId, s, users.find(_.id == userId))
                        }
                      }
                    )
                  )
                )
              ),
              div(cls := "homeworks")(
                table(cls := "slist")(
                  thead(
                    tr(
                      th("课节"),
                      th("上课时间"),
                      th("截止时间"),
                      th("完成进度"),
                      th
                    )
                  ),
                  tbody(
                    courseHomeworks.map { courseHomework =>
                      val course = courseHomework.course
                      val homeworkOption = courseHomework.homework
                      val stuHomeworkOption = courseHomework.stuHomework
                      val isCoach = ctx.me.??(u => clazzWithCoach.isCoach(u.id))
                      val isStudent = ctx.me.??(u => clazzWithCoach.isStudent(u.id))
                      tr(
                        td(s"第${course.index}节"),
                        td(course.courseFormatTime),
                        td(homeworkOption.map(homework => homework.deadline | "-") | "-"),
                        td(
                          if (isStudent) {
                            stuHomeworkOption.map(homework => homework.finishRate) | "-"
                          } else "-"
                        ),
                        td(
                          isCoach option a(cls := List("button button-empty" -> true, "button-green" -> !homeworkOption.??(_.hasContent)), href := routes.Homework.createForm(course.clazz, course.id))(
                            if (homeworkOption.??(_.hasContent)) "查看"
                            else "创建"
                          ),
                          isCoach option homeworkOption.map { homework =>
                            homework.isPublished option a(cls := "button button-empty", href := routes.Homework.report(homework.id))("报告")
                          },
                          isStudent option stuHomeworkOption.map { homework =>
                            a(cls := "button button-empty", href := routes.Homework.show(homework.id))("查看")
                          }
                        )
                      )
                    }
                  )
                )
              )
            )
          )
        )
      }
  }

  def studentTr(clazz: Clazz, userId: String, s: Student, user: Option[User])(implicit ctx: Context) =
    tr(
      td(userIdLink(userId.some)),
      td(user.map(_.realNameOrUsername)),
      td(user.map(_.profileOrDefault.currentLevel.label)),
      (ctx.me.get.id == clazz.coach) option td(s.statusPretty.name),
      (ctx.me.get.id == clazz.coach) option td(
        a(cls := "button button-empty", href := user.map(u => "/inbox/new?user=" + u.username))("发消息"),
        !clazz.stopped option postForm(style := "display:initial", action := routes.Student.remove(clazz._id, userId))(
          button(cls := "button button-empty button-red confirm remove", title := "确认移除")("移除")
        ),
        s.expired option postForm(style := "display:initial", action := routes.Student.invite(clazz._id))(
          form3.hidden("username", user.map(_.username) | ""),
          button(cls := "button button-empty button-green")("重新邀请")
        )
      )
    )
}

