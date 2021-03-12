package views.html.member.card

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

/**
 * views.member.card
 *
 * @author nan.zhou
 * @date 2021/1/13
 */
object bits {

  private[member] def layout(
    title: String,
    active: String,
    moreCss: Frag = emptyFrag,
    moreJs: Frag = emptyFrag
  )(content: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = frag(
      cssTag("member"),
      moreCss
    ),
    moreJs = moreJs
  ) {
      main(cls := "page-menu member")(
        st.aside(cls := "page-menu__menu subnav")(
          menuLinks(active)
        ),
        div(cls := "page-menu__content box box-pad")(
          content
        )
      )
    }

  private[member] def menuLinks(active: String)(implicit ctx: Context) = {
    def activeCls(c: String) = cls := (c == active).option("active")
    frag(
      a(activeCls("mine"), href := routes.MemberCard.page(1, none, none))("会员卡"),
      a(activeCls("givingLogs"), href := routes.MemberCard.givingLogPage(1))("转赠记录"),
      a(activeCls("buy"), href := routes.MemberCard.toBuy)("购卡"),
      a(activeCls("orders"), href := routes.MemberOrder.page(1))("购卡记录")
    )
  }

}
