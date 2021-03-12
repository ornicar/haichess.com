package views.html.member

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._

object orderPayRedirect {

  def apply(body: String)(implicit ctx: Context) = frag(
    raw(
      s"""<!doctype html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <title>支付跳转</title>
  </head>
  <body><div style ="width:100%;text-align:center;padding:20px;font-size:14px">支付跳转...</div>"""
    ),
    raw(body),
    raw(
      """
  </body>
</html>"""
    )
  )

}
