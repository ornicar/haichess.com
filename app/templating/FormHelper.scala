package lila.app
package templating

import play.api.data._
import lila.api.Context
import lila.app.templating.Environment.spinner
import lila.app.ui.ScalatagsTemplate.{ id, input, _ }
import lila.i18n.I18nDb

trait FormHelper { self: I18nHelper =>

  def errMsg(form: Field)(implicit ctx: Context): Frag = errMsg(form.errors)

  def errMsg(form: Form[_])(implicit ctx: Context): Frag = errMsg(form.errors)

  def errMsg(error: FormError)(implicit ctx: Context): Frag =
    p(cls := "error")(transKey(error.message, I18nDb.Site, error.args))

  def errMsg(errors: Seq[FormError])(implicit ctx: Context): Frag =
    errors map errMsg

  def globalError(form: Form[_])(implicit ctx: Context): Option[Frag] =
    form.globalError map errMsg

  val booleanChoices = Seq("true" -> "✓ Yes", "false" -> "✗ No")

  val postForm = form(method := "post")
  val submitButton = button(tpe := "submit")

  object form3 {

    private val idPrefix = "form3"

    def id(field: Field): String = s"$idPrefix-${field.id}"

    private def groupLabel(field: Field) = label(cls := "form-label", `for` := id(field))
    private val helper = small(cls := "form-help")

    private def errors(errs: Seq[FormError])(implicit ctx: Context): Frag = errs map error
    private def errors(field: Field)(implicit ctx: Context): Frag = errors(field.errors)
    private def error(err: FormError)(implicit ctx: Context): Frag =
      p(cls := "error")(transKey(err.message, I18nDb.Site, err.args))

    private def validationModifiers(field: Field): Seq[Modifier] = field.constraints collect {
      /* Can't use constraint.required, because it applies to optional fields
         * such as `optional(nonEmptyText)`.
         * And we can't tell from the Field whether it's optional or not :(
         */
      // case ("constraint.required", _) => required
      case ("constraint.minLength", Seq(m: Int)) => minlength := m
      case ("constraint.maxLength", Seq(m: Int)) => maxlength := m
      case ("constraint.min", Seq(m: Int)) => min := m
      case ("constraint.max", Seq(m: Int)) => max := m
    }

    val split = div(cls := "form-split")

    def group(
      field: Field,
      labelContent: Frag,
      klass: String = "",
      half: Boolean = false,
      help: Option[Frag] = None
    )(content: Field => Frag)(implicit ctx: Context): Frag = div(cls := List(
      "form-group" -> true,
      "is-invalid" -> field.hasErrors,
      "form-half" -> half,
      klass -> klass.nonEmpty
    ))(
      groupLabel(field)(labelContent),
      content(field),
      errors(field),
      help map { helper(_) }
    )

    def groupNoLabel(
      field: Field,
      klass: String = "",
      help: Option[Frag] = None
    )(content: Field => Frag)(implicit ctx: Context): Frag = div(cls := List(
      "form-group" -> true,
      "is-invalid" -> field.hasErrors,
      klass -> klass.nonEmpty
    ))(
      content(field),
      errors(field),
      help map { helper(_) }
    )

    def input(field: Field, typ: String = "", klass: String = ""): BaseTagType =
      st.input(
        st.id := id(field),
        name := field.name,
        value := field.value,
        tpe := typ.nonEmpty.option(typ),
        cls := List("form-control" -> true, klass -> klass.nonEmpty)
      )(validationModifiers(field))

    def input2(field: Field, vl: Option[String] = None, typ: String = "", klass: String = ""): BaseTagType =
      st.input(
        st.id := id(field),
        name := field.name,
        value := vl,
        tpe := typ.nonEmpty.option(typ),
        cls := List("form-control" -> true, klass -> klass.nonEmpty)
      )(validationModifiers(field))

    def input3(field: Field, vl: Option[String] = None, typ: String = "", klass: String = ""): BaseTagType =
      st.input(
        st.id := id(field),
        name := field.name,
        value := field.value.fold(vl)(_ => field.value),
        tpe := typ.nonEmpty.option(typ),
        cls := List("form-control" -> true, klass -> klass.nonEmpty)
      )(validationModifiers(field))

    def checkbox(
      field: Field,
      labelContent: Frag,
      half: Boolean = false,
      help: Option[Frag] = None,
      disabled: Boolean = false,
      klass: String = ""
    ): Frag = div(cls := List(
      "form-check form-group" -> true,
      "form-half" -> half,
      klass -> klass.nonEmpty
    ))(
      div(
        span(cls := "form-check-input")(
          st.input(
            st.id := id(field),
            name := field.name,
            value := "true",
            tpe := "checkbox",
            cls := "form-control cmn-toggle",
            field.value.has("true") option checked,
            disabled option st.disabled
          ),
          label(`for` := id(field))
        ),
        groupLabel(field)(labelContent)
      ),
      help map { helper(_) }
    )

    def select(
      field: Field,
      options: Iterable[(Any, String)],
      default: Option[String] = None,
      klass: String = ""
    ): Frag = st.select(
      st.id := id(field),
      name := field.name,
      cls := List("form-control" -> true, klass -> klass.nonEmpty)
    )(validationModifiers(field))(
        default map { option(value := "")(_) },
        options.toSeq map {
          case (value, name) => option(
            st.value := value.toString,
            field.value.has(value.toString) option selected
          )(name)
        }
      )

    def select2(
      field: Field,
      vl: Option[String],
      options: Iterable[(Any, String)],
      default: Option[String] = None,
      disabled: Boolean = false
    ): Frag = st.select(
      st.id := id(field),
      name := field.name,
      cls := "form-control",
      disabled option st.disabled
    )(validationModifiers(field))(
        default map { option(value := "")(_) },
        options.toSeq map {
          case (value, name) => option(
            st.value := value.toString,
            vl.has(value.toString) option selected
          )(name)
        }
      )

    def textarea(
      field: Field,
      klass: String = "",
      vl: Option[String] = None
    )(modifiers: Modifier*): Frag = st.textarea(
      st.id := id(field),
      name := field.name,
      cls := List("form-control" -> true, klass -> klass.nonEmpty)
    )(validationModifiers(field))(modifiers)(vl | ~field.value)

    val actions = div(cls := "form-actions")
    val action = div(cls := "form-actions single")

    def submit(
      content: Frag,
      icon: Option[String] = Some("E"),
      nameValue: Option[(String, String)] = None,
      klass: String = "",
      confirm: Option[String] = None,
      isDisable: Boolean = false
    ): Frag = submitButton(
      dataIcon := icon,
      name := nameValue.map(_._1),
      value := nameValue.map(_._2),
      cls := List(
        "submit button" -> true,
        "text" -> icon.isDefined,
        "confirm" -> confirm.nonEmpty,
        klass -> klass.nonEmpty
      ),
      title := confirm,
      isDisable option disabled
    )(content)

    def hidden(field: Field, value: Option[String] = None): Frag = st.input(
      st.id := id(field),
      name := field.name,
      st.value := value.orElse(field.value),
      tpe := "hidden"
    )

    def hidden(name: String, value: String): Tag = st.input(
      st.name := name,
      st.value := value,
      tpe := "hidden"
    )

    def password(field: Field, content: Frag, help: Option[Frag] = None)(implicit ctx: Context): Frag =
      group(field, content, help = help)(input(_, typ = "password")(required))

    def passwordModified(field: Field, content: Frag)(modifiers: Modifier*)(implicit ctx: Context): Frag =
      group(field, content)(input(_, typ = "password")(required)(modifiers))

    def globalError(form: Form[_])(implicit ctx: Context): Option[Frag] =
      form.globalError map { err =>
        div(cls := "form-group is-invalid")(error(err))
      }

    def flatpickr(field: Field, withTime: Boolean = true): Frag =
      input(field, klass = "flatpickr")(
        dataEnableTime := withTime,
        datatime24h := withTime
      )

    def timeFlatpickr(field: Field): Frag =
      input(field, klass = "flatpickr")(
        dataEnableTime := true,
        datatime24h := true,
        dataNoCalendar := true
      )

    def radio(
      field: Field,
      options: Iterable[(Any, String)]
    ): Frag =
      div(cls := "radio-group")(
        options.toSeq.map {
          case (value, name) => {
            val check = field.value.has(value.toString)
            span(cls := "radio")(
              st.input(
                check.option(checked),
                st.id := s"${field.name}_${value}",
                tpe := "radio",
                st.name := field.name,
                st.value := value.toString
              ),
              label(cls := "radio-label", `for` := s"${field.name}_${value}")(name)
            )
          }
        }
      )

    def radio2(
      field: Field,
      options: Iterable[(Any, String)],
      defaultValue: Option[String] = None
    ): Frag =
      div(cls := "radio-group")(
        options.toSeq.map {
          case (value, name) => {
            val check = (field.value.fold(defaultValue)(_.some)).has(value.toString)
            span(cls := "radio")(
              st.input(
                check.option(checked),
                st.id := s"${field.name}_${value}",
                tpe := "radio",
                st.name := field.name,
                st.value := value.toString
              ),
              label(cls := "radio-label", `for` := s"${field.name}_${value}")(name)
            )
          }
        }
      )

    def tagsWithKv(form: Form[_], f: String, elements: List[(String, String)])(implicit ctx: Context): Frag = {
      val vs = (0 to elements.size - 1) map { i =>
        form(s"$f[$i]").value
      } filter (_.isDefined) map (_.get)

      div(cls := "tag-group")(
        elements.zipWithIndex map {
          case ((v, n), i) =>
            val _id = s"$f-$v"
            span(
              st.input(
                st.id := _id,
                name := s"$f[$i]",
                tpe := "checkbox",
                value := v,
                vs.contains(v) option checked
              ),
              label(`for` := _id)(n)
            )
        }
      )
    }

    def tags(form: Form[_], f: String, elements: Set[String])(implicit ctx: Context): Frag = {
      val v = (0 to elements.size - 1) map { i =>
        form(s"$f[$i]").value
      } filter (_.isDefined) map (_.get)

      div(cls := "tag-group")(
        elements.toList.zipWithIndex map {
          case (e, i) =>
            val _id = s"$f-$e"
            span(
              st.input(
                st.id := _id,
                name := s"$f[$i]",
                tpe := "checkbox",
                value := e,
                v.contains(e) option checked
              ),
              label(`for` := _id)(e)
            )
        }
      )
    }

    def dbImageUrl(path: String) = s"//${lila.api.Env.current.Net.AssetDomain}/image/$path"
    def singleImage(field: Field, label: String = "点击上传图片") = {
      val image = field.value.getOrElse("")
      div(cls := "single-uploader")(
        div(cls := List("uploader" -> true, "none" -> image.nonEmpty))(
          i(cls := "upload-icon")("+"),
          p(label)
        ),
        div(cls := List("preview" -> true, "none" -> image.isEmpty))(
          img(src := image.nonEmpty option dbImageUrl(image)),
          div(cls := "loading none")(
            span("上传中...")
          )
        ),
        st.input(tpe := "file", name := "file", cls := "none", accept := "image/jpg,image/png,image/jpeg"),
        st.input(tpe := "hidden", name := field.name, value := image)
      )
    }

    def multiImage(form: Form[_], fieldName: String, maxLength: Int = 5, label: String = "点击上传图片") = {
      div(cls := "multi-uploader")(
        div(cls := "preview-list")(
          (0 to maxLength - 1).map { i => form(s"$fieldName[$i]") }.filter(_.value.isDefined).zipWithIndex.map {
            case (field, i) =>
              val image = field.value.get
              div(cls := s"preview", dataId := i)(
                a(cls := "remove", dataIcon := "L", title := "删除"),
                img(src := dbImageUrl(image)),
                st.input(tpe := "hidden", name := field.name, value := image)
              )
          }
        ),
        div(cls := "uploader")(
          i(cls := "upload-icon")("+"),
          p(label),
          div(cls := "loading none")(
            span("上传中...")
          ),
          st.input(tpe := "file", name := "file", cls := "none", accept := "image/jpg,image/png,image/jpeg")
        )
      )
    }

    def dbFileUrl(path: String) = s"//${lila.api.Env.current.Net.AssetDomain}/file/$path"
    def singleFile(field: Field) = {
      val filePath = field.value.getOrElse("")
      val fileName = filePath.split("/").toList.reverse.head
      div(cls := "single-file")(
        a(cls := "button choose")("选择文件"),
        st.input(tpe := "file", name := "file", cls := "none", accept := "image/*,application/msexcel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/msword,application/pdf"),
        st.input(tpe := "hidden", name := field.name, value := filePath),
        div(cls := "loader none")(spinner),
        div(cls := List("preview" -> true, "none" -> filePath.isEmpty))(
          a(cls := "name", href := dbFileUrl(filePath))(fileName),
          a(cls := "remove", dataIcon := "L", title := "删除")
        )
      )
    }

    object file {
      def image(name: String): Frag = st.input(tpe := "file", st.name := name, accept := "image/*")
      def pgn(name: String): Frag = st.input(tpe := "file", st.name := name, accept := ".pgn")
      def fen(name: String): Frag = st.input(tpe := "file", st.name := name, accept := ".fen")
      def office(name: String): Frag = st.input(tpe := "file", st.name := name, accept := "application/msexcel,application/msword,application/pdf")
    }
  }
}
