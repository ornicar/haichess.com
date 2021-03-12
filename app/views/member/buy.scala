package views.html.member

import lila.api.Context
import lila.user.User
import lila.user.MemberLevel
import play.api.data.{ Field, Form }
import play.api.libs.json.Json
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.member.{ JsonView, PayWay, Product, ProductType, VmcGold, VmcSilver }
import controllers.routes

object buy {

  def apply(me: User, form: Form[_], discounts: List[(String, String)], level: Option[String])(implicit ctx: Context) = views.html.base.layout(
    title = "加入/续费",
    moreJs = frag(
      jsTag("member.buy.js"),
      embedJsUnsafe(s"""lichess=lichess||{};lichess.memberBuy=${
        safeJsonValue(Json.obj(
          "member" -> JsonView.memberJson(me),
          "products" -> JsonView.productsJson(List(VmcGold, VmcSilver)),
          "default" -> Json.obj(
            VmcGold.id -> ProductType.VirtualMember.defaultItemOfProduct(VmcGold).map(_.code),
            VmcSilver.id -> ProductType.VirtualMember.defaultItemOfProduct(VmcSilver).map(_.code)
          )
        ))
      }""")
    ),
    moreCss = cssTag("member")
  ) {
      val products = ProductType.VirtualMember.all.filter(p => {
        val ml = Product.toMemberLevel(p)
        ml.id > me.memberLevel.id || (ml == me.memberLevel && !me.memberOrDefault.lvWithExpire.isForever)
      }) map { product =>
        product.id -> product.name
      }
      val detaultProductId = level.fold(ProductType.VirtualMember.defaultProduct.id.some) { lv => ProductType.VirtualMember.productOfLevel(lv) }
      main(cls := "box box-pad page-small")(
        h1("加入/续费"),
        postForm(action := routes.MemberOrder.toPay)(
          table(cls := "orderForm")(
            tr(
              th("会员类型"),
              td(
                form3.hidden(name = "productTyp", value = "virtualMember"),
                form3.hidden(name = "count", value = "1"),
                form3.radio2(form("productId"), products, detaultProductId)
              )
            ),
            tr(
              th("使用时长"),
              td(
                div(cls := "items")(
                  form3.radio2(form("itemCode"), VmcGold.itemList.map { item =>
                    item.code -> item.name
                  }, ProductType.VirtualMember.defaultItem.map(_.code))
                )
              )
            ),
            tr(
              th("价格"),
              td(
                div(cls := "price")(
                  div(
                    label(cls := "symbol")("￥"),
                    span(cls := "number")("-.--"),
                    del("[", label(cls := "symbol")("￥"), span(cls := "del-price")(MemberLevel.defaultView.prices.year.toString), "]")
                  ),
                  div(cls := "note")("2年以上8折，3年以上7折（银牌用户升级为金牌会员，优先计算差价）")
                )
              )
            ),
            tr(
              th("优惠码"),
              td(
                div(cls := "inviteUser")(
                  form3.select(form("inviteUser"), discounts, default = if (discounts.size > 1) "".some else none),
                  div(cls := "minusAmount")(
                    label(cls := "symbol")("优惠：￥"),
                    span(cls := "number")("0.00")
                  )
                ),
                div(cls := "inviteUserError formError")
              )
            ),
            tr(
              th("积分抵扣"),
              td(
                div(cls := "points")(
                  input(tpe := "number", min := 0, max := me.memberOrDefault.points, step := "1", name := "points", value := me.memberOrDefault.points),
                  label(s"共 ${me.memberOrDefault.points} 积分（1积分=1人民币）")
                ),
                div(cls := "pointsError formError")
              )
            ),
            tr(
              th("待支付"),
              td(
                div(cls := "payPrice")(
                  label(cls := "symbol")("￥"),
                  span(cls := "number")("-.--")
                )
              )
            ),
            tr(
              th("支付方式"),
              td(
                payRadio(form("payWay"), PayWay.choices, PayWay.Alipay.id.some)
              )
            ),
            tr(
              th,
              td(
                button(cls := "button button-green topay")("去支付")
              )
            )
          )
        )
      )
    }

  def payRadio(
    field: Field,
    options: Iterable[(String, String)],
    defaultValue: Option[String] = None
  ): Frag =
    div(cls := "radio-group payRadio")(
      options.toSeq.map {
        case (value, _) => {
          val check = field.value.fold(defaultValue)(_.some).has(value)
          div(cls := "radio")(
            st.input(
              check.option(checked),
              st.id := s"${field.name}_$value",
              tpe := "radio",
              st.name := field.name,
              st.value := value
            ),
            label(cls := "radio-label", `for` := s"${field.name}_$value")(
              img(cls := s"icon $value", `for` := s"${field.name}_$value", src := staticUrl(s"images/pay/$value.png"))
            )
          )
        }
      }
    )

}
