package views.html.site

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object lag {

  def apply()(implicit ctx: Context) = help.layout(
    title = "有延迟吗？",
    active = "lag",
    moreCss = cssTag("lag"),
    moreJs = frag(
      echartsTag,
      jsTag("lag.js")
    )
  ) {
      main(cls := "box box-pad lag")(
        h1(
          "有延迟吗？",
          span(cls := "answer short")(
            span(cls := "waiting")("检测中..."),
            span(cls := "nope-nope none")(strong("No."), " 您的网络很棒！"),
            span(cls := "nope-yep none")(strong("No."), " 但您的网络不够给力！"),
            span(cls := "yep none")(strong("Yes."), " 我们正尽力修复中！")
          )
        ),
        div(cls := "answer long")(
          "现在请看详细回答！对局中的延迟来自两个不同的因素（越低越好）："
        ),
        div(cls := "sections")(
          st.section(cls := "server")(
            h2("服务器延迟"),
            div(cls := "meter"),
            p(
              "服务器处理每一步棋需要时间。",
              "对所有人来说基本都是一样的，仅仅依赖服务器的负载。 ",
              "T在线的棋手越多，负载就会越大，但 Haichess 的工程师们 ",
              "尽最大努力降低这个延迟。时间很少会超过10毫秒。"
            )
          ),
          st.section(cls := "network")(
            h2("您和 Haichess 服务器之间的网络"),
            div(cls := "meter"),
            p(
              "您走一步棋通过网络传输到Haichess服务器所需的时间，",
              "还有您收到反馈的时间。",
              "这取决于您距离Haichess服务器（北京）的距离，还有 ",
              "您使用的网络的连接情况。",
              "我们已经让服务器在最好的网络中，没有办法解决您使用网络的问题，或者降低这个延迟。"
            )
          )
        ),
        div(cls := "last-word")(
          p("您可以点击屏幕右上您的用户名，来随时检查这两个延迟时间。"),
          h2("延迟补偿"),
          p(
            "我们会对网络产生的延迟给与补偿，最多每步棋1秒。",
            "在您走棋后，由于网络延迟浪费的时间会以平均值补偿到您的棋钟。 ",
            "所以，比对手拥有一个更好的网络（更低延迟），并不会在对局中成为有利的因素！"
          )
        )
      )
    }
}
