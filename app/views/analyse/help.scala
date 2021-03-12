package views.html.analyse

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object help {

  private def header(text: String) = tr(
    th(colspan := 2)(
      p(text)
    )
  )
  private def row(keys: Frag, desc: Frag) = tr(
    td(cls := "keys")(keys),
    td(cls := "desc")(desc)
  )
  private val or = raw("""<or>/</or>""")
  private def k(str: String) = raw(s"""<kbd>$str</kbd>""")

  def apply(isStudy: Boolean)(implicit ctx: Context) = frag(
    h2(trans.keyboardShortcuts()),
    table(
      tbody(
        header("浏览棋谱"),
        row(frag(k("←"), or, k("→")), trans.keyMoveBackwardOrForward()),
        row(frag(k("j"), or, k("k")), trans.keyMoveBackwardOrForward()),
        row(frag(k("↑"), or, k("↓")), trans.keyGoToStartOrEnd()),
        row(frag(k("0"), or, k("$")), trans.keyGoToStartOrEnd()),
        row(frag(k("shift"), k("←"), or, k("shift"), k("→")), trans.keyEnterOrExitVariation()),
        row(frag(k("shift"), k("J"), or, k("shift"), k("K")), trans.keyEnterOrExitVariation()),
        header("分析选项"),
        row(frag(k("shift"), k("I")), trans.inlineNotation()),
        row(frag(k("l")), "本地引擎分析"),
        row(frag(k("a")), "隐藏/显示箭头"),
        row(frag(k("space")), "按引擎最佳着法走棋"),
        row(frag(k("x")), "隐藏/显示威胁"),
        row(frag(k("e")), "开局/残局浏览器"),
        row(frag(k("f")), trans.flipBoard()),
        row(frag(k("c")), "聊天"),
        row(frag(k("shift"), k("C")), trans.keyShowOrHideComments()),
        row(frag(k("?")), "显示帮助"),
        isStudy option frag(
          header("研习操作"),
          row(frag(k("d")), trans.study.commentThisPosition()),
          row(frag(k("g")), trans.study.annotateWithGlyphs())
        ),
        header("鼠标操作技巧"),
        tr(
          td(cls := "mouse", colspan := 2)(
            ul(
              li(trans.youCanAlsoScrollOverTheBoardToMoveInTheGame()),
              li(trans.analysisShapesHowTo())
            )
          )
        )
      )
    )
  )
}
