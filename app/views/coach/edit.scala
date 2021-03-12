package views.html.coach

import play.api.data.Form
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object edit {

  private val dataTab = attr("data-tab")
  private val dataValue = attr("data-value")

  def apply(c: lila.coach.Coach.WithUser, form: Form[_])(implicit ctx: Context) = {
    views.html.account.layout(
      title = s"${c.user.titleUsername} 教练资料",
      evenMoreCss = cssTag("coach.editor"),
      evenMoreJs = frag(
        singleUploaderTag,
        jsTag("coach.form.js")
      ),
      active = "coach-profile"
    )(
        div(cls := "account coach-edit box box-pad")(
          h1(c.user.username),
          postForm(cls := "form3", action := routes.Coach.editApply)(
            div(cls := "tabs")(
              div(dataTab := "basics", cls := "active")("基本信息"),
              div(dataTab := "texts")("详细介绍")
            ),
            div(cls := "panels")(
              div(cls := "panel basics active")(
                form3.split(
                  form3.groupNoLabel(form("profile.picturePath"), klass = "head form-half")(form3.singleImage(_, "上传头像"))
                ),
                form3.split(
                  form3.group(form("profile.headline"), raw("简短标题"), help = raw("只需一句话，让学生认识您").some, half = true)(form3.input(_)),
                  form3.checkbox(form("available"), raw("招收学员"), help = raw("是否招收学员").some, half = true)
                ),
                form3.split(
                  form3.group(form("profile.languages"), raw("教学语言"), help = raw("您可以使用哪些语言上课？").some, half = true)(form3.input(_)),
                  form3.group(form("profile.hourlyRate"), raw("小时费用"), help = raw("指示性，非合同性").some, half = true)(form3.input(_))
                )
              ),
              div(cls := "panel texts")(
                form3.group(form("profile.description"), raw("您是谁？"), help = raw("年龄，职业，国家...让您的学生认识您").some)(form3.textarea(_)(rows := 8)),
                form3.group(form("profile.playingExperience"), raw("参赛经验"), help = raw("参加比赛，取得最佳成绩或其它成就").some)(form3.textarea(_)(rows := 8)),
                form3.group(form("profile.teachingExperience"), raw("教学经验"), help = raw("学历，实际教龄，学生最好成绩").some)(form3.textarea(_)(rows := 8)),
                form3.group(form("profile.otherExperience"), raw("其它经验"), help = raw("作为国际象棋解说员，或教授其他领域").some)(form3.textarea(_)(rows := 8)),
                form3.group(form("profile.skills"), raw("教学技巧"), help = raw("最好的国际象棋和教学技巧").some)(form3.textarea(_)(rows := 8)),
                form3.group(form("profile.methodology"), raw("教学方法"), help = raw("您如何准备和运行课程。您如何跟进学生。").some)(form3.textarea(_)(rows := 8))
              )
            ),
            form3.actions(
              a(href := routes.Coach.show(c.user.username), cls := "preview")("预览"),
              form3.submit(trans.apply())
            )
          )
        )
      )
  }
}
