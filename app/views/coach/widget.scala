package views.html
package coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.coach.Student
import controllers.routes

object widget {

  def titleName(c: lila.coach.Coach.WithUser) = frag(
    c.user.title.map { t => s"$t " },
    c.user.realNameOrUsername
  )

  def pic(c: lila.coach.Coach.WithUser, size: Int)(implicit ctx: Context) =
    c.coach.profile.picturePath.map { path =>
      img(width := size, height := size, cls := "picture", src := dbImageUrl(path), alt := s"${c.user.titleUsername} 教练")
    }.getOrElse {
      img(width := size, height := size, cls := "default picture", src := staticUrl("images/coach-default.svg"))
    }

  def apply(c: lila.coach.Coach.WithUser, student: Option[Student], link: Boolean)(implicit ctx: Context) = {
    val profile = c.user.profileOrDefault
    frag(
      link option a(cls := "overlay", href := routes.Coach.show(c.user.username)),
      pic(c, if (link) 300 else 350),
      div(cls := "overview")(
        (if (link) h2 else h1)(cls := "coach-name")(titleName(c)),
        c.coach.profile.headline.map { h =>
          p(cls := s"headline ${if (h.size < 60) "small" else if (h.size < 120) "medium" else "large"}")(h)
        },
        table(
          tbody(
            tr(
              th("城市"),
              td(
                span(cls := "location")(profile.location)
              )
            ),
            c.coach.profile.languages.map { l =>
              tr(cls := "languages")(
                th("教学语言"),
                td(l)
              )
            },
            tr(cls := "rating")(
              th("积分"),
              td(
                a(href := routes.User.show(c.user.username))(
                  c.user.best8Perfs.take(6).filter(c.user.hasEstablishedRating).map {
                    showPerfRating(c.user, _)
                  }
                )
              )
            ),
            c.coach.profile.hourlyRate.map { r =>
              tr(cls := "rate")(
                th("小时费用"),
                td(r)
              )
            },
            tr(cls := "available")(
              th("招收学员"),
              td(
                if (c.coach.available.value) {
                  if (ctx.isAnon || ctx.me.??(c.coach.id.value == _.id)) {
                    button(cls := "button button-green apply disabled", dataIcon := "E", disabled)("正在招收学员")
                  } else {
                    student.map { stu =>
                      stu.status match {
                        case Student.Status.Applying => button(cls := "button button-green apply disabled", dataIcon := "E", disabled)("申请成为学员（", stu.status.name, "）")
                        case Student.Status.Approved => {
                          if (stu.available) {
                            button(cls := "button button-green apply disabled", dataIcon := "E", disabled)("申请成为学员（", stu.status.name, "）")
                          } else button(cls := "button button-green apply", dataIcon := "E", dataId := c.coach.id.value)("申请成为学员")
                        }
                        case Student.Status.Decline => button(cls := "button button-green apply", dataIcon := "E", dataId := c.coach.id.value)("申请成为学员")
                      }
                    } getOrElse {
                      button(cls := "button button-green apply", dataIcon := "E", dataId := c.coach.id.value)("申请成为学员")
                    }
                  }
                } else button(cls := "button button-green apply disabled", dataIcon := "L", disabled)("暂时不招收学员")
              )
            ),
            c.user.seenAt.map { seen =>
              tr(cls := "seen")(
                th,
                td(trans.lastSeenActive(momentFromNow(seen)))
              )
            }
          )
        )
      )
    )
  }
}
