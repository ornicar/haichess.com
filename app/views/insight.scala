package views.html

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.user.User

import controllers.routes

object insight {

  def index(
    u: User,
    cache: lila.insight.UserCache,
    prefId: Int,
    ui: play.api.libs.json.JsObject,
    question: play.api.libs.json.JsObject,
    stale: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username}'s chess insights",
      moreJs = frag(
        echartsTag,
        jsAt("vendor/multiple-select/multiple-select.js"),
        jsAt(s"compiled/lichess.insight${isProd ?? (".min")}.js"),
        jsTag("insight-refresh.js"),
        jsTag("insight-tour.js"),
        embedJsUnsafe(s"""
$$(function() {
lichess = lichess || {};
lichess.insight = LichessInsight(document.getElementById('insight'), ${
          safeJsonValue(Json.obj(
            "ui" -> ui,
            "initialQuestion" -> question,
            "i18n" -> Json.obj(),
            "myUserId" -> ctx.userId,
            "user" -> Json.obj(
              "id" -> u.id,
              "name" -> u.username,
              "nbGames" -> cache.count,
              "stale" -> stale,
              "shareId" -> prefId
            ),
            "pageUrl" -> routes.Insight.index(u.username).url,
            "postUrl" -> routes.Insight.json(u.username).url
          ))
        });
});""")
      ),
      moreCss = cssTag("insight")
    )(frag(
        main(id := "insight"),
        stale option div(cls := "insight-stale none")(
          p("有新的对局可以加入洞察"),
          refreshForm(u, "刷新")
        )
      ))

  def empty(u: User)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username}'s chess insights",
      moreJs = jsTag("insight-refresh.js"),
      moreCss = cssTag("insight")
    )(
        main(cls := "box box-pad page-small")(
          h1(cls := "text", dataIcon := "7")(u.username, "洞察数据"),
          p({ u.username }, " 尚未生成洞察数据！"),
          refreshForm(u, s"生成${u.username}的洞察数据")
        )
      )

  def forbidden(u: User)(implicit ctx: Context) =
    views.html.site.message(
      title = s"${u.username}的洞察数据是受保护的",
      back = true,
      icon = "7".some
    )(
      p(style := "display:flex;")("对不起， 您不能查看 ", userLink(u, withOnline = false), "的洞察数据"),
      br,
      p(
        "或许可以让他们修改",
        a(href := routes.Pref.form("privacy"))("隐私设置"),
        " ？"
      )
    )

  def refreshForm(u: User, action: String)(implicit ctx: Context) =
    postForm(cls := "insight-refresh", attr("data-user") := u.id, st.action := routes.Insight.refresh(u.username))(
      button(dataIcon := "E", cls := "button text")(action),
      div(cls := "crunching none")(
        spinner,
        br,
        p(strong("立即为您处理数据！"))
      )
    )
}
