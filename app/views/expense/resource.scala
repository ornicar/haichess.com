package views.html.expense

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object resource {

  def apply()(implicit ctx: Context) = {
    views.html.base.layout(
      title = "收费文章",
      moreCss = cssTag("expense")
    )(
        main(cls := "box")(
          table(cls := "resource")(
            tr(
              td(cls := "subject")(
                h2(
                  a(href := "/login")("20个开局陷阱")
                ),
                p("开局陷阱，也称为“开局圈套”，是指在开局阶段一方以兵或者子作为诱饵，引诱对方上钩以获取子力或者局面优势的欺骗性着法。开局陷阱往往违反开局的战略原则，若对方不上钩极有可能反噬自身，损害己方的局面。")
              ),
              td(cls := "price")("68元/38元(VIP)")
            ),
            tr(
              td(cls := "subject")(
                h2(
                  a(href := "/login")("国际象棋流芳百世的名局欣赏")
                ),
                p("能够流芳百世的棋局很多，作者选了20个对局，其出发点是每局棋里都有很美的想法，通过实现这些美好的想法，我们可以看到伟大的棋手们是怎样把我们现在学的基本理论运用到他们的棋局中。而我们通过反复观看他们的对局，可以加深我们的基本理论修养、拓展我们的思维空间、学习创造自己的美好的想法。")
              ),
              td(cls := "price")("58元/28元(VIP)")
            ),
            tr(
              td(cls := "subject")(
                h2(
                  a(href := "/login")("100个中局妙手")
                ),
                p("中局中双方激烈厮杀，你来我往，在危机中潜藏着机遇。本文精选100个国际大赛中的名局中局，重点分析在受到攻击时应如何发起反击，争取先手。适合中高级水平学习。")
              ),
              td(cls := "price")("88元/58元(VIP)")
            ),
            tr(
              td(cls := "subject")(
                h2(
                  a(href := "/login")("国际象棋的技术与战术")
                ),
                p("国际象棋中技术与战术如何区分？区分技术与战术对理解棋理，提升棋力有什么帮助？如何正确理解技术与战术，什么又是战术组合？")
              ),
              td(cls := "price")("88元/58元(VIP)")
            ),
            tr(
              td(cls := "subject")(
                h2(
                  a(href := "/login")("大师之路")
                ),
                p("成为国际象棋大师是众多国象爱好者的梦想，如何成为大师，需要具备哪些知识和理论，经过怎样的训练，有什么高效的训练方法。5年训练计划，可根据基础调整学习。")
              ),
              td(cls := "price")("68元/38元(VIP)")
            )
          )
        )
      )
  }
}
