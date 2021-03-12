package views.html.expense

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object vip {

  def apply()(implicit ctx: Context) = {
    views.html.base.layout(
      title = "VIP会员",
      moreCss = cssTag("expense")
    )(
        main(cls := "box")(
          table(cls := "vip")(
            tr(
              td,
              td(
                h1("注册会员")
              ),
              td(
                h1("VIP会员")
              )
            ),
            tr(
              td(
                h2("战术题数量")
              ),
              td(
                h3("5道/天")
              ),
              td(
                h3("无限制")
              )
            ),
            tr(
              td(
                h2("对局数量")
              ),
              td(
                h3("2盘/天")
              ),
              td(
                h3("无限制")
              )
            ),
            tr(
              td(
                h2("主题战术学习")
              ),
              td(
                h3("5道/天")
              ),
              td(
                h3("无限制")
              )
            ),
            tr(
              td(
                h2("战术题数量")
              ),
              td(
                h3("无")
              ),
              td(
                h3("有")
              )
            ),
            tr(
              td(
                h2("分析报告")
              ),
              td(
                h3("无")
              ),
              td(
                h3("有")
              )
            ),
            tr(
              td(
                h2("消费优惠")
              ),
              td(
                h3("无")
              ),
              td(
                h3("有")
              )
            ),
            tr(
              td(
                h2("价格")
              ),
              td(
                a(cls := "price", href := "/login")("免费（Free）")
              ),
              td(
                a(cls := "price", href := "/login")("699/年")
              )
            )
          )
        )
      )
  }
}
