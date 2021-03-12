package views.html.clazz

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.Clazz
import controllers.routes

object list {

  def current(list: List[Clazz.ClazzWithTeam])(implicit ctx: Context) = layout(
    title = "当前班级",
    active = "current",
    list = list
  )

  def history(list: List[Clazz.ClazzWithTeam])(implicit ctx: Context) = layout(
    title = "历史班级",
    active = "history",
    list = list
  )

  private[clazz] def layout(
    title: String,
    active: String,
    list: List[Clazz.ClazzWithTeam]
  )(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreJs = frag(
      flatpickrTag,
      jsTag("clazz.list.js")
    ),
    moreCss = cssTag("clazz")
  ) {
      main(cls := "page-menu clazz")(
        st.aside(cls := "page-menu__menu subnav")(
          menuLinks(active)
        ),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(
            h1("班级"),
            isGranted(_.Coach) option div(cls := "box__top__actions")(
              a(
                href := routes.Clazz.createForm,
                cls := "button button-green text",
                dataIcon := "O"
              )("建立新的班级")
            )
          ),
          table(
            cls := "slist clazz-list",
            thead(
              tr(
                th("序号"),
                th("名称"),
                th("班型"),
                th("俱乐部"),
                th("上课时间"),
                th("学员"),
                th("操作")
              )
            ),
            tbody(
              if (list.isEmpty) {
                tr(cls := "empty")(
                  td(colspan := 7)("没有更多班级")
                )
              } else {
                list.zipWithIndex.map { case (c, i) => clazzTr(c, i) }
              }
            )
          )
        )
      )
    }

  private[clazz] def clazzTr(c: Clazz.ClazzWithTeam, index: Int)(implicit ctx: Context) = {
    tr(
      td(index + 1),
      td(c.clazz.name),
      td(c.clazz.clazzType.name),
      td(c.teamName),
      td(
        c.clazz.clazzType match {
          case Clazz.ClazzType.Week => {
            c.clazz.weekClazz.map { wc =>
              ul(cls := "course")(
                wc.weekCourse.map { c =>
                  li(c.toString)
                }
              )
            }
          }
          case Clazz.ClazzType.Train => {
            c.clazz.trainClazz.map { tc =>
              ul(cls := "course")(
                tc.trainCourse.map { c =>
                  li(c.toString)
                }
              )
            }
          }
        }
      ),
      td(c.clazz.studentCount),
      td(
        a(cls := "button button-empty", href := routes.Clazz.detail(c.clazz._id))("进入班级"),
        (c.clazz.isCoach(ctx.me) && c.clazz.editable) option a(cls := "button button-empty", href := routes.Clazz.editForm(c.clazz._id))("修改"),
        c.clazz.isCoach(ctx.me) option frag(
          c.clazz.stopped option a(cls := "button button-empty button-red")("已停课"),
          !c.clazz.stopped option postForm(style := "display:initial", action := routes.Clazz.stop(c.clazz._id))(
            button(cls := "button button-empty button-red confirm stop", title := "确认停课？")("停课")
          )
        ),
        (c.clazz.isCoach(ctx.me) && c.clazz.deleteAble) option postForm(style := "display:initial", action := routes.Clazz.delete(c.clazz._id))(
          button(cls := "button button-empty button-red confirm delete", title := "确认删除？")("删除")
        )
      )
    )
  }

  private[clazz] def menuLinks(active: String)(implicit ctx: Context) = {
    def activeCls(c: String) = cls := (c == active).option("active")

    frag(
      a(activeCls("current"), href := routes.Clazz.current())("当前"),
      a(activeCls("history"), href := routes.Clazz.history())("历史")
    )
  }

}
