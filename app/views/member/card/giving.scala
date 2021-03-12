package views.html.member.card

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.member.MemberCard
import controllers.routes

object giving {

  def apply(card: MemberCard)(implicit ctx: Context) =
    div(cls := "modal-content modal-card-giving none")(
      h2("转赠会员卡"),
      postForm(cls := "form3 givingForm", action := routes.MemberCard.giving(card.id))(
        table(
          tr(
            th("会员卡号："),
            td(card.id)
          ),
          tr(
            th("会员卡类型："),
            td(card.level.name)
          ),
          tr(
            th("使用期限："),
            td(card.days.name)
          ),
          tr(
            th("有效期至："),
            td(card.expireAt.toString("yyyy-MM-dd HH:mm"))
          ),
          tr(
            th("选择会员："),
            td(
              input(cls := "user-autocomplete", name := "username", placeholder := "用户名", dataTag := "span"),
              div(cls := "note")("如果您是教练可以选择您的学员, 如果您是俱乐部可以选择俱乐部学员"),
              div(cls := "usernameError formError")
            )
          )
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交", klass = "giving disabled", isDisable = true)
        )
      )
    )

}
