package views.html.coach

import lila.api.Context
import play.api.data.Form
import views.html.account
import lila.coach.Coach
import lila.coach.Certify
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.Region
import controllers.routes

object certify {

  private val dataTab = attr("data-tab")

  def apply(form: Form[_], c: Option[Coach.WithUser], alipayUrl: Option[String])(implicit ctx: Context) = account.layout(
    title = "教练认证",
    active = "coach-certify",
    evenMoreJs = frag(
      smsCaptchaTag,
      provinceCascadeTag,
      jsAt("javascripts/vendor/qrcode.min.js"),
      jsTag("coach.certify.js")
    ),
    evenMoreCss = cssTag("coach")
  ) {
      div(cls := "account box box-pad")(
        h1("教练认证"),
        postForm(cls := "form3 coach-certify", dataSmsrv := 1, action := actionUrl(c).toString)(
          form3.globalError(form),
          form3.split(
            form3.group(form("realName"), "姓名", half = true)(form3.input(_)),
            form3.group(form("certNo"), "身份证号", half = true)(form3.input(_))
          ),
          form3.split(
            form3.group(form("province"), "省份", half = true) { f =>
              form3.select(f, Region.Province.provinces, default = "".some)
            },
            form3.group(form("city"), "城市", half = true) { f =>
              val empty = form3.select(f, List.empty, default = "".some)
              form("province").value.fold(empty) { v =>
                form3.select(f, Region.City.citys(v), default = "".some)
              }
            }
          ),
          ctx.me.get.cellphone.fold {
            views.html.base.smsCaptcha(form)
          } { phone =>
            div(
              strong(label("手机号："), phone, span(cls := "binded")("（已绑定）")),
              form3.hidden(form("template")),
              form3.hidden(form("cellphone")),
              form3.hidden(form("code"), "123456".some)
            )
          },
          br, br,
          c.??(_.coach.certify.overPassed) option strong(label("实名认证："), span(cls := "binded")("已通过")),
          (c.isEmpty || c.??(_.certify.status.isEmpty)) option {
            frag(
              div(
                strong("注："), br,
                p("1、为了您的信息安全，我们使用支付宝第三方认证服务，不保留任何您在认证过程中提供的身份证照片，也不会关联您的支付宝账号，请放心使用。"),
                p("2、需要您准确的填写“姓名”、“身份证号码”，再使用手机上的支付宝APP扫码，完成认证。")
              ),
              div(cls := "qrcode")(
                alipayUrl.isEmpty option img(width := "300px", height := "300px", src := staticUrl("images/certify-qrcode.png")),
                textarea(id := "alipayCertifyUrl", cls := "none")(alipayUrl),
                div(id := "qrcode")
              ),
              div(cls := "qrcode-desc")(
                span("支付宝“扫一扫”")
              )
            )
          },
          c.??(_.coach.certify.passed) option {
            div(cls := "passed")(
              br,
              strong("您的实名认证", span(cls := "binded")("已通过"), "，请点击下方按钮继续完成认证。")
            )
          },
          form3.action(
            form3.submit(submitName(c, alipayUrl), klass = if (submitDisabled(c, alipayUrl)) "disabled" else "", isDisable = submitDisabled(c, alipayUrl))
          )
        )
      )
    }

  def certifyPersonCallback(passed: Boolean)(implicit ctx: Context) =
    views.html.base.layout(
      title = "认证结果",
      moreCss = cssTag("coach"),
      moreJs = frag(
        flatpickrTag,
        jsTag("contest.show.js")
      )
    ) {
        main(cls := "page-small box box-pad certify-callback")(
          h1("认证结果"),
          div(img(cls := "pay-icon", src := staticUrl("images/alipay.png"))),
          div(cls := "message")(
            h3(cls := List("passed" -> passed, "unpassed" -> !passed))(
              strong("认证", if (passed) "成功" else "失败")
            ),
            h3("请回到电脑继续完成认证")
          )
        )
      }

  def actionUrl(c: Option[Coach.WithUser]) = c match {
    case Some(c) => c.certify.status match {
      case None => routes.Coach.certifyPerson
      case _ => routes.Coach.certifyQualify
    }
    case _ => routes.Coach.certifyPerson
  }

  def submitName(c: Option[Coach.WithUser], alipayUrl: Option[String]): String = c match {
    case None => "生成二维码"
    case Some(c) => c.certify.status match {
      case None => if (alipayUrl.isDefined) "实名认证中" else "生成二维码"
      case Some(s) => s match {
        case Certify.Status.Passed => "提交审核"
        case Certify.Status.Applying => "审核中"
        case Certify.Status.Approved => "认证通过"
        case Certify.Status.Rejected => "提交审核"
      }
    }
  }

  def submitDisabled(c: Option[Coach.WithUser], alipayUrl: Option[String]): Boolean = c match {
    case None => false
    case Some(c) => c.certify.status match {
      case None => alipayUrl.isDefined
      case Some(s) => s match {
        case Certify.Status.Passed => false
        case Certify.Status.Applying => true
        case Certify.Status.Approved => true
        case Certify.Status.Rejected => false
      }
    }
  }

}
