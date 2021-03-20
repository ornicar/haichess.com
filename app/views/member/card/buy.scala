package views.html.member.card

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import play.api.libs.json.Json
import play.api.data.{ Field, Form }
import lila.common.String.html.safeJsonValue
import lila.user.User
import lila.member.{ JsonView, PayWay, ProductType, VmCardGold, VmCardSilver }
import controllers.routes

object buy {

  def apply(me: User, form: Form[_])(implicit ctx: Context) = bits.layout(
    title = "购买",
    active = "buy",
    moreJs = frag(
      jsTag("member.cardbuy.js"),
      embedJsUnsafe(s"""lichess=lichess||{};lichess.memberCardBuy=${
        safeJsonValue(Json.obj(
          "member" -> JsonView.memberJson(me),
          "products" -> JsonView.productsJson(List(VmCardGold, VmCardSilver)),
          "default" -> Json.obj(
            VmCardGold.id -> ProductType.VirtualMemberCard.defaultItemOfProduct(VmCardGold).map(_.code),
            VmCardSilver.id -> ProductType.VirtualMemberCard.defaultItemOfProduct(VmCardSilver).map(_.code)
          )
        ))
      }""")
    )
  ) {
      frag(
        h1("购买会员卡"),
        postForm(action := routes.MemberOrder.toPay)(
          table(cls := "orderForm")(
            tr(
              th("会员类型"),
              td(
                form3.hidden(name = "productTyp", value = "virtualMemberCard"),
                form3.radio2(form("productId"), ProductType.VirtualMemberCard.all.map { product =>
                  product.id -> product.name
                }, ProductType.VirtualMemberCard.defaultProduct.id.some)
              )
            ),
            tr(
              th("使用期限"),
              td(
                div(cls := "items")(
                  form3.radio2(form("itemCode"), VmCardGold.itemList.map { item =>
                    item.code -> item.name
                  }, ProductType.VirtualMember.defaultItem.map(_.code))
                ),
                div(cls := "note")("有效期3个月，购买后请尽快使用")
              )
            ),
            tr(
              th("单价"),
              td(
                div(cls := "price")(
                  label(cls := "symbol")("￥"),
                  span(cls := "number")("-.--")
                )
              )
            ),
            tr(
              th("数量"),
              td(
                div(cls := "count")(
                  input(tpe := "number", min := 1, max := 999, step := "1", name := "count", value := 1)
                ),
                div(cls := "countError formError")
              )
            ),
            tr(
              th("积分抵扣"),
              td(
                div(cls := "points")(
                  input(tpe := "number", min := 0, max := me.memberOrDefault.points, step := "1", name := "points", value := me.memberOrDefault.points),
                  input(tpe := "hidden", name := "isPointsChange", value := false),
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
