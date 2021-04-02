package views.html.team

import play.api.data.{ Field, Form }
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.team.RatingSetting
import lila.team.Team
import controllers.routes

object request {

  def requestForm(t: lila.team.Team, form: Form[_], captcha: lila.common.Captcha)(implicit ctx: Context) = {

    val title = s"${trans.joinTeam.txt()} ${t.name}"

    views.html.base.layout(
      title = title,
      moreCss = cssTag("team"),
      moreJs = frag(infiniteScrollTag, captchaTag)
    ) {
        main(cls := "page-menu page-small")(
          bits.menu("requests".some),
          div(cls := "page-menu__content box box-pad")(
            h1(title),
            p(style := "margin:2em 0")(richText(t.description)),
            postForm(cls := "form3", action := routes.Team.requestCreate(t.id))(
              form3.group(form("message"), raw("消息"))(form3.textarea(_)()),
              p(cls := "is-gold", dataIcon := "")(b("加入俱乐部，将同步您的姓名、性别、出生年份和当前级别给俱乐部。")),
              views.html.base.captcha(form, captcha),
              form3.actions(
                a(href := routes.Team.show(t.slug))(trans.cancel()),
                form3.submit(trans.joinTeam())
              )
            )
          )
        )
      }
  }

  def all(requests: List[lila.team.RequestWithUser])(implicit ctx: Context) = {
    val title = s"${requests.size} 加入请求"
    bits.layout(
      title = title,
      evenMoreJs = frag(
        flatpickrTag,
        jsTag("team.member.js")
      )
    ) {
        main(cls := "page-menu")(
          bits.menu("requests".some),
          div(cls := "page-menu__content box box-pad")(
            h1(title),
            list(requests, none)
          )
        )
      }
  }

  private[team] def list(requests: List[lila.team.RequestWithUser], t: Option[lila.team.Team])(implicit ctx: Context) = {
    val url = t.fold(routes.Team.requests())(te => routes.Team.show(te.id)).toString
    table(cls := "slist requests @if(t.isEmpty){all}else{for-team} datatable")(
      tbody(
        requests.map { request =>
          tr(
            td(userLink(request.user)),
            t.isEmpty option td(teamLink(request.team)),
            td(richText(request.message)),
            td(momentFromNow(request.date)),
            td(cls := "process")(
              postForm(cls := "process-request", action := routes.Team.requestProcess(request.id))(
                input(tpe := "hidden", name := "url", value := url),
                button(name := "process", cls := "button button-empty button-red", value := "decline")(trans.decline()),
                if (request.team.tagTip) {
                  a(cls := "button button-green member-accept", href := routes.Team.acceptMemberModal(request.id, url))(trans.accept())
                } else {
                  button(name := "process", cls := "button button-green", value := "accept")(trans.accept())
                }
              )
            )
          )
        }
      )
    )
  }

  def accept(team: Team, requestId: String, requestUser: Option[lila.user.User], referrer: String, tags: List[lila.team.Tag], form: Form[_])(implicit ctx: Context) = frag(
    div(cls := "modal-content none")(
      h2("接受请求"),
      p("如果不想默认添加标签，您可以", a(href := routes.Team.setting(team.id))("设置")),
      postForm(cls := "form3 member-editform", style := "text-align:left;", action := routes.Team.acceptMemberApply(requestId, referrer))(
        form3.group(form("mark"), "备注")(form3.input2(_, vl = requestUser.fold(none[String]) { u => u.profileOrDefault.realName })),
        form3.group(form("rating"), "初始等级分")(form3.input2(_, typ = "number", vl = team.ratingSettingOrDefault.toString.some)),
        tags.zipWithIndex.map {
          case (t, i) => buildAcceptField(t, form(s"fields[$i]"))
        },
        form3.globalError(form),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("保存并接受")
        )
      )
    )
  )

  def buildAcceptField(tag: lila.team.Tag, form: Field)(implicit ctx: Context) = {
    tag.typ match {
      case lila.team.Tag.Type.Text => form3.group(form("fieldValue"), raw(tag.label))(f => frag(
        form3.input(f),
        form3.hidden(form("fieldName"), tag.field.some)
      ))
      case lila.team.Tag.Type.Number => form3.group(form("fieldValue"), raw(tag.label))(f => frag(
        form3.input(f, typ = "number"),
        form3.hidden(form("fieldName"), tag.field.some)
      ))
      case lila.team.Tag.Type.Date => form3.group(form("fieldValue"), raw(tag.label))(f => frag(
        form3.input(f, klass = "flatpickr"),
        form3.hidden(form("fieldName"), tag.field.some)
      ))
      case lila.team.Tag.Type.SingleChoice => form3.group(form("fieldValue"), raw(tag.label))(f => frag(
        form3.select(f, tag.toChoice, default = "".some),
        form3.hidden(form("fieldName"), tag.field.some)
      ))
    }
  }

}
