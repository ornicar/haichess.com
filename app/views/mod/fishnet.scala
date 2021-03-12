package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.fishnet.Client
import controllers.routes

object fishnet {

  def apply(clients: List[Client])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Fishnet",
      moreCss = cssTag("mod.fishnet")
    ) {
        main(cls := "page-menu")(
          views.html.mod.menu("fishnet"),
          div(cls := "page-menu__content box")(
            div(cls := "box__top")(
              h1("Fishnet"),
              div(cls := "box__top__actions")()
            ),
            table(cls := "slist")(
              thead(
                tr(
                  th("ID"),
                  th("用户"),
                  th("角色"),
                  th("注册日期"),
                  th("stockfish")
                )
              ),
              tbody(
                clients map { c =>
                  tr(
                    td(c._id.value),
                    td(c.userId.value),
                    td(c.skill.key),
                    td(momentFromNow(c.createdAt)),
                    td(c.instance.map(toDesc))
                  )
                }
              )
            )
          )
        )
      }

  def toDesc(ins: Client.Instance) = frag(
    p("version：", ins.version.value),
    p("python：", ins.python.value),
    p("engines：", ins.engines.stockfish.name),
    p("ip：", ins.ip.value),
    p("seenAt：", strong(momentFromNow(ins.seenAt)))
  )

}
