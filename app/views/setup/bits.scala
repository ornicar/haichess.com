package views.html.setup

import play.api.data.{ Field, Form }
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes
import org.joda.time.DateTime

private object bits {

  val prefix = "sf_"

  def fenInput(field: Field, strict: Boolean, validFen: Option[lila.setup.ValidFen])(implicit ctx: Context) = {
    val url = field.value.fold(routes.Editor.index)(routes.Editor.load).url
    div(cls := "fen_position optional_config")(
      frag(
        div(cls := "fen_form", dataValidateUrl := s"""${routes.Setup.validateFen()}${strict.??("?strict=1")}""")(
          form3.input(field)(st.placeholder := trans.pasteTheFenStringHere.txt()),
          a(cls := "button button-empty", dataIcon := "m", title := trans.boardEditor.txt(), href := url)
        ),
        a(cls := "board_editor", href := url)(
          span(cls := "preview")(
            validFen.map { vf =>
              div(
                cls := "mini-board cg-wrap parse-fen is2d",
                dataColor := vf.color.name,
                dataFen := vf.fen.value,
                dataResizable := "1"
              )(cgWrapContent)
            }
          )
        )
      )
    )
  }

  def renderVariant(form: Form[_], variants: List[SelectChoice])(implicit ctx: Context) =
    div(cls := "variant label_select")(
      renderLabel(form("variant"), trans.variant()),
      renderSelect(form("variant"), variants.filter {
        case (id, _, _) => ctx.noBlind || lila.game.Game.blindModeVariants.exists(_.id.toString == id)
      })
    )

  def renderSelect(
    field: Field,
    options: Seq[SelectChoice],
    compare: (String, String) => Boolean = (a, b) => a == b,
    selectedValue: Option[String] = None
  ) = select(id := s"$prefix${field.id}", name := field.name)(
    options.map {
      case (value, name, title) => {
        val tv = if (selectedValue.isDefined) selectedValue else field.value
        option(
          st.value := value,
          st.title := title,
          tv.exists(v => compare(v, value)) option selected
        )(name)
      }
    }
  )

  def renderRadios(field: Field, options: Seq[SelectChoice]) =
    st.group(cls := "radio")(
      options.map {
        case (key, name, hint) => div(
          input(
            `type` := "radio",
            id := s"$prefix${field.id}_${key}",
            st.name := field.name,
            value := key,
            field.value.has(key) option checked
          ),
          label(
            cls := "required",
            title := hint,
            `for` := s"$prefix${field.id}_$key"
          )(name)
        )
      }
    )

  def renderInput(field: Field) =
    input(name := field.name, value := field.value, `type` := "hidden")

  def renderLabel(field: Field, content: Frag) =
    label(`for` := s"$prefix${field.id}")(content)

  def renderTimeMode(form: Form[_], config: lila.setup.BaseConfig)(implicit ctx: Context) =
    div(cls := "time_mode_config optional_config")(
      div(cls := "label_select")(
        renderLabel(form("timeMode"), trans.timeControl()),
        renderSelect(form("timeMode"), translatedTimeModeChoices)
      ),
      if (ctx.blind) frag(
        div(cls := "time_choice")(
          renderLabel(form("time"), trans.minutesPerSide()),
          renderSelect(form("time"), clockTimeChoices, (a, b) => a.replace(".0", "") == b)
        ),
        div(cls := "increment_choice")(
          renderLabel(form("increment"), trans.incrementInSeconds()),
          renderSelect(form("increment"), clockIncrementChoices)
        )
      )
      else frag(
        div(cls := "time_choice slider")(
          trans.minutesPerSide(),
          ": ",
          span(chess.Clock.Config(~form("time").value.map(x => (x.toDouble * 60).toInt), 0).limitString.toString),
          renderInput(form("time"))
        ),
        div(cls := "increment_choice slider")(
          trans.incrementInSeconds(),
          ": ",
          span(form("increment").value),
          renderInput(form("increment"))
        )
      ),
      div(cls := "correspondence")(
        if (ctx.blind) div(cls := "days_choice")(
          renderLabel(form("days"), trans.daysPerTurn()),
          renderSelect(form("days"), corresDaysChoices)
        )
        else div(cls := "days_choice slider")(
          trans.daysPerTurn(),
          ": ",
          span(form("days").value),
          renderInput(form("days"))
        )
      )
    )

  def renderAppt(form: Form[_], appt: Boolean)(implicit ctx: Context) = {
    val now = DateTime.now
    div(cls := "appt_config")(
      div(cls := "label_select")(
        renderLabel(form("appt"), "????????????"),
        renderSelect(form("appt"), apptChoices, selectedValue = (if (appt) "1".some else "0".some))
      ),
      div(cls := "appt_pick none")(
        form3.input2(form("apptStartsAt"), vl = now.plusMinutes(5).toString("yyyy-MM-dd HH:mm").some, klass = "flatpickr")(
          dataEnableTime := true,
          datatime24h := true,
          placeholder := "????????????"
        )(dataMinDate := now.plusMinutes(1).toString("yyyy-MM-dd HH:mm"), dataMaxDate := (now plusWeeks 2).toString("yyyy-MM-dd HH:mm")),
        form3.textarea(form("apptMessage"), vl = "".some)(placeholder := "??????")
      )
    )
  }

  val dataRandomColorVariants =
    attr("data-random-color-variants") := lila.game.Game.variantsWhereWhiteIsBetter.map(_.id).mkString(",")

  val dataAnon = attr("data-anon")
  val dataMin = attr("data-min")
  val dataMax = attr("data-max")
  val dataValidateUrl = attr("data-validate-url")
  val dataResizable = attr("data-resizable")
  val dataType = attr("data-type")
}
