package views.html.clazz

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.Course
import play.api.data.Form
import controllers.routes

object modal {

  object clazz {

  }

  object course {

    def update(course: Course, form: Form[_], week: Int)(implicit ctx: Context) = frag(
      div(cls := "modal-content course-update none")(
        h2("修改课时"),
        postForm(cls := "form3", action := routes.Course.update(course._id, week))(
          form3.split(
            div(cls := "form-third info")(
              course.date.toString("M月d日"),
              "（" + course.weekFormat + "）"
            ),
            div(cls := "form-third info")(course.timeBegin),
            div(cls := "form-third info")(course.timeEnd)
          ),
          form3.split(
            form3.group(form("date"), raw("日期"), klass = "form-third")(form3.input(_, klass = "flatpickr")(dataMinDate := "today")),
            form3.group(form("timeBegin"), raw("开始时间"), klass = "form-third")(form3.timeFlatpickr(_)),
            form3.group(form("timeEnd"), raw("结束时间"), klass = "form-third")(form3.timeFlatpickr(_))
          ),
          form3.actions(
            a(cls := "cancel")("取消"),
            form3.submit("提交")
          )
        )
      )
    )

    def stop(id: String, week: Int)(implicit ctx: Context) = frag(
      div(cls := "modal-content none")(
        h2("停课一次"),
        postForm(cls := "form3", action := routes.Course.stop(id, week))(
          h3("说明：本次课取消，课节直接跳过。"),
          form3.actions(
            a(cls := "cancel")("取消"),
            form3.submit("提交")
          )
        )
      )
    )

    def postpone(id: String, week: Int)(implicit ctx: Context) = frag(
      div(cls := "modal-content none")(
        h2("顺延"),
        postForm(cls := "form3", action := routes.Course.postpone(id, week))(
          h3("说明：本次及后续课节统一做顺延，保证与实际发生一致。"),
          form3.actions(
            a(cls := "cancel")("取消"),
            form3.submit("提交")
          )
        )
      )
    )

  }

}
