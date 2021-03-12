package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object permissions {

  def apply(u: lila.user.User)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username} permissions",
      moreCss = frag(
        cssTag("mod.communication"),
        cssTag("form3")
      ),
      moreJs = embedJsUnsafe("""$(function() {
$('button.clear').on('click', function() {
  $('#permissions option:selected').prop('selected', false);
});});""")
    ) {
        main(id := "permissions", cls := "page-small box box-pad")(
          div(userLink(u, cssClass = "large".some)),
          postForm(cls := "form3", action := routes.Mod.permissions(u.username))(
            select(name := "permissions[]", multiple)(
              lila.security.Permission.allButSuperAdmin.sortBy(_.name).flatMap { p =>
                ctx.me.exists(canGrant(_, p)) option option(
                  value := p.name,
                  u.roles.contains(p.name) option selected,
                  title := p.children.mkString(", ")
                )(p.toString)
              }
            ),
            p("使用 Ctrl 选择多个权限"),
            form3.actions(
              button(cls := "button button-red clear", tpe := "button")("清除"),
              submitButton(cls := "button")("保存")
            )
          )
        )
      }
}
