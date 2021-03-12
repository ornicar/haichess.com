package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.team.Team
import lila.team.Certification
import controllers.routes
import play.api.data.Form

object mod {

  def list(pager: Paginator[Team], status: Certification.Status)(implicit ctx: Context) =
    views.html.base.layout(
      title = "俱乐部认证",
      moreCss = cssTag("mod.team"),
      moreJs = infiniteScrollTag
    ) {
        main(cls := "page-menu")(
          views.html.mod.menu("team"),
          div(cls := "page-menu__content box")(
            div(cls := "box__top")(
              h1("俱乐部认证"),
              div(cls := "box__top__actions")(
                views.html.base.bits.mselect(
                  "team-status",
                  status.name,
                  Certification.Status.all map { s =>
                    a(href := routes.TeamCertification.modList(pager.currentPage, s.id), cls := (status == s).option("current"))(s.name)
                  }
                )
              )
            ),
            table(cls := "slist")(
              thead(
                tr(
                  th("名称"),
                  th("申请人"),
                  th("申请日期"),
                  th("状态"),
                  th("可用"),
                  th("操作")
                )
              ),
              if (pager.nbResults > 0) {
                tbody(cls := "infinitescroll")(
                  pagerNextTable(pager, np => routes.TeamCertification.modList(np, status.id).url),
                  pager.currentPageResults.map { t =>
                    val cert = t.certification.get
                    tr(cls := "paginated")(
                      td(t.name),
                      td(userIdLink(t.createdBy.some, params = "?mod")),
                      td(cert.applyAt.toString("yyyy-MM-dd")),
                      td(cert.status.name),
                      td(if (t.open) "是" else "否"),
                      td(
                        a(cls := "button button-empty", href := routes.TeamCertification.modDetail(t.id))("详情")
                      )
                    )
                  }
                )
              } else {
                tbody(
                  tr(
                    td(colspan := 6)("暂无申请")
                  )
                )
              }
            )
          )
        )
      }

  def detail(team: Team, form: Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = "俱乐部认证",
      moreCss = cssTag("mod.team")
    ) {
        val cert = team.certification.get
        main(cls := "page-menu")(
          views.html.mod.menu("team"),
          div(cls := "page-menu__content box")(
            div(cls := "box__top")(
              h1("俱乐部认证")
            ),
            postForm(cls := "form3", action := routes.TeamCertification.processCertification(team.id))(
              div(cls := "form3 box-pad team-detail")(
                table(
                  tr(
                    th("名称"),
                    td(teamLink(team))
                  ),
                  tr(
                    th("省市"),
                    td(team.location)
                  ),
                  tr(
                    th("详细地址"),
                    td(cert.addr)
                  ),
                  tr(
                    th("会员人数"),
                    td(cert.members)
                  ),
                  tr(
                    th("注册单位名称"),
                    td(cert.org)
                  ),
                  tr(
                    th("负责人"),
                    td(cert.leader)
                  ),
                  tr(
                    th("手机号码"),
                    td(cert.leaderContact)
                  ),
                  tr(
                    th("留言"),
                    td(cert.message)
                  ),
                  tr(
                    th("营业执照"),
                    td(img(src := dbImageUrl(cert.businessLicense)))
                  ),
                  tr(
                    th("备注"),
                    td(
                      form3.textarea(form("comments"), vl = cert.processComments)()
                    )
                  )
                ),
                form3.actions(
                  a(cls := "cancel", href := routes.TeamCertification.modList(1, Certification.Status.Applying.id))("返回"),
                  div(cls := "btn-group")(
                    button(name := "process", value := "approve", cls := List("button confirm" -> true, "disabled" -> !(team.enabled && cert.status.applying)), !(team.enabled && cert.status.applying) option disabled)("通过"),
                    button(name := "process", value := "reject", cls := List("button button-red confirm" -> true, "disabled" -> !(team.enabled && cert.status.applying)), !(team.enabled && cert.status.applying) option disabled)("拒绝")
                  )
                )
              )
            )
          )
        )
      }
}
