package views.html.offlineContest

import play.api.data.Form
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.offlineContest.OffContest
import lila.clazz.Clazz
import play.api.libs.json._
import play.api.mvc.Call
import controllers.routes

object form {

  def create(form: Form[_], teams: List[lila.team.Team], clazzs: List[(Clazz, Boolean)])(implicit ctx: Context) =
    layout(
      form,
      teams,
      clazzs,
      "创建比赛编排",
      routes.OffContest.create
    )

  def update(id: OffContest.ID, form: Form[_], teams: List[lila.team.Team], clazzs: List[(Clazz, Boolean)])(implicit ctx: Context) =
    layout(
      form,
      teams,
      clazzs,
      "编辑比赛编排",
      routes.OffContest.update(id)
    )

  val dataTeamInner = attr("data-team-inner")
  val dataClazzInner = attr("data-clazz-inner")
  val dataPublic = attr("data-public")
  def layout(form: Form[_], teams: List[lila.team.Team], clazzs: List[(Clazz, Boolean)], title: String, url: Call)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = cssTag("offlineContest"),
    moreJs = frag(
      singleUploaderTag,
      jsTag("offlineContest.form.js")
    )
  )(main(cls := "page-small")(
      div(
        cls := "contest__form box box-pad",
        dataPublic := JsArray(teams.filter(_.certified).map(t => Json.obj("id" -> t.id, "name" -> t.name, "teamRated" -> t.ratingSettingOrDefault.open))).toString,
        dataTeamInner := JsArray(teams.map(t => Json.obj("id" -> t.id, "name" -> t.name, "teamRated" -> t.ratingSettingOrDefault.open))).toString,
        dataClazzInner := JsArray(clazzs.map(c => Json.obj("id" -> c._1.id, "name" -> c._1.name, "teamRated" -> c._2))).toString
      )(
          h1(title),
          postForm(cls := "form3", action := url)(
            div(cls := "top")(
              form3.group(form("logo"), raw("Logo"), klass = "logo")(form3.singleImage(_, "上传LOGO"))
            ),
            form3.split(
              form3.group(form("name"), raw("比赛名称"), half = true)(form3.input(_)),
              form3.group(form("groupName"), raw("组别（可选）"), half = true)(form3.input(_))
            ),
            form3.split(
              {
                def available = ctx.me.?? { user =>
                  List(
                    OffContest.Type.Public -> user.hasTeam,
                    OffContest.Type.TeamInner -> user.hasTeam,
                    OffContest.Type.ClazzInner -> isGranted(_.Coach, user)
                  ).filter(_._2).map { t => t._1.id -> t._1.name }
                }
                val baseField = form("typ")
                val field = ctx.req.queryString get "team" flatMap (_.headOption) match {
                  case None => baseField
                  case Some(_) => baseField.copy(value = "team-inner".some)
                }
                form3.group(field, raw("比赛类型"), half = true)(form3.select(_, available))
              },
              form3.group(form("organizer"), raw("主办方"), half = true)(f => frag(
                form3.select(f, teams.map(t => t.id -> t.name)),
                form3.hidden("organizerSelected", f.value | "")
              ))
            ),
            form3.checkbox(form("teamRated"), raw("记录俱乐部等级分"), help = raw("影响棋手所在俱乐部的积分").some, klass = "none"),
            form3.split(
              form3.group(form("rule"), raw("赛制"), half = true)(form3.select(_, OffContest.Rule.list)),
              form3.group(form("rounds"), raw("轮次"), half = true)(form3.input(_, typ = "number"))
            ),
            div(cls := "form-group")(
              label(cls := "form-label")("破同分规则"),
              div(
                div(cls := "btss-wrap")(
                  label("瑞士制"),
                  div(cls := "btss-list")(
                    span(cls := "btss")("对手分"),
                    span(cls := "btss")("中间分"),
                    span(cls := "btss")("胜局数")
                  ),
                  form3.hidden("swissBtss[0]", "opponent"),
                  form3.hidden("swissBtss[1]", "mid"),
                  form3.hidden("swissBtss[2]", "vict")
                ) /*,
                  div(cls := "btss-wrap")(
                    label("循环赛"),
                    div(cls := "btss-list")(
                      span(cls := "btss")("直胜"),
                      span(cls := "btss")("胜局数"),
                      span(cls := "btss")("索伯分")
                    ),
                    form3.hidden("roundRobinBtss[0]", "res"),
                    form3.hidden("roundRobinBtss[1]", "vict"),
                    form3.hidden("roundRobinBtss[2]", "sauber")
                  )*/
              )
            ),
            form3.globalError(form),
            form3.actions(
              a(href := routes.OffContest.home())(trans.cancel()),
              form3.submit("保存", icon = "g".some)
            )
          )
        )
    ))

}
