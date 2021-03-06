package controllers

import lila.app._
import views._

object Page extends LilaController {

  val thanks = helpBookmark("thanks")
  val help = helpBookmark("help")
  val master = helpBookmark("master")

  def about = Open { implicit ctx =>
    pageHit
    Ok(views.html.site.help.about()).fuccess
  }

  def faq = Open { implicit ctx =>
    pageHit
    Ok(views.html.site.help.faq()).fuccess
  }

  def contact = Open { implicit ctx =>
    pageHit
    Ok(views.html.site.help.contact()).fuccess
  }

  def privacy = Open { implicit ctx =>
    pageHit
    Ok(views.html.site.help.privacy()).fuccess
  }

  def tos = Open { implicit ctx =>
    pageHit
    Ok(views.html.site.help.tos()).fuccess
  }

  private def helpBookmark(name: String) = Open { implicit ctx =>
    pageHit
    Ok(views.html.site.help.page(name)).fuccess
  }

  /*  private def helpBookmark(name: String) = Open { implicit ctx =>
      pageHit
      OptionOk(Prismic getBookmark name) {
        case (doc, resolver) => views.html.site.help.page(name, doc, resolver)
      }
    }*/

  val howToCheat = bookmark("how-to-cheat")

  private def bookmark(name: String) = Open { implicit ctx =>
    pageHit
    OptionOk(Prismic getBookmark name) {
      case (doc, resolver) => views.html.site.page(doc, resolver)
    }
  }

  def source = Open { implicit ctx =>
    pageHit
    Ok(views.html.site.help.source).fuccess
  }

  def swag = Open { implicit ctx =>
    pageHit
    OptionOk(Prismic getBookmark "swag") {
      case (doc, resolver) => views.html.site.swag(doc, resolver)
    }
  }

  def variantHome = Open { implicit ctx =>
    import play.api.libs.json._
    negotiate(
      html = OptionOk(Prismic getBookmark "variant") {
        case (doc, resolver) => views.html.site.variant.home(doc, resolver)
      },
      api = _ => Ok(JsArray(
        List(
          Json.obj(
            "id" -> chess.variant.Standard.id,
            "key" -> chess.variant.Standard.name,
            "name" -> "标准国际象棋"
          )
        )
      )).fuccess
    /*      api = _ => Ok(JsArray(chess.variant.Variant.all.map { v =>
        Json.obj(
          "id" -> v.id,
          "key" -> v.key,
          "name" -> v.name
        )
      })).fuccess*/
    )
  }

  def variant(key: String) = Open { implicit ctx =>
    (for {
      variant <- chess.variant.Variant.byKey get key
      perfType <- lila.rating.PerfType byVariant variant
    } yield OptionOk(Prismic getVariant variant) {
      case (doc, resolver) => views.html.site.variant.show(doc, resolver, variant, perfType)
    }) | notFound
  }
}
