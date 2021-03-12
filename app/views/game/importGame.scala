package views.html
package game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object importGame {

  private val dataTab = attr("data-tab")

  private def analyseHelp(implicit ctx: Context) =
    ctx.isAnon option a(cls := "blue", href := routes.Auth.signup)(trans.youNeedAnAccountToDoThat())

  def apply(pgnForm: play.api.data.Form[_], fenForm: play.api.data.Form[_], pgnActive: Boolean, fenActive: Boolean)(implicit ctx: Context) = views.html.base.layout(
    title = trans.importGame.txt(),
    moreCss = frag(
      cssTag("importer")
    ),
    moreJs = frag(
      tagsinputTag,
      jsTag("importer.js")
    ),
    openGraph = lila.app.ui.OpenGraph(
      title = "导入",
      url = s"$netBaseUrl${routes.Importer.importGame.url}",
      description = trans.importGameExplanation.txt()
    ).some
  ) {
      val pgnPanelCls = if (pgnActive) "panel game active" else "panel game"
      val fenPanelCls = if (fenActive) "panel puzzle active" else "panel puzzle"
      val lastMove = List(
        false -> "局面 + 答案",
        true -> "原始局面 + 对方初始着法 + 答案"
      )
      main(cls := "importer page-small box box-pad")(
        h1("导入"),
        div(cls := "tabs")(
          div(dataTab := "game", pgnActive option (cls := "active"))("棋局"),
          div(dataTab := "puzzle", fenActive option (cls := "active"))("战术题")
        ),
        div(cls := "panels")(
          div(cls := pgnPanelCls)(
            p(cls := "explanation")(trans.importGameExplanation()),
            postForm(cls := "form3 import", action := routes.Importer.sendBatchGame())(
              form3.group(pgnForm("gameTag"), "自定义标签")(form3.input(_)),
              form3.group(pgnForm("pgn"), labelContent = frag(trans.pasteThePgnStringHere(), "（", a(href := staticUrl("static/对局批量导入.pgn"))("样例"), "）"))(form3.textarea(_)()),
              form3.group(pgnForm("pgnFile"), raw("或者上传一个PGN文件"), klass = "upload") { f =>
                form3.file.pgn(f.name)
              },
              form3.checkbox(pgnForm("analyse"), trans.requestAComputerAnalysis(), help = Some(analyseHelp), disabled = ctx.isAnon),
              form3.action(form3.submit(trans.importGame(), "/".some))
            )
          ),
          div(cls := fenPanelCls)(
            postForm(cls := "form3 import", action := routes.Importer.sendBatchPuzzle())(
              form3.group(fenForm("puzzleTag"), "自定义标签")(form3.input(_)),
              form3.group(fenForm("hasLastMove"), raw("格式"))(form3.select(_, lastMove)),
              form3.group(fenForm("pgn"), labelContent = frag(trans.pasteThePgnStringHere(), "（", a(href := staticUrl("static/战术题批量导入.pgn"))("样例"), "）"))(form3.textarea(_)(placeholder := "#样例# 局面 + 答案\n[FEN \"8/2r4p/8/P3N3/1P1p4/7P/4Kpk1/5R2 b - - 7 58\"]\n58... Rc2+ 59. Kd3 Kxf1\n\n#样例# 原始局面 + 对方初始着法 + 答案\n[FEN \"8/2r4p/8/P3N3/1P1p4/7P/3K1pk1/5R2 w - - 6 58\"]\n58. Ke2 Rc2+ 59. Kd3 Kxf1")),
              form3.group(fenForm("pgnFile"), raw("或者上传一个PGN文件"), klass = "upload") { f =>
                form3.file.pgn(f.name)
              },
              form3.globalError(fenForm),
              form3.action(form3.submit(trans.importGame(), "/".some))
            )
          )
        )
      )
    }
}
