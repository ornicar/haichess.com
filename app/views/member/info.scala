package views.html.member

import lila.user.Member
import lila.user.MemberLevel
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.member.{ MemberCard, MemberLevelLog, MemberPointsLog, Order }
import lila.member.MemberLevelLog.MemberLevelChangeType
import lila.user.MemberLevel.{ Gold, Silver }
import scala.math.BigDecimal.RoundingMode
import controllers.routes

object info {

  private val dataTab = attr("data-tab")

  def apply(
    u: lila.user.User,
    orders: List[Order],
    cards: List[MemberCard],
    levelChangeLogs: List[MemberLevelLog],
    levelPointsLogs: List[MemberPointsLog]
  )(implicit ctx: Context) = views.html.account.layout(
    title = s"${u.username} - 会员信息",
    active = "member",
    evenMoreJs = frag(
      jsTag("member.info.js")
    ),
    evenMoreCss = cssTag("member")
  ) {
      val member = u.memberOrDefault
      div(cls := "box box-pad account member-info")(
        div(cls := "member-line")(
          userSpan(u, cssClass = "large".some, withPowerTip = false),
          div(member.points, nbsp, "积分")
        ),
        div(cls := "levels")(
          MemberLevel.nofree.map { l =>
            level(l, member)
          }
        ),
        div(cls := "component")(
          div(cls := "tabs-horiz")(
            span(dataTab := "levelLogs", cls := "active")(s"续费记录(${levelChangeLogs.length})"),
            span(dataTab := "card")(s"会员卡(${cards.filterNot(_.isUsed).length})"),
            span(dataTab := "coupon")("优惠券(0)"),
            span(dataTab := "points")(s"积分记录(${levelPointsLogs.length})"),
            span(dataTab := "orders")(s"历史订单(${orders.length})")
          ),
          div(cls := "tabs-content")(
            div(cls := "levelLogs active")(
              table(cls := "slist")(
                thead(
                  tr(
                    th("会员信息"),
                    th("操作时间"),
                    th("原到期时间"),
                    th("现到期时间"),
                    th("续费方式"),
                    th("备注"),
                    th("操作")
                  )
                ),
                tbody(
                  levelChangeLogs.map { log =>
                    tr(
                      td(log.desc),
                      td(momentFromNow(log.createAt)),
                      td(log.oldExpireAt.map(_.toString("yyyy-MM-dd HH:mm")) | "-"),
                      td(log.newExpireAt.map(_.toString("yyyy-MM-dd HH:mm")) | "-"),
                      td(log.typ.name),
                      td(log.note | "-"),
                      td(
                        log.typ match {
                          case MemberLevelChangeType.Buy => {
                            log.orderId.map { orderId =>
                              a(href := routes.MemberOrder.info(orderId), target := "_blank")("详情")
                            } getOrElse "-"
                          }
                          case _ => "-"
                        }
                      )
                    )
                  }
                )
              )
            ),
            div(cls := "card")(
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
                tbody(
                  cards.map { card =>
                    tr(
                      td(card.id),
                      td(card.level.name),
                      td(card.days.name),
                      td(card.expireAt.toString("yyyy-MM-dd HH:mm")),
                      td(
                        card.isAvailable option postForm(action := routes.MemberCard.use(card.id))(
                          button(cls := "button button-empty confirm", title := "确认使用？")("使用")
                        ),
                        card.isUsed option button(cls := "button button-empty disabled", disabled)("已使用"),
                        card.expired option button(cls := "button button-empty disabled", disabled)("已过期")
                      )
                    )
                  }
                )
              )
            ),
            div(cls := "coupon")(),
            div(cls := "points")(
              table(cls := "slist")(
                thead(
                  tr(
                    th("时间"),
                    th("信息"),
                    th("积分")
                  )
                ),
                tbody(
                  levelPointsLogs.map { log =>
                    tr(
                      td(cls := "time")(momentFromNow(log.createAt)),
                      td(
                        log.typ match {
                          case MemberPointsLog.PointsType.OrderPay | MemberPointsLog.PointsType.OrderPayRebate => log.orderId.map { orderId =>
                            a(href := routes.MemberOrder.info(orderId), target := "_blank")(log.typ.name)
                          }
                          case MemberPointsLog.PointsType.BackendGiven => frag(log.typ.name)
                        }
                      ),
                      td(cls := List("diff" -> true, "minus" -> (log.diff < 0)))(
                        if (log.diff < 0) { log.diff } else { "+" + log.diff }
                      )
                    )
                  }
                )
              )
            ),
            div(cls := "orders")(
              table(cls := "slist")(
                thead(
                  tr(
                    th("订单编号"),
                    th("商品描述"),
                    th("支付金额"),
                    th("订单状态"),
                    th("备注"),
                    th("创建时间"),
                    th("操作")
                  )
                ),
                tbody(
                  orders.map { order =>
                    tr(
                      td(order.id),
                      td(order.descWithCount),
                      td(label(cls := "symbol")("￥"), span(cls := "number")(order.payAmount.setScale(2, RoundingMode.DOWN).toString())),
                      td(order.status.name),
                      td(order.note | "-"),
                      td(momentFromNow(order.createAt)),
                      td(a(href := routes.MemberOrder.info(order.id), target := "_blank")("详情"))
                    )
                  }
                )
              )
            )
          )
        )
      )
    }

  def level(ml: MemberLevel, member: Member) = {
    val levelWithExpireOption = member.levels.get(ml.code)
    div(cls := List("level" -> true, s"bg-${ml.code}" -> true))(
      div(cls := "header")(
        div(cls := "title")(
          span(cls := "name")(ml.name),
          ml == member.lv option span(cls := "current")("当前"),
          levelWithExpireOption.??(_.expired) option span(cls := "expired")("已过期")
        ),
        (ml.id > member.lv.id || (ml == member.lv && !member.lvWithExpire.isForever)) option {
          a(cls := s"btn-vip ${ml.code}", href := routes.Member.toBuy(ml.code.some))(levelWithExpireOption.map(_ => "续费") | "加入")
        }
      ),
      levelWithExpireOption.map { levelWithExpire =>
        div(cls := "note")(
          "有效期至：", levelWithExpire.expireNote
        )
      } | {
        div(cls := "note")(
          span("加入", ml.name, "，享受", a(cls := "member-intro")("更多"), "功能"),
          (!member.isGoldAvailable && member.isSilverAvailable && ml == Gold) option {
            span(cls := "tip")("（", "您现在是", Silver.name, "加入时首先计算差价", "）")
          }
        )
      }
    )
  }

}
