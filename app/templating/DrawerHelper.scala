package lila.app
package templating

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._

trait DrawerHelper {

  val drawerHide = attr("drawer-hide")

  def drawer(toggleName: String, headerName: String)(bodyFrag: Frag)(implicit ctx: Context): Frag = frag {
    div(cls := "drawer", style := "display:none")(
      div(cls := "drawer-mask"),
      div(cls := "drawer-content-wrapper")(
        a(cls := "drawer-toggle", drawerHide := true)(toggleName),
        div(cls := "drawer-content")(
          div(cls := "drawer-wrapper-body")(
            div(cls := "drawer-header")(
              span(cls := "drawer-title")(headerName)
            ),
            div(cls := "drawer-body")(
              bodyFrag
            )
          )
        )
      )
    )
  }

}
