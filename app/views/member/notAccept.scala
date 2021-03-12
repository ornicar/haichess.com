package views.html.member

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object notAccept {

  def apply()(implicit ctx: Context) = views.html.base.layout(
    title = "每日试用次数超过上限",
    moreJs = embedJsUnsafe(
      """$(function() {
window.lichess.memberIntro();
});"""
    )
  ) {
      frag()
    }

}
