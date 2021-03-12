package views.html
package coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.coach.Student
import controllers.routes

object show {

  private def section(title: String, text: Option[lila.coach.CoachProfile.RichText]) = text.map { t =>
    st.section(
      h2(title),
      div(cls := "content")(richText(t.value))
    )
  }

  def apply(
    c: lila.coach.Coach.WithUser,
    student: Option[Student]
  )(implicit ctx: Context) = {
    val profile = c.coach.profile
    val coachName = s"${c.user.title.??(t => s"$t ")}${c.user.realNameOrUsername}"
    val title = s"$coachName 教练资料"
    views.html.base.layout(
      title = title,
      moreJs = frag(
        jsTag("coach.show.js")
      ),
      moreCss = cssTag("coach"),
      openGraph = lila.app.ui.OpenGraph(
        title = title,
        description = shorten(~(c.coach.profile.headline), 152),
        url = s"$netBaseUrl${routes.Coach.show(c.user.username)}",
        `type` = "profile",
        image = c.coach.profile.picturePath.map(p => dbImageUrl(p))
      ).some
    ) {
        main(cls := "coach-show coach-full-page")(
          st.aside(cls := "coach-show__side coach-side")(
            a(cls := "button button-empty", href := routes.User.show(c.user.username))("显示资料"),
            if (ctx.me.exists(c.coach.is)) frag(
              a(cls := "button button-empty", href := routes.Coach.edit)("编辑资料")
            )
            else a(cls := "button button-empty", href := s"${routes.Message.form}?user=${c.user.username}")("私信")
          ),
          div(cls := "coach-show__main coach-main box")(
            div(cls := "coach-widget")(widget(c, student, link = false)),
            div(cls := "coach-show__sections")(
              section("关于我", profile.description),
              section("参赛经验", profile.playingExperience),
              section("教学经验", profile.teachingExperience),
              section("其它经验", profile.otherExperience),
              section("教学技巧", profile.skills),
              section("教学方法", profile.methodology)
            )
          )
        )
      }
  }
}
