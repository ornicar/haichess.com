package views.html
package forum

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object topic {

  def form(categ: lila.forum.Categ, form: Form[_], captcha: lila.common.Captcha)(implicit ctx: Context) =
    views.html.base.layout(
      title = "新建讨论区主题",
      moreCss = cssTag("forum"),
      moreJs = frag(
        jsTag("forum-post.js"),
        captchaTag
      )
    ) {
        main(cls := "forum forum-topic topic-form page-small box box-pad")(
          h1(
            a(href := routes.ForumCateg.show(categ.slug), dataIcon := "I", cls := "text"),
            categ.name
          ),
          /*          st.section(cls := "warning")(
            h2(dataIcon := "!", cls := "text")("重要提示"),
            p(
              "您的问题可能已经有了答案", strong(a(href := routes.Page.faq)("常见问题"))
            ),
            p(
              "举报用户作弊或不良行为", strong(a(href := routes.Report.form)("举报"))
            ),
            p(
              "请求帮助", strong(a(href := routes.Page.contact())(raw("联系我们")))
            )
          ),*/

          postForm(cls := "form3", action := routes.ForumTopic.create(categ.slug))(
            form3.group(form("name"), trans.subject())(form3.input(_)(autofocus)),
            form3.group(form("post")("text"), trans.message())(form3.textarea(_, klass = "post-text-area")(rows := 10)),
            views.html.base.captcha(form("post"), captcha),
            form3.actions(
              a(href := routes.ForumCateg.show(categ.slug))(trans.cancel()),
              isGranted(_.PublicMod) option
                form3.submit(frag("Create as mod"), nameValue = (form("post")("modIcon").name, "true").some, icon = "".some),
              form3.submit(trans.createTheTopic())
            )
          )
        )
      }

  def show(
    categ: lila.forum.Categ,
    topic: lila.forum.Topic,
    posts: Paginator[lila.forum.Post],
    formWithCaptcha: Option[FormWithCaptcha],
    unsub: Option[Boolean],
    canModCateg: Boolean
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${topic.name} • page ${posts.currentPage}/${posts.nbPages} • ${categ.name}",
    moreJs = frag(
      jsTag("forum-post.js"),
      formWithCaptcha.isDefined option captchaTag,
      jsAt("compiled/embed-analyse.js")
    ),
    moreCss = cssTag("forum"),
    openGraph = lila.app.ui.OpenGraph(
      title = topic.name,
      url = s"$netBaseUrl${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage).url}",
      description = shorten(posts.currentPageResults.headOption.??(_.text), 152)
    ).some
  ) {
      val pager = bits.pagination(routes.ForumTopic.show(categ.slug, topic.slug, 1), posts, showPost = true)

      main(cls := "forum forum-topic page-small box box-pad")(
        h1(
          a(
            href := routes.ForumCateg.show(categ.slug),
            dataIcon := "I",
            cls := "text"
          ),
          topic.name
        ),
        pager,
        div(cls := "forum-topic__posts embed_analyse")(
          posts.currentPageResults.map { p =>
            post.show(
              categ,
              topic,
              p,
              s"${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage)}#${p.number}",
              canModCateg = canModCateg
            )
          }
        ),

        div(cls := "forum-topic__actions")(
          if (posts.hasNextPage) emptyFrag
          else if (topic.isOld)
            p("此主题已存档，无法再评论。")
          else if (formWithCaptcha.isDefined)
            h2(id := "reply")(trans.replyToThisTopic())
          else if (topic.closed) p(trans.thisTopicIsNowClosed())
          else categ.team.filterNot(myTeam).map { teamId =>
            p(
              "加入 ",
              a(href := routes.Team.show(teamId))(teamIdToName(teamId), "俱乐部"),
              " 在本讨论区发帖"
            )
          } getOrElse p("您还不能在讨论区上发帖！"),
          div(
            unsub.map { uns =>
              postForm(cls := s"unsub ${if (uns) "on" else "off"}", action := routes.Timeline.unsub(s"forum:${topic.id}"))(
                button(cls := "button button-empty text on", dataIcon := "v", bits.dataUnsub := "off")("订阅"),
                button(cls := "button button-empty text off", dataIcon := "v", bits.dataUnsub := "on")("取消订阅")
              )
            },

            isGranted(_.ModerateForum) option
              postForm(action := routes.ForumTopic.hide(categ.slug, topic.slug))(
                button(cls := "button button-empty button-green")(if (topic.hidden) "Feature" else "Un-feature")
              ),
            canModCateg option
              postForm(action := routes.ForumTopic.close(categ.slug, topic.slug))(
                button(cls := "button button-empty button-red")(if (topic.closed) "打开" else "关闭")
              ),
            canModCateg option
              postForm(action := routes.ForumTopic.sticky(categ.slug, topic.slug))(
                button(cls := "button button-empty button-brag")(if (topic.isSticky) "取消置顶" else "置顶")
              )
          )
        ),

        formWithCaptcha.map {
          case (form, captcha) => postForm(
            cls := "form3 reply",
            action := s"${routes.ForumPost.create(categ.slug, topic.slug, posts.currentPage)}#reply",
            novalidate
          )(
              form3.group(form("text"), trans.message()) { f =>
                form3.textarea(f, klass = "post-text-area")(rows := 10, bits.dataTopic := topic.id)
              },
              views.html.base.captcha(form, captcha),
              form3.actions(
                a(href := routes.ForumCateg.show(categ.slug))(trans.cancel()),
                isGranted(_.PublicMod) option
                  form3.submit(frag("Reply as mod"), nameValue = (form("modIcon").name, "true").some, icon = "".some),
                form3.submit(trans.reply())
              )
            )
        },

        pager
      )
    }
}
