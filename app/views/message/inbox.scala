package views.html.message

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object inbox {

  def apply(me: lila.user.User, threads: Paginator[lila.message.Thread])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.inbox.txt(),
      moreCss = cssTag("message"),
      moreJs = frag(infiniteScrollTag, jsTag("message.js"))
    ) {
        main(cls := "message-list box")(
          div(cls := "box__top")(
            h1(trans.inbox()),
            div(cls := "box__top__actions")(
              threads.nbResults > 0 option frag(
                select(cls := "select")(
                  option(value := "")("选择"),
                  option(value := "all")("选中所有"),
                  option(value := "none")("取消选择"),
                  option(value := "unread")("所有未读"),
                  option(value := "read")("所有已读")
                ),
                select(cls := "action")(
                  option(value := "")("操作"),
                  option(value := "unread")("标记为未读"),
                  option(value := "read")("标记为已读"),
                  option(value := "delete")("删除")
                )
              ),
              a(href := routes.Message.form, cls := "button button-green text", dataIcon := "m")(trans.composeMessage())
            )
          ),
          table(cls := "slist slist-pad")(
            if (threads.nbResults > 0) tbody(cls := "infinitescroll")(
              pagerNextTable(threads, p => routes.Message.inbox(p).url),
              threads.currentPageResults.map { thread =>
                tr(cls := List(
                  "paginated" -> true,
                  "new" -> thread.isUnReadBy(me),
                  "mod" -> thread.asMod
                ))(
                  td(cls := "author")(userIdLink(thread.visibleOtherUserId(me), none)),
                  td(cls := "subject")(a(href := s"${routes.Message.thread(thread.id)}#bottom")(thread.name)),
                  td(cls := "date")(momentFromNow(thread.updatedAt)),
                  td(cls := "check")(input(tpe := "checkbox", name := "threads", value := thread.id))
                )
              }
            )
            else tbody(tr(td(trans.noNewMessages(), br, br)))
          )
        )
      }
}
