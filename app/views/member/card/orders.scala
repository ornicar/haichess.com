package views.html.member.card

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import play.api.data.Form
import play.mvc.Call
import scala.math.BigDecimal.RoundingMode
import org.joda.time.DateTime
import lila.member.{ Order, ProductType }
import controllers.routes

object orders {

  def apply(pager: Paginator[Order], form: Form[_])(implicit ctx: Context) = bits.layout(
    title = "购卡记录",
    active = "orders",
    moreJs = frag(
      infiniteScrollTag,
      flatpickrTag,
      delayFlatpickrStart
    )
  ) {
      val call: Call = routes.MemberOrder.page()
      var url = if (call.url.contains("?")) call.url else call.url.concat("?q=1")
      form.data.foreach {
        case (key, value) =>
          url = url.concat("&").concat(key).concat("=").concat(value)
      }
      frag(
        h1("购卡记录"),
        st.form(
          cls := "search_form",
          action := s"$call#results",
          method := "GET"
        )(
            form3.hidden(form("typ"), ProductType.VirtualMemberCard.id.some),
            table(
              tr(
                th(label("交易时间")),
                td(form3.input2(form("dateMin"), vl = DateTime.now.minusMonths(3).toString("yyyy-MM-dd").some, klass = "flatpickr")),
                th(label("至")),
                td(form3.input2(form("dateMax"), vl = DateTime.now.toString("yyyy-MM-dd").some, klass = "flatpickr"))
              ),
              tr(
                th(label("会员类型")),
                td(form3.select(form("level"), lila.user.MemberLevel.choices, "".some)),
                th,
                td
              )
            ),
            div(cls := "action")(
              submitButton(cls := "button")("搜索")
            )
          ),
        table(cls := "slist")(
          thead(
            tr(
              th("订单单号"),
              th("交易时间"),
              th("会员类型"),
              th("数量"),
              th("支付金额"),
              th("支付方式"),
              th("操作")
            )
          ),
          if (pager.nbResults > 0) {
            tbody(cls := "infinitescroll")(
              pagerNextTable(pager, np => addQueryParameter(url, "page", np)),
              pager.currentPageResults.map { order =>
                tr(cls := "paginated")(
                  td(order.id),
                  td(momentFromNow(order.createAt)),
                  td(order.products.attrs.get("level").map(lila.user.MemberLevel(_).name) | "-"),
                  td(order.products.count),
                  td("￥", priceOf(order.payAmount)),
                  td(order.payWay.name),
                  td(
                    a(href := routes.MemberOrder.info(order.id), target := "_blank")("详情")
                  )
                )
              }
            )
          } else {
            tbody(
              tr(
                td(colspan := 6)("暂无记录")
              )
            )
          }
        )
      )
    }
  private def priceOf(d: BigDecimal) = d.setScale(2, RoundingMode.DOWN).toString()
}
