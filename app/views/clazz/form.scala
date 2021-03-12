package views.html.clazz

import play.api.data.{ Field, Form }
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.Clazz
import lila.clazz.ClazzForm
import controllers.routes

object form {

  val weekList = List((1 -> "周一"), (2 -> "周二"), (3 -> "周三"), (4 -> "周四"), (5 -> "周五"), (6 -> "周六"), (7 -> "周日"))

  def create(form: Form[_], teams: List[(String, String)])(implicit ctx: Context) = {
    views.html.base.layout(
      title = "建立班级",
      moreJs = frag(
        flatpickrTag,
        jsTag("clazz.form.js")
      ),
      moreCss = cssTag("clazz")
    ) {
        main(cls := "box box-pad page-small create")(
          h1("建立班级"),
          postForm(cls := "form3", action := routes.Clazz.create)(
            fm(form, teams, none)
          )
        )
      }
  }

  def update(form: Form[_], teams: List[(String, String)], clazz: Clazz)(implicit ctx: Context) = {
    views.html.base.layout(
      title = "修改班级",
      moreJs = frag(
        flatpickrTag,
        jsTag("clazz.form.js")
      ),
      moreCss = cssTag("clazz")
    ) {
        main(cls := "box box-pad page-small create")(
          h1("修改班级"),
          postForm(cls := "form3", action := routes.Clazz.update(clazz.id))(
            fm(form, teams, clazz.some)
          )
        )
      }
  }

  def fm(form: Form[_], teams: List[(String, String)], clazz: Option[Clazz])(implicit ctx: Context) = frag(
    globalError(form),
    form3.split(
      div(cls := "form-head")(
        form3.input(form("color"), "color", "head")
      ),
      div(cls := "form-head-other")(
        form3.split(
          form3.group(form("name"), raw("班级名称"), half = true)(form3.input(_)),
          form3.group(form("team"), raw("所属俱乐部"), half = true)(form3.select(_, teams, default = "".some))
        ),
        form3.group(form("clazzType"), raw("班级类型")) { f =>
          val select = clazz.fold(ClazzForm.clazzTypeChoices)(c => ClazzForm.clazzTypeChoices.filter(_._1 == c.clazzType.id))
          form3.select(f, select)
        },
        div(cls := List("week-clazz" -> true, "none" -> form("clazzType").value.??(_ == Clazz.ClazzType.Train.id)))(
          form3.group(form("weekClazz")("dateStart"), raw("开始日期"), half = true)(form3.input(_, klass = "flatpickr")),
          form3.group(form("weekClazz")("times"), raw("课节数"), half = true)(form3.input(_, "number")),
          form3.hidden(form("weekClazz")("dateEnd")),
          div(cls := "course-list")(
            (0 to 6) map { i =>
              form("weekClazz")("weekCourse[" + i + "]")("week").value map { _ =>
                div(cls := "course")(
                  courseWeek(form("weekClazz")("weekCourse[" + i + "]")("week"), weekList),
                  courseTime(form("weekClazz")("weekCourse[" + i + "]")("timeBegin")),
                  strong("至"),
                  courseTime(form("weekClazz")("weekCourse[" + i + "]")("timeEnd")),
                  div(cls := "control")(
                    a(cls := "rm", title := "移除")("-"),
                    a(cls := "ad", title := "添加")("+")
                  )
                )
              }
            }
          ),
          form.errors("weekClazz").map(e => span(cls := "error")(e.message))
        ),
        div(cls := List("train-clazz" -> true, "none" -> form("clazzType").value.??(_ == Clazz.ClazzType.Week.id)))(
          form3.hidden(form("trainClazz")("times")),
          form3.hidden(form("trainClazz")("dateStart")),
          form3.hidden(form("trainClazz")("dateEnd")),
          div(cls := "course-list")(
            (0 to 6) map { i =>
              form("trainClazz")("trainCourse[" + i + "]")("dateStart").value map { _ =>
                div(cls := "course")(
                  courseDate(form("trainClazz")("trainCourse[" + i + "]")("dateStart")),
                  strong("至"),
                  courseDate(form("trainClazz")("trainCourse[" + i + "]")("dateEnd")),
                  nbsp, nbsp,
                  courseTime(form("trainClazz")("trainCourse[" + i + "]")("timeBegin")),
                  strong("至"),
                  courseTime(form("trainClazz")("trainCourse[" + i + "]")("timeEnd")),
                  div(cls := "control")(
                    a(cls := "rm", title := "移除")("-"),
                    a(cls := "ad", title := "添加")("+")
                  )
                )
              }
            }
          ),
          form.errors("trainClazz").map(e => span(cls := "error")(e.message))
        ),
        form3.actions(
          a(href := routes.Clazz.current)("取消"),
          form3.submit("提交")
        )
      )
    )
  )

  def courseWeek(
    field: Field,
    options: Iterable[(Any, String)]
  ): Frag =
    select(
      name := field.name,
      cls := "form-control"
    )(
        options.toSeq map {
          case (value, name) => option(
            st.value := value.toString,
            field.value.has(value.toString) option selected
          )(name)
        }
      )

  def courseDate(field: Field) =
    input(
      name := field.name,
      value := field.value,
      cls := "form-control flatpickr"
    )

  def courseTime(field: Field) =
    input(
      name := field.name,
      value := field.value,
      dataEnableTime := true,
      datatime24h := true,
      dataNoCalendar := true,
      cls := "form-control flatpickr"
    )
}
