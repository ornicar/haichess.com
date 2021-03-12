package views.html.member

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.member.{ Order, ProductType }
import scala.math.BigDecimal.RoundingMode
import controllers.routes

object orderPayReturn {

  def apply(order: Order)(implicit ctx: Context) =
    views.html.base.layout(
      title = "支付完成",
      moreCss = cssTag("member")
    ) {
        main(cls := "page-small box box-pad orderPayReturn")(
          h1("支付完成"),
          div(cls := "message")(
            div(cls := "desc")(
              span(order.desc),
              span(
                "订单编号：",
                a(href := routes.MemberOrder.info(order.id), target := "_blank")(order.id)
              )
            ),
            div(cls := "amount")(
              "金额：", span(cls := "number")("￥", order.payAmount.setScale(2, RoundingMode.DOWN).toString())
            ),
            div(cls := "link")(
              order.typ match {
                case ProductType.VirtualMember => a(cls := "button", href := routes.Member.info(), target := "_blank")("返回")
                case ProductType.VirtualMemberCard => a(cls := "button", href := routes.MemberCard.page(1), target := "_blank")("返回")
                case _ =>
              }
            )
          ),
          p(cls := "is-gold", dataIcon := "")(
            "支付已经完成，如果未收到商品请稍后再刷新查看，或", a(href := "/contact")("联系我们"), "。"
          )
        )
      }

}
