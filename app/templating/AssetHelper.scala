package lila.app
package templating

import controllers.routes
import play.api.mvc.RequestHeader

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.common.{ Nonce, AssetVersion, ContentSecurityPolicy }

trait AssetHelper { self: I18nHelper with SecurityHelper =>

  def isProd: Boolean

  val siteDomain = lila.api.Env.current.Net.Domain
  val assetDomain = lila.api.Env.current.Net.AssetDomain
  val socketDomain = lila.api.Env.current.Net.SocketDomain
  val remoteSocketDomain = lila.api.Env.current.Net.RemoteSocketDomain
  val vapidPublicKey = lila.push.Env.current.WebVapidPublicKey

  val sameAssetDomain = siteDomain == assetDomain

  val assetBaseUrl = s"//$assetDomain"

  def assetVersion = AssetVersion.current

  def assetUrl(path: String): String = s"$assetBaseUrl/assets/_$assetVersion/$path"

  def cdnUrl(path: String) = s"$assetBaseUrl$path"
  def staticUrl(path: String) = s"$assetBaseUrl/assets/$path"

  def dbImageUrl(path: String) = s"$assetBaseUrl/image/$path"

  def cssTag(name: String)(implicit ctx: Context): Frag =
    cssTagWithTheme(name, ctx.currentBg)

  def cssTagWithTheme(name: String, theme: String): Frag =
    cssAt(s"css/$name.$theme.${if (isProd) "min" else "dev"}.css")

  def cssTagNoTheme(name: String)(implicit ctx: Context): Frag =
    cssAt(s"css/$name.${if (isProd) "min" else "dev"}.css")

  private def cssAt(path: String): Frag =
    link(href := assetUrl(path), tpe := "text/css", rel := "stylesheet")

  def jsTag(name: String, defer: Boolean = false): Frag =
    jsAt("javascripts/" + name, defer = defer)

  /* about async & defer, see https://flaviocopes.com/javascript-async-defer/
   * we want defer only, to ensure scripts are executed in order of declaration,
   * so that round.js doesn't run before site.js */
  def jsAt(path: String, defer: Boolean = false): Frag = script(
    defer option deferAttr,
    src := assetUrl(path)
  )

  val jQueryTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.min.js")}"></script>"""
  }

  def roundTag = jsAt(s"compiled/lichess.round${isProd ?? (".min")}.js", defer = true)
  def roundNvuiTag(implicit ctx: Context) = ctx.blind option
    jsAt(s"compiled/lichess.round.nvui.min.js", defer = true)

  def analyseTag = jsAt(s"compiled/lichess.analyse${isProd ?? (".min")}.js")
  def analyseNvuiTag(implicit ctx: Context) = ctx.blind option
    jsAt(s"compiled/lichess.analyse.nvui.min.js")

  def captchaTag = jsAt(s"compiled/captcha.js")
  def smsCaptchaTag = jsAt(s"compiled/smsCaptcha.js")
  def provinceCascadeTag = jsAt(s"compiled/province-cascade.js")

  val fingerprintTag = raw {
    s"""<script async src="${staticUrl("javascripts/vendor/fp2.min.js")}"></script>"""
  }

  val flatpickrTag = raw {
    s"""<script defer src="${staticUrl("javascripts/vendor/flatpickr.min.js")}"></script>"""
  }

  val nonAsyncFlatpickrTag = raw {
    s"""<script defer src="${staticUrl("javascripts/vendor/flatpickr.min.js")}"></script>"""
  }

  def delayFlatpickrStart(implicit ctx: Context) = embedJsUnsafe {
    """$(function() { setTimeout(function() { $(".flatpickr").flatpickr(); }, 500) });"""
  }

  val infiniteScrollTag = jsTag("vendor/jquery.infinitescroll.min.js")

  def prismicJs(implicit ctx: Context): Frag = raw {
    isGranted(_.Prismic) ?? {
      embedJsUnsafe("""window.prismic={endpoint:'https://lichess.prismic.io/api/v2'}""").render ++
        """<script type="text/javascript" src="//static.cdn.prismic.io/prismic.min.js"></script>"""
    }
  }

  val tagsinputTag = raw {
    s"""<script defer src="${staticUrl("javascripts/vendor/jquery.tagsinput.min.js")}"></script>"""
  }

  val drawerTag = raw {
    s"""<script defer src="${staticUrl("javascripts/vendor/jquery.drawer.js")}"></script>"""
  }

  val singleUploaderTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.singleUpload.js")}"></script>"""
  }

  val multiUploaderTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.multiUpload.js")}"></script>"""
  }

  val singleFileUploaderTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.singleFileUploader.js")}"></script>"""
  }

  val unsliderTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/unslider.min.js")}"></script>"""
  }

  val echartsTag = raw {
    s"""<script src="${staticUrl("vendor/echarts/echarts.min.js")}"></script>"""
  }

  val echartsThemeTag = raw {
    s"""<script src="${staticUrl("vendor/echarts/theme/light.js")}"></script><script src="${staticUrl("vendor/echarts/theme/dark.js")}"></script>"""
  }

  //https://www.jq22.com/jquery-info12837
  val printTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.print.js")}"></script>"""
  }

  //https://github.com/isocra/TableDnD
  val tableDnDTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.tablednd.js")}"></script>"""
  }

  val cookieTag = raw {
    s"""<script src="${staticUrl("javascripts/vendor/jquery.cookie.min.js")}"></script>"""
  }

  def basicCsp(implicit req: RequestHeader): ContentSecurityPolicy = {
    val assets = if (req.secure) "https://" + assetDomain else assetDomain
    val socket = (if (req.secure) "wss://" else "ws://") + socketDomain + (if (socketDomain.contains(":")) "" else ":*")
    val remoteSocket = (if (req.secure) "wss://" else "ws://") + remoteSocketDomain + (if (remoteSocketDomain.contains(":")) "" else ":*")
    ContentSecurityPolicy(
      defaultSrc = List("'self'", assets),
      connectSrc = List(
        "'self'",
        assets,
        socket,
        remoteSocket,
        lila.api.Env.current.ExplorerEndpoint,
        lila.api.Env.current.TablebaseEndpoint
      ),
      styleSrc = List("'self'", "'unsafe-inline'", assets),
      fontSrc = List("'self'", assetDomain, "https://fonts.gstatic.com"),
      frameSrc = List("'self'", assets, "https://www.youtube.com", "https://player.twitch.tv"),
      workerSrc = List("'self'", assets),
      imgSrc = List("data:", "*"),
      scriptSrc = List("'self'", "'unsafe-eval'", assets), // unsafe-eval for WebAssembly (wasmx)
      baseUri = List("'none'")
    )
  }

  def defaultCsp(implicit ctx: Context): ContentSecurityPolicy = {
    val csp = basicCsp(ctx.req)
    ctx.nonce.fold(csp)(csp.withNonce(_))
  }

  def embedJsUnsafe(js: String)(implicit ctx: Context): Frag = raw {
    val nonce = ctx.nonce ?? { nonce => s""" nonce="$nonce"""" }
    s"""<script$nonce>$js</script>"""
  }

  def embedJsUnsafe(js: String, nonce: Nonce): Frag = raw {
    s"""<script nonce="$nonce">$js</script>"""
  }
}
