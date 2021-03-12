package views.html.member

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.member.Order
import scala.math.BigDecimal.RoundingMode
import controllers.routes

object order {

  def apply(order: Order)(implicit ctx: Context) = views.html.base.layout(
    title = "订单详情",
    moreCss = cssTag("member")
  ) {
      val zero = BigDecimal(0.00)
      main(cls := "box box-pad page-small")(
        h1("订单详情"),
        table(cls := "orderInfo")(
          tr(
            th("订单单号"),
            td(order.id)
          ),
          tr(
            th("商品信息"),
            td(order.descWithCount)
          ),
          tr(
            th("订单状态"),
            td(order.status.name)
          ),
          tr(
            th("交易时间"),
            td(order.createAt.toString("yyyy-MM-dd HH:mm"))
          ),
          tr(
            th("原始价格"),
            td(
              div(
                label(cls := "symbol")("￥"),
                span(cls := "number")(priceOf(order.totalAmount))
              )
            )
          ),
          !order.amounts.isEmpty option tr(
            th("优惠信息"),
            td(
              ul(
                order.amounts.silverDaysAmount.map { amount =>
                  (amount > zero) option li(
                    label(cls := "symbol")("银牌会员升级抵扣：￥"),
                    span(cls := "number")(priceOf(amount))
                  )
                },
                order.amounts.promotions.ladderPromotion.map { amount =>
                  li(
                    label(cls := "symbol")("折扣："),
                    span(cls := "number")(amount.promotion.name)
                  )
                },
                order.amounts.inviteUserAmount.map { amount =>
                  (amount > zero) option li(
                    label(cls := "symbol")("邀请码抵扣：￥"),
                    span(cls := "number")(priceOf(amount))
                  )
                },
                order.amounts.pointsAmount.map { amount =>
                  (amount > zero) option li(
                    label(cls := "symbol")("积分抵扣：￥"),
                    span(cls := "number")(priceOf(amount))
                  )
                },
                order.amounts.couponAmount.map { amount =>
                  (amount > zero) option li(
                    label(cls := "symbol")("优惠卷抵扣：￥"),
                    span(cls := "number")(priceOf(amount))
                  )
                }
              )
            )
          ),
          tr(
            th("支付金额"),
            td(
              div(
                label(cls := "symbol")("￥"),
                span(cls := "number")(priceOf(order.payAmount))
              )
            )
          ),
          tr(
            th("支付方式"),
            td(order.payWay.name)
          )
        )
      )
    }

  private def priceOf(d: BigDecimal) = d.setScale(2, RoundingMode.DOWN).toString()

}
