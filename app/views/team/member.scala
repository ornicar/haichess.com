package views.html.team

import play.api.data.{ Form, Field }
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.team.{ Team, Member, MemberWithUser }
import controllers.routes

object member {

  def apply(form: Form[_], team: Team, pager: Paginator[MemberWithUser], tags: List[lila.team.Tag], memberSearch: lila.team.MemberSearch)(implicit ctx: Context) =
    bits.layout(
      title = s"${team.name} 成员",
      evenMoreJs = frag(
        flatpickrTag,
        delayFlatpickrStart,
        jsTag("team.member.js")
      )
    ) {
        main(cls := "page-menu")(
          bits.menu(none),
          div(cls := "page-menu__content box member")(
            h1(a(href := routes.Team.show(team.id))(team.name), nbsp, em("成员")),
            st.form(
              rel := "nofollow",
              cls := "box__pad member__search",
              action := s"${routes.Team.members(team.id)}#results",
              method := "GET"
            )(
                table(
                  tr(
                    td(label("账号/备注")),
                    td(form3.input(form("username"))),
                    td(label("姓名")),
                    td(form3.input(form("name")))
                  ),
                  tr(
                    td(label("角色")),
                    td(form3.select(form("role"), Member.Role.list, "".some)),
                    td(label("年龄")),
                    td(form3.input(form("age"), typ = "number"))
                  ),
                  tr(
                    td(label("棋协级别")),
                    td(form3.select(form("level"), lila.user.FormSelect.Level.levelWithRating, "".some)),
                    td(label("性别")),
                    td(form3.select(form("sex"), lila.user.FormSelect.Sex.list, "".some))
                  ),
                  tags.filterNot(_.typ.range).zipWithIndex.map {
                    case (t, i) => buildSearchField(memberSearch, t, form(s"fields[$i]"))
                  },
                  tags.filter(_.typ.range).zipWithIndex.map {
                    case (t, i) => buildRangeSearchField(memberSearch, t, form(s"rangeFields[$i]"))
                  },
                  tr(
                    td,
                    td(colspan := 3, cls := "action")(
                      submitButton(cls := "button")(trans.search())
                    )
                  )
                )
              ),
            table(cls := "slist")(
              thead(
                tr(
                  th("账号"),
                  th("角色"),
                  th("姓名（备注）"),
                  th("等级分"),
                  th("性别"),
                  th("年龄"),
                  th("级别"),
                  tags.map { tag =>
                    th(tag.label)
                  },
                  th("操作")
                )
              ),
              if (pager.nbResults > 0) {
                tbody(cls := "infinitescroll")(
                  pagerNextTable(pager, np => nextPageUrl(form, team, np)),
                  pager.currentPageResults.map { mu =>
                    tr(cls := "paginated")(
                      td(userLink(mu.user)),
                      td(mu.member.role.name),
                      td(mu.viewName),
                      td(mu.member.rating.map(_.intValue.toString) | "-"),
                      td(mu.profile.ofSex.map(_.name) | "-"),
                      td(mu.profile.age.map(_.toString) | "-"),
                      td(mu.profile.ofLevel.name),
                      tags.map { tag =>
                        td(mu.member.tagValue(tag.field))
                      },
                      td(
                        a(cls := "button button-empty", href := "/inbox/new?user=" + mu.user.username)("发消息"),
                        a(cls := "button button-empty member-edit", href := routes.Team.editMemberModal(mu.member.id))("编辑"),
                        postForm(cls := "inline", action := routes.Team.kick(team.id))(
                          input(tpe := "hidden", name := "url", value := routes.Team.members(team.id, 1)),
                          !team.isCreator(mu.user.id) option button(name := "userId", title := "移除成员后不可恢复，是否继续？", cls := "button button-empty button-no-upper confirm", value := mu.user.id)("移除")
                        )
                      )
                    )
                  }
                )
              } else {
                tbody(
                  tr(
                    td(colspan := 3)("暂无记录")
                  )
                )
              }
            )
          )
        )
      }

  def nextPageUrl(form: Form[_], team: Team, np: Int)(implicit ctx: Context) = {
    var url: String = routes.Team.members(team.id, np).url
    form.data.foreach {
      case (key, value) => url = url.concat("&").concat(key).concat("=").concat(value)
    }
    url
  }

  def buildSearchField(memberSearch: lila.team.MemberSearch, tag: lila.team.Tag, form: Field)(implicit ctx: Context) = {
    val vl = memberSearch.fields.find(_.field == tag.field).??(_.value)
    tag.typ match {
      case lila.team.Tag.Type.Text => tr(
        td(label(`for` := form3.id(form(tag.field)))(tag.label)),
        td(colspan := 3)(
          form3.input2(form("fieldValue"), vl),
          form3.hidden(form("fieldName"), tag.field.some)
        )
      )
      case lila.team.Tag.Type.SingleChoice => tr(
        td(label(`for` := form3.id(form(tag.field)))(tag.label)),
        td(colspan := 3)(
          form3.select2(form("fieldValue"), vl, tag.toChoice, default = "".some),
          form3.hidden(form("fieldName"), tag.field.some)
        )
      )
      case _ => frag()
    }
  }

  def buildRangeSearchField(memberSearch: lila.team.MemberSearch, tag: lila.team.Tag, form: Field)(implicit ctx: Context) = {
    val rangeTag = memberSearch.rangeFields.find(_.field == tag.field)
    val min = rangeTag ?? { r => r.min }
    val max = rangeTag ?? { r => r.max }
    tag.typ match {
      case lila.team.Tag.Type.Number => tr(
        td(label(`for` := form3.id(form(tag.field)))(tag.label)),
        td(colspan := 3)(
          form3.hidden(form("fieldName"), tag.field.some),
          div(cls := "half")("从 ", form3.input2(form("min"), min, typ = "number")),
          div(cls := "half")("到 ", form3.input2(form("max"), max, typ = "number"))
        )
      )
      case lila.team.Tag.Type.Date => tr(
        td(label(`for` := form3.id(form(tag.field)))(tag.label)),
        td(colspan := 3)(
          form3.hidden(form("fieldName"), tag.field.some),
          div(cls := "half")("从 ", form3.input2(form("min"), min, klass = "flatpickr")),
          div(cls := "half")("到 ", form3.input2(form("max"), max, klass = "flatpickr"))
        )
      )
      case _ => frag()
    }
  }

  def edit(mu: MemberWithUser, tags: List[lila.team.Tag], form: Form[_])(implicit ctx: Context) = frag(
    div(cls := "modal-content none")(
      h2(mu.user.username),
      postForm(cls := "form3 member-editform", style := "text-align:left;", action := routes.Team.editMemberApply(mu.member.id))(
        form3.group(form("role"), "角色", klass = mu.member.isOwner ?? "none")(form3.radio(_, if (mu.member.isOwner) Member.Role.list else Member.Role.list.filterNot(_._1 == "owner"))),
        form3.group(form("mark"), "备注")(form3.input(_)),
        tags.zipWithIndex.map {
          case (t, i) => buildEditField(mu.member, t, form(s"fields[$i]"))
        },
        form3.globalError(form),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交")
        )
      )
    )
  )

  def buildEditField(m: Member, tag: lila.team.Tag, form: Field)(implicit ctx: Context) = {
    val vl = m.tagsIfEmpty.tagMap.get(tag.field) ?? (_.value)
    tag.typ match {
      case lila.team.Tag.Type.Text => form3.group(form("fieldValue"), raw(tag.label))(f => frag(
        form3.input2(f, vl),
        form3.hidden(form("fieldName"), tag.field.some)
      ))
      case lila.team.Tag.Type.Number => form3.group(form("fieldValue"), raw(tag.label))(f => frag(
        form3.input2(f, vl, typ = "number"),
        form3.hidden(form("fieldName"), tag.field.some)
      ))
      case lila.team.Tag.Type.Date => form3.group(form("fieldValue"), raw(tag.label))(f => frag(
        form3.input2(f, vl, klass = "flatpickr"),
        form3.hidden(form("fieldName"), tag.field.some)
      ))
      case lila.team.Tag.Type.SingleChoice => form3.group(form("fieldValue"), raw(tag.label))(f => frag(
        form3.select2(f, vl, tag.toChoice, default = "".some),
        form3.hidden(form("fieldName"), tag.field.some)
      ))
    }
  }

}
