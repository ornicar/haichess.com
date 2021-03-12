package views.html.report

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object thanks {

  def apply(userId: String, blocked: Boolean)(implicit ctx: Context) = {

    val title = "感谢您反馈"

    val moreJs = embedJsUnsafe("""
$('button.report-block').one('click', function() {
var $button = $(this);
$button.find('span').text('屏蔽中...');
$.ajax({
url:$button.data('action'),
method:'post',
success: function() {
$button.find('span').text('已屏蔽！');
}
});
});
""")

    views.html.base.layout(title = title, moreJs = moreJs) {
      main(cls := "page-small box box-pad")(
        h1(title),
        p("管理员已收到您的举报信息，我们会尽快确认，并做相应处理！"),
        br, br,
        !blocked option p(
          "同时，您可以加入黑名单屏蔽该用户。",
          submitButton(
            attr("data-action") := routes.Relation.block(userId),
            cls := "report-block button",
            st.title := trans.block.txt()
          )(
              span(cls := "text", dataIcon := "k")("屏蔽 ", usernameOrId(userId))
            )
        ),
        br, br,
        p(
          a(href := routes.Home.home)("返回首页")
        )
      )

    }
  }
}
