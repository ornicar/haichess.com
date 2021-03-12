package views.html.member

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.MemberLevel
import scala.util.Random
import controllers.routes

object intro {

  def apply(hasGoldCard: Boolean, hasSilverCard: Boolean)(implicit ctx: Context) = {
    val cm = Map(
      MemberLevel.Gold.code -> hasGoldCard,
      MemberLevel.Silver.code -> hasSilverCard
    )

    div(cls := "modal-content member-intro none")(
      div(cls := "modal-header")(
        h3("会员升级"),
        span(cls := "close", dataIcon := "L")
      ),
      div(cls := "levels")(
        MemberLevel.all map { level =>
          val permissions = level.permissions
          div(cls := s"level ${level.code}")(
            div(cls := "level-title")(
              img(cls := "icon", src := staticUrl(s"/images/icons/${level.code}.svg")), nbsp, level.name
            ),
            div(cls := "level-permission")(
              table(
                tr(
                  th("战术题"),
                  td(
                    if (permissions.puzzle == Int.MaxValue) "无限制"
                    else frag(permissions.puzzle, "/天")
                  )
                ),
                tr(
                  th("主题战术"),
                  td(
                    if (permissions.themePuzzle == Int.MaxValue) "无限制"
                    else frag(permissions.themePuzzle, "/天")
                  )
                ),
                tr(
                  th("战术冲刺"),
                  td(
                    if (permissions.puzzleRush == Int.MaxValue) "无限制"
                    else frag(permissions.puzzleRush, "/天")
                  )
                ),
                tr(
                  th("研习"),
                  td(
                    if (permissions.study.Private) "创建私有研习" else "创建公共研习"
                  )
                ),
                tr(
                  th("资源管理"),
                  td(
                    if (permissions.resource) "有" else "无"
                  )
                ),
                tr(
                  th("数据洞察"),
                  td(
                    if (permissions.insight) "有" else "无"
                  )
                )
              )
            ),
            div(cls := "level-foot")(
              if (level.prices.year.equals(0)) frag(
                div(cls := "price invisible")(
                  span(cls := "symbol")("￥"),
                  span(cls := "number")(level.prices.year.setScale(1).toString),
                  nbsp, "/年"
                ),
                div(cls := "buy")(span(cls := "btn free disabled")("免费"))
              )
              else frag(
                div(cls := "price")(
                  span(cls := "symbol")("￥"),
                  span(cls := "number")(level.prices.year.setScale(1).toString),
                  nbsp, "/年"
                ),
                div(cls := "buy")(
                  a(cls := List("btn toBuy" -> true, "disabled" -> (isGranted(_.Coach) || isGranted(_.Team))), (!isGranted(_.Coach) && !isGranted(_.Team)) option (href := routes.Member.toBuy(level.code.some)))("购买"),
                  cm.exists(d => d._1 == level.code && d._2) && !isGranted(_.Coach) && !isGranted(_.Team) option a(cls := "useCard", href := s"${routes.Member.info}?q=${Random.nextInt()}#card")("使用会员卡")
                )
              )
            )
          )
        }
      ),
      div(cls := "ad")("通过认证教练或俱乐部购买，可享受团购优惠！")
    )
  }

}
