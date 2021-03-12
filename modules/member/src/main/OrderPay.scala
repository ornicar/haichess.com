package lila.member

import org.joda.time.DateTime

case class OrderPay(
    _id: String, // orderNo
    payWay: PayWay,
    alipayData: List[AlipayDataWithTime],
    createAt: DateTime
) {

  def id = _id

}

case class AlipayDataWithTime(data: AlipayData, updateAt: DateTime)

case class AlipayData(
    trade_no: String, // 支付宝交易号
    app_id: String, // 支付宝分配给开发者的应用 ID
    out_trade_no: String, // 商户订单号, 原支付请求的商户订单号
    out_biz_no: Option[String], // 商户业务号, 商户业务ID，主要是退款通知中返回退款申请的流水号。
    buyer_id: Option[String], // 买家支付宝用户号。买家支付宝账号对应的支付宝唯一用户号。以 2088 开头的纯 16 位数字。
    seller_id: Option[String], // 卖家支付宝用户号
    trade_status: Option[String], // 交易状态。交易目前所处的状态。WAIT_BUYER_PAY（交易创建，等待买家付款）、TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）
    total_amount: Option[BigDecimal], // 订单金额。本次交易支付的订单金额，单位为人民币（元），精确到小数点后 2 位
    receipt_amount: Option[BigDecimal], // 实收金额。商家在交易中实际收到的款项，单位为元，精确到小数点后 2 位。
    invoice_amount: Option[BigDecimal], // 开票金额。用户在交易中支付的可开发票的金额，单位为元，精确到小数点后 2 位。
    buyer_pay_amount: Option[BigDecimal], // 用户在交易中支付的金额，单位为元，精确到小数点付款金额后 2 位。
    point_amount: Option[BigDecimal], // 集分宝金额。使用集分宝支付的金额，单位为元，精确到小数点后 2 位。
    refund_fee: Option[BigDecimal], // 总退款金额。退款通知中，返回总退款金额，单位为元，精确到小数点后 2 位。
    subject: Option[String], // 订单标题。商品的标题/交易标题/订单标题/订单关键字等，是请求时对应的参数，原样通知回来。
    body: Option[String], // 商品描述。该订单的备注、描述、明细等。对应请求时的 body 参数，原样通知回来。
    gmt_create: Option[DateTime], // 交易创建时间。该笔交易创建的时间。格式为yyyy-MM-dd HH:mm:ss。
    gmt_payment: Option[DateTime], //  交易付款时间。该笔交易的买家付款时间。格式为yyyy-MM-dd HH:mm:ss。
    gmt_refund: Option[DateTime], //  交易退款时间。该笔交易的退款时间。格式为yyyy-MM-dd HH:mm:ss.。
    gmt_close: Option[DateTime], //  交易结束时间。该笔交易结束时间。格式为yyyy-MM-dd HH:mm:ss。
    fund_bill_list: Option[String], // 支付金额信息。支付成功的各个渠道金额信息。详情请参见 资金明细信息说明。[{“amount”:“15.00”,“fundChannel”:“ALIPAYACCOUNT”}]
    voucher_detail_list: Option[String], // 优惠券信息。本交易支付时所使用的所有优惠券信息。详情请参见 优惠券信息说明。[{“amount”:“0.20”,“merchantContribute”:“0.00”,“name”:“一键创建券模板的券名称”,“otherContribute”:“0.20”,“type”:“ALIPAY_DISCOUNT_VOUCHER”,“memo”:“学生卡8折优惠”]
    passback_params: Option[String] // 回传参数。公共回传参数，如果请求时传递了该参数，则返回给商户时会在异步通知时将该参数原样返回。本参数必须进行 UrlEncode 之后才可以发送给支付宝
)
