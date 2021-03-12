package lila.app
package templating

import lila.app.ui.ScalatagsTemplate._
import lila.clazz.Env.{ current => clazzEnv }
import controllers.routes

trait ClazzHelper {

  private def api = clazzEnv.api

  def clazzLinkById(id: String, cssClass: Option[String] = None): Frag =
    api.byId(id).awaitSeconds(3).fold[Frag](span("班级已失效")) { clazz =>
      clazzLink(clazz)
    }

  def clazzLink(clazz: lila.clazz.Clazz, cssClass: Option[String] = None): Frag =
    a(cls := List("clazz-link" -> true, ~cssClass -> cssClass.isDefined), href := routes.Clazz.detail(clazz._id))(clazz.name)

}
