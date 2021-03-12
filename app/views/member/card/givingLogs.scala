package views.html.member.card

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import play.api.data.Form
import play.mvc.Call
import org.joda.time.DateTime
import lila.member.{ MemberCardLog, MemberCard }
import controllers.routes

object givingLogs {

  def apply(pager: Paginator[MemberCardLog], form: Form[_])(implicit ctx: Context) = bits.layout(
    title = "购卡记录",
    active = "givingLogs",
    moreJs = frag(
      infiniteScrollTag,
      flatpickrTag,
      delayFlatpickrStart
    )
  ) {
      val call: Call = routes.MemberCard.givingLogPage()
      var url = if (call.url.contains("?")) call.url else call.url.concat("?q=1")
      form.data.foreach {
        case (key, value) =>
          url = url.concat("&").concat(key).concat("=").concat(value)
      }
      frag(
        h1("转赠记录"),
        st.form(
          cls := "search_form",
          action := s"$call#results",
          method := "GET"
        )(
            table(
              tr(
                th(label("会员账号")),
                td(form3.input(form("username"))),
                th,
                td
              ),
              tr(
                th(label("转赠时间")),
                td(form3.input2(form("dateMin"), vl = DateTime.now.minusMonths(3).toString("yyyy-MM-dd").some, klass = "flatpickr")),
                th(label("至")),
                td(form3.input2(form("dateMax"), vl = DateTime.now.toString("yyyy-MM-dd").some, klass = "flatpickr"))
              ),
              tr(
                th(label("卡类型")),
                td(form3.select(form("level"), lila.user.MemberLevel.choices, "".some)),
                th(label("卡状态")),
                td(form3.select(form("status"), MemberCard.CardStatus.choices, "".some))
              )
            ),
            div(cls := "action")(
              submitButton(cls := "button")("搜索")
            )
          ),
        table(cls := "slist")(
          thead(
            tr(
              th("卡号"),
              th("卡类型"),
              th("使用期限"),
              th("有效期至"),
              th("会员账号"),
              th("转赠时间"),
              th("卡状态")
            )
          ),
          if (pager.nbResults > 0) {
            tbody(cls := "infinitescroll")(
              pagerNextTable(pager, np => addQueryParameter(url, "page", np)),
              pager.currentPageResults.map { card =>
                tr(cls := "paginated")(
                  td(card.cardId),
                  td(card.level.name),
                  td(card.days.name),
                  td(card.expireAt.toString("yyyy-MM-dd HH:mm")),
                  td(card.newUserId),
                  td(momentFromNow(card.createAt)),
                  td(card.status.name)
                )
              }
            )
          } else {
            tbody(
              tr(
                td(colspan := 7)("暂无记录")
              )
            )
          }
        )
      )
    }
}
