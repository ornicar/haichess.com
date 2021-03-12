package views.html
package coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.coach.Certify.Status
import lila.user.FormSelect
import controllers.routes

object mod {

  def apply(pager: Paginator[lila.coach.Coach.WithUser], status: Status)(implicit ctx: Context) =
    views.html.base.layout(
      title = "教练认证",
      moreCss = cssTag("mod.coach"),
      moreJs = infiniteScrollTag
    ) {
        main(cls := "page-menu")(
          views.html.mod.menu("coach"),
          div(cls := "page-menu__content box")(
            div(cls := "box__top")(
              h1("教练认证"),
              div(cls := "box__top__actions")(
                views.html.base.bits.mselect(
                  "coach-sort",
                  status.name,
                  Status.all map { s =>
                    a(href := routes.Coach.modList(pager.currentPage, s.id), cls := (status == s).option("current"))(s.name)
                  }
                )
              )
            ),
            table(cls := "slist")(
              thead(
                tr(
                  th("账号"),
                  th("姓名"),
                  th("状态"),
                  th("申请日期"),
                  th("操作")
                )
              ),
              if (pager.nbResults > 0) {
                tbody(cls := "infinitescroll")(
                  pagerNextTable(pager, np => routes.Coach.modList(np, status.id).url),
                  pager.currentPageResults.map { c =>
                    tr(cls := "paginated")(
                      td(
                        userIdLink(c.coach._id.value.some, params = "?mod")
                      ),
                      td(c.user.realNameOrUsername),
                      td(c.certify.status.map(_.name) | "-"),
                      td(c.certify.applyAt.map(_.toString("yyyy-MM-dd")) | "-"),
                      td(
                        a(cls := "button button-empty", href := routes.Coach.modDetail(c.coach._id.value))("详情")
                      )
                    )
                  }
                )
              } else {
                tbody(
                  tr(
                    td(colspan := 5)("暂无申请")
                  )
                )
              }
            )
          )
        )
      }

  def detail(c: lila.coach.Coach.WithUser, error: Option[String] = None)(implicit ctx: Context) =
    views.html.base.layout(
      title = "教练认证",
      moreCss = cssTag("mod.coach"),
      moreJs = infiniteScrollTag
    ) {
        val profile = c.user.profileOrDefault
        val certify = c.certify

        main(cls := "page-menu")(
          views.html.mod.menu("coach"),
          div(cls := "page-menu__content box")(
            div(cls := "box__top")(
              h1("教练认证")
            ),
            postForm(cls := "form3 coach-detail box-pad", action := routes.Coach.modRejected(c.coach._id.value))(
              st.section(
                h2("基本信息"),
                table(
                  tr(
                    th("姓名"),
                    td(style := "display:flex")(c.user.realNameOrUsername, userIdLink(c.user.id.some, params = "?mod")),
                    th("当前级别"),
                    td(c.user.profileOrDefault.currentLevel.label)
                  ),
                  tr(
                    th("性别"),
                    td(
                      profile.sex.fold("-") { sex =>
                        FormSelect.Sex.name(sex)
                      }
                    ),
                    th("出生年份"),
                    td(profile.birthyear)
                  ),
                  tr(
                    th("手机"),
                    td(c.user.cellphone),
                    th("微信"),
                    td(profile.wechat)
                  ),
                  tr(
                    th("省市"),
                    td(colspan := 3)(
                      profile.location
                    )
                  ),
                  tr(
                    th("个人简介"),
                    td(colspan := 3)(
                      profile.nonEmptyBio
                    )
                  )
                )
              ),
              st.section(
                h2("认证信息"),
                table(
                  tr(
                    th("身份证号"),
                    td(certify.certNo)
                  ),
                  tr(
                    th("实名认证"),
                    td(
                      if (certify.overPassed) "通过" else "失败"
                    )
                  )
                )
              ),
              error.map { e =>
                p(cls := "error")(e)
              },
              form3.actions(
                a(cls := "cancel", href := routes.Coach.modList(1, "applying"))("返回"),
                a(cls := List("button" -> true, "disabled" -> certify.rejected), href := routes.Mod.permissions(c.user.username))("授权/移除"),
                button(tpe := "submit", cls := List("button button-red" -> true, "disabled" -> !certify.applying), !certify.applying option disabled)("拒绝（重新认证）")
              )
            )
          )
        )
      }
}
