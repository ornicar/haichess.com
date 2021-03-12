package views.html.member.card

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.member.MemberCard
import lila.common.paginator.Paginator
import controllers.routes

object mine {

  def apply(pager: Paginator[MemberCard], status: Option[MemberCard.CardStatus] = None, level: Option[lila.user.MemberLevel] = None)(implicit ctx: Context) = bits.layout(
    title = "会员卡",
    active = "mine",
    moreJs = frag(
      infiniteScrollTag,
      jsTag("member.giving.js")
    )
  ) {
      frag(
        div(cls := "box__top")(
          h1("会员卡"),
          div(cls := "box__top__actions")(
            views.html.base.bits.mselect(
              "card-status",
              "卡状态",
              MemberCard.CardStatus.all map { s =>
                a(href := routes.MemberCard.page(pager.currentPage, s.id.some, level.map(_.code)), cls := status.??(_ == s).option("current"))(s.name)
              }
            ),
            views.html.base.bits.mselect(
              "card-level",
              "卡类型",
              lila.user.MemberLevel.nofree map { l =>
                a(href := routes.MemberCard.page(pager.currentPage, status.map(_.id), l.code.some), cls := level.??(_ == l).option("current"))(l.name)
              }
            )
          )
        ),
        table(cls := "slist")(
          thead(
            tr(
              th("卡号"),
              th("卡类型"),
              th("使用期限"),
              th("有效期至"),
              th("操作")
            )
          ),
          if (pager.nbResults > 0) {
            tbody(cls := "infinitescroll")(
              pagerNextTable(pager, np => routes.MemberCard.page(np, status.map(_.id), level.map(_.code)).url),
              pager.currentPageResults.map { card =>
                tr(cls := "paginated")(
                  td(card.id),
                  td(card.level.name),
                  td(card.days.name),
                  td(card.expireAt.toString("yyyy-MM-dd HH:mm")),
                  td(
                    if (!card.isAvailable) "-"
                    else a(cls := "button button-empty modal-alert", href := routes.MemberCard.givingForm(card.id))("转赠")
                  )
                )
              }
            )
          } else {
            tbody(
              tr(
                td(colspan := 5)("暂无记录")
              )
            )
          }
        )
      )
    }

}
