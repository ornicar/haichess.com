package views.html.search

import play.api.data.Form
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.gameSearch.{ Query, Sorting }

private object bits {

  private val dateFormatter = DateTimeFormat.forPattern("YYYY-MM-dd");
  private val dateMin = "2011-01-01"
  private def dateMinMax: List[Modifier] = List(min := dateMin, max := dateFormatter.print(DateTime.now))

  def of(form: Form[_])(implicit ctx: Context) = new {

    def dataReqs = List("winner", "loser", "white", "black").map { f =>
      data(s"req-$f") := ~form("players")(f).value
    }

    def colors(hide: Boolean) =
      chess.Color.all.map { color =>
        tr(cls := List(s"${color.name}User user-row" -> true, "none" -> hide))(
          th(label(`for` := form3.id(form("players")(color.name)))(color.fold(trans.white, trans.black)())),
          td(cls := "single")(
            st.select(id := form3.id(form("players")(color.name)), name := form("players")(color.name).name)(
              option(cls := "blank", value := "")
            )
          )
        )
      }

    def winner(hide: Boolean) = tr(cls := List("winner user-row" -> true, "none" -> hide))(
      th(label(`for` := form3.id(form("players")("winner")))(trans.winner())),
      td(cls := "single")(
        st.select(id := form3.id(form("players")("winner")), name := form("players")("winner").name)(
          option(cls := "blank", value := "")
        )
      )
    )

    def loser(hide: Boolean) = tr(cls := List("loser user-row" -> true, "none" -> hide))(
      th(label(`for` := form3.id(form("players")("loser")))("失败者")),
      td(cls := "single")(
        st.select(id := form3.id(form("players")("loser")), name := form("players")("loser").name)(
          option(cls := "blank", value := "")
        )
      )
    )

    def rating = tr(
      th(label(trans.rating(), " ", span(cls := "help", title := "两位棋手的平均得分")("(?)"))),
      td(
        div(cls := "half")("从 ", form3.select(form("ratingMin"), Query.averageRatings, "".some)),
        div(cls := "half")("到 ", form3.select(form("ratingMax"), Query.averageRatings, "".some))
      )
    )

    def hasAi = tr(
      th(label(`for` := form3.id(form("hasAi")))("对手类型", " ", span(cls := "help", title := "玩家的对手是人还是电脑")("(?)"))),
      td(cls := "single opponent")(form3.select(form("hasAi"), Query.hasAis, "".some))
    )

    def aiLevel = tr(cls := "aiLevel none")(
      th(label("A.I. 级别")),
      td(
        div(cls := "half")("从 ", form3.select(form("aiLevelMin"), Query.aiLevels, "".some)),
        div(cls := "half")("到 ", form3.select(form("aiLevelMax"), Query.aiLevels, "".some))
      )
    )

    def source = tr(
      th(label(`for` := form3.id(form("source")))("来源")),
      td(cls := "single")(form3.select(form("source"), Query.sources, "".some))
    )

    def perf = tr(
      th(label(`for` := form3.id(form("perf")))(trans.variant())),
      td(cls := "single")(form3.select(form("perf"), Query.perfs, "".some))
    )

    def mode = tr(
      th(label(`for` := form3.id(form("mode")))(trans.mode())),
      td(cls := "single")(form3.select(form("mode"), Query.modes, "".some))
    )

    def turns = tr(
      th(label("回合")),
      td(
        div(cls := "half")("从 ", form3.select(form("turnsMin"), Query.turns, "".some)),
        div(cls := "half")("到 ", form3.select(form("turnsMax"), Query.turns, "".some))
      )
    )

    def duration = tr(
      tr(
        th(label(trans.duration())),
        td(
          div(cls := "half")("从 ", form3.select(form("durationMin"), Query.durations, "".some)),
          div(cls := "half")("到 ", form3.select(form("durationMax"), Query.durations, "".some))
        )
      )
    )

    def clockTime = tr(
      th(label("时钟初始时间")),
      td(
        div(cls := "half")("从 ", form3.select(form("clock")("initMin"), Query.clockInits, "".some)),
        div(cls := "half")("到 ", form3.select(form("clock")("initMax"), Query.clockInits, "".some))
      )
    )

    def clockIncrement = tr(
      th(label("时钟增量")),
      td(
        div(cls := "half")("从 ", form3.select(form("clock")("incMin"), Query.clockIncs, "".some)),
        div(cls := "half")("到 ", form3.select(form("clock")("incMax"), Query.clockIncs, "".some))
      )
    )

    def status = tr(
      th(label(`for` := form3.id(form("status")))("结果")),
      td(cls := "single")(form3.select(form("status"), Query.statuses, "".some))
    )

    def winnerColor = tr(
      th(label(`for` := form3.id(form("winnerColor")))("赢方")),
      td(cls := "single")(form3.select(form("winnerColor"), Query.winnerColors, "".some))
    )

    def date = tr(cls := "date")(
      th(label("日期")),
      td(
        div(cls := "half")("从 ", form3.input(form("dateMin"), klass = "flatpickr")(dateMinMax: _*)),
        div(cls := "half")("到 ", form3.input(form("dateMax"), klass = "flatpickr")(dateMinMax: _*))
      /*        div(cls := "half")("从 ", form3.input(form("dateMin"), "date")(dateMinMax: _*)),
        div(cls := "half")("到 ", form3.input(form("dateMax"), "date")(dateMinMax: _*))*/
      )
    )

    def sort = tr(
      th(label("排序")),
      td(
        div(cls := "half")("用 ", form3.select(form("sort")("field"), Sorting.fields)),
        div(cls := "half")("顺序 ", form3.select(form("sort")("order"), Sorting.orders))
      )
    )

    def analysed = {
      val field = form("analysed")
      tr(
        th(label(`for` := form3.id(field))("分析 ", span(cls := "help", title := "Only games where a computer analysis is available")("(?)"))),
        td(cls := "single")(
          st.input(
            tpe := "checkbox",
            cls := "cmn-toggle",
            id := form3.id(field),
            name := field.name,
            value := "1",
            field.value.has("1") option checked
          ),
          label(`for` := form3.id(field))
        )
      )
    }
  }
}
