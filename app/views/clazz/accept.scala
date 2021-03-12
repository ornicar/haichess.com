package views.html.clazz

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.clazz.Clazz
import lila.clazz.Clazz.ClazzWithCoach
import lila.clazz.Student.InviteStatus
import lila.team.Team
import controllers.routes

object accept {

  def apply(clazzWithCoach: ClazzWithCoach, team: Option[Team], error: Option[String])(implicit ctx: Context) = {
    val clazz = clazzWithCoach.clazz
    views.html.base.layout(
      title = "接受邀请",
      moreCss = cssTag("clazz")
    ) {
        main(cls := "box box-pad page-small detail")(
          bits.clazzInfo(clazzWithCoach, team),
          div(cls := "accept-box")(
            clazzWithCoach.clazz.studentList.find(_._1 == ctx.me.get.id) map { s =>
              s._2.statusPretty match {
                case InviteStatus.Invited => forms(clazz)
                case InviteStatus.Joined => message("学员已经加入")
                case InviteStatus.Expired => message("邀请已经过期")
                case InviteStatus.Refused => message("邀请已经拒绝")
              }
            },
            error.map {
              badTag(_)
            }
          )
        )
      }
  }

  def forms(clazz: Clazz) = div(
    p(strong(
      "加入班级之后，您将成为教练的学员，有权访问教练设置了学员权限的资源；同时，教练可以看到您的个人信息、动态、数据洞察和课后练等信息。"
    )),
    div(cls := "actions")(
      postForm(action := routes.Student.accept(clazz._id))(
        submitButton(cls := "text button", dataIcon := "E")("接受")
      ),
      postForm(action := routes.Student.refused(clazz._id))(
        submitButton(cls := "text button button-red", dataIcon := "L")("拒绝")
      )
    )
  )

  def message(message: String) = h2(message)

}

