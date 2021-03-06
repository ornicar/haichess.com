package views.html.team

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.app.templating.Environment._

object ratingRulePage {

  def apply()(implicit ctx: Context) = views.html.base.layout(
    title = "计分规则",
    moreCss = cssTag("team")
  )(
      main(cls := "page-small box box-pad ratingRule")(
        h1("俱乐部等级分计分规则"),
        div(cls := "s1")(
          div(cls := "h2")(
            "每个俱乐部成员在俱乐部内部有一个唯一俱乐部等级分，用于俱乐部内部评估棋手的棋力（最低500，最高3200），俱乐部管理员对内部积分有绝对的管理权。"
          )
        ),
        div(cls := "s1")(
          div(cls := "h2")(b("只有俱乐部会员之间的对局会计算俱乐部成员的内部等级分，包括3种情况：")),
          div(cls := "s2")(
            div(cls := "h2")(s"1. 线上、线下比赛，在创建比赛时如果选择了计算内部等级分；")
          ),
          div(cls := "s2")(
            div(cls := "h2")(s"2. 线上有积分对局，以初始局面开始，并满足管理员设定的条件。注意，闪电棋（小于2分钟）不计算等级分；")
          ),
          div(cls := "s2")(
            div(cls := "h2")(s"3. 管理员手工设定。")
          )
        ),
        div(cls := "s1")(
          div(cls := "h2")(b("等级分的计算与更新：")),
          div(cls := "s2")(
            div(cls := "h2")("1. 每次对局完成，实时更新等级分；（比赛如果有多轮，由于计算公式的线性关系，每轮计算和所有轮次结束统一算，最终结果是一样的）。")
          ),
          div(cls := "s2")(
            div(cls := "h2")("2. 计算公式：Rn = Ro + K * (W - We)"),
            div("其中：Rn是对局后的新等级分；Ro是对局前的原等级分；K是成长系数，取值按第6条；W是实际对局得分(胜得1、和得0.5)；We是在原等级分基础上的预期对局得分，We取值根据对局双方原有等级分查表获得。")
          ),
          div(cls := "s2")(
            div(cls := "h2")("3. 棋手K取值："),
            ul(
              li("a) 默认为20"),
              li("b) 管理员可对每位成员的情况，自行设定。")
            )
          )
        )
      )
    )
}

