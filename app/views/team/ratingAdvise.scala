package views.html.team

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.app.templating.Environment._

object ratingAdvisePage {

  def apply()(implicit ctx: Context) = views.html.base.layout(
    title = "使用建议",
    moreCss = cssTag("team")
  )(
      main(cls := "page-small box box-pad ratingAdvise")(
        h1("俱乐部等级分使用建议"),
        div(cls := "s1")(
          div(cls := "s2")(
            div(cls := "h2")("1. 理解系统的计分规则后，再开启计算内部等级分的设置；")
          ),
          div(cls := "s2")(
            div(cls := "h2")("2. 根据自己俱乐部的特点，和对等级分管理的需求，对如何在俱乐部内使用进行规划；")
          ),
          div(cls := "s2")(
            div(cls := "h2")("3. 建议俱乐部指定自己的计分规则，并形成说明，在俱乐部讨论区发布，并置顶；")
          ),
          div(cls := "s2")(
            div(cls := "h2")("4. 开启内部等级分以后，虽然棋手都会有默认的等级分，但为了让成员有更好的体验，在新成员加入俱乐部时，手工为其指定一个适当的等级分；")
          ),
          div(cls := "s2")(
            div(cls := "h2")("5 等级分参考"),
            div(cls := "tb")(
              table(
                thead(
                  tr(th("级别"), th("使用建议"))
                ),
                tbody(
                  tr(td("特级大师"), td("2500")),
                  tr(td("国际大师"), td("2400")),
                  tr(td("健将等于国家大师"), td("2300")),
                  tr(td("一级运动员"), td("2100")),
                  tr(td("二级运动员约等于强棋协大师"), td("1900")),
                  tr(td("棋协大师"), td("1800")),
                  tr(td("候补棋协大师"), td("1700")),
                  tr(td("一级"), td("1500")),
                  tr(td("二级"), td("1300")),
                  tr(td("三级"), td("1200")),
                  tr(td("四级"), td("1100")),
                  tr(td("五级"), td("1000")),
                  tr(td("六级"), td("900")),
                  tr(td("七级"), td("800")),
                  tr(td("八级"), td("750")),
                  tr(td("九级"), td("700")),
                  tr(td("十级及以下"), td("600"))
                )
              )
            )
          )
        )
      )
    )
}

