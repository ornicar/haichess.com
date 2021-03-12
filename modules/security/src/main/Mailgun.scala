package lila.security

import java.util.UUID

import scala.concurrent.duration.{ span => _, _ }
import akka.actor.ActorSystem
import javax.xml.crypto.dsig.SignatureMethod
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current
import scalatags.Text.all._
import lila.common.String.html.escapeHtml
import lila.common.String.html.nl2brUnsafe
import lila.common.{ EmailAddress, Lang }
import lila.i18n.I18nKeys.{ emails => trans }
import play.api.libs.json._
import lila.security.AliYunApiHelper._
import lila.security.AliYunApiEmailParam._

final class Mailgun(
    apiUrl: String,
    apiKey: String,
    apiSecret: String,
    from: String,
    replyTo: String,
    system: ActorSystem
) {

  private implicit val successResponseReader = Json.reads[SuccessResponse]
  private case class SuccessResponse(
      EnvId: String,
      RequestId: String
  )

  private implicit val errorResponseReader = Json.reads[ErrorResponse]
  private case class ErrorResponse(
      RequestId: String,
      HostId: String,
      Code: String,
      Message: String
  )

  def send(msg: Mailgun.Message): Funit =
    if (apiUrl.isEmpty) {
      println(msg, "No mailgun API URL")
      funit
    } else {
      lila.mon.email.actions.send()

      val accessKeyId = apiKey
      val accessKeySecret = apiSecret
      val format = "JSON"
      val regionId = "cn-hangzhou"
      val signatureMethod = "HMAC-SHA1"
      val signatureNonce = UUID.randomUUID().toString
      val signatureVersion = "1.0"
      val timestamp = AliYunApiHelper.getUTCTimeStr
      val version = "2015-11-23"
      val accountName = msg.from | from
      val action = "SingleSendMail"
      val addressType = "1"
      val htmlBody = msg.htmlBody ?? { body => Mailgun.html.wrap(msg.subject, body).render }
      val replyToAddress = "false"
      val subject = msg.subject
      val tagName = msg.tag.getOrElse("")
      val textBody = msg.text
      val toAddress = msg.to.value
      val signature = AliYunApiHelper.getSignature(new AliYunApiEmailParam(
        accessKeyId,
        accessKeySecret,
        format,
        regionId,
        signatureMethod,
        signatureNonce,
        signatureVersion,
        timestamp,
        version,
        accountName,
        action,
        addressType,
        htmlBody,
        replyToAddress,
        subject,
        tagName,
        textBody,
        toAddress
      ))

      WS.url(apiUrl).withHeaders("Content-Type" -> "application/x-www-form-urlencoded").post(Map(
        "AccessKeyId" -> Seq(apiKey),
        "Format" -> Seq(format),
        "RegionId" -> Seq(regionId),
        "SignatureMethod" -> Seq(signatureMethod),
        "SignatureNonce" -> Seq(signatureNonce),
        "SignatureVersion" -> Seq(signatureVersion),
        "Timestamp" -> Seq(timestamp),
        "Version" -> Seq(version),
        "Action" -> Seq(action),
        "AccountName" -> Seq(accountName),
        "AddressType" -> Seq(addressType),
        "ReplyToAddress" -> Seq(replyToAddress),
        "Subject" -> Seq(subject),
        "TagName" -> Seq(tagName),
        "TextBody" -> Seq(textBody),
        "ToAddress" -> Seq(toAddress),
        "Signature" -> Seq(signature)
      ) ++ msg.htmlBody.?? { body =>
          Map("HtmlBody" -> Seq(htmlBody))
        }).void addFailureEffect {
        case e: java.net.ConnectException => lila.mon.http.mailgun.timeout()
        case _ =>
      } recoverWith {
        case e if msg.retriesLeft > 0 => {
          lila.mon.email.actions.retry()
          akka.pattern.after(15 seconds, system.scheduler) {
            send(msg.copy(retriesLeft = msg.retriesLeft - 1))
          }
        }
        case e => {
          lila.mon.email.actions.fail()
          fufail(e)
        }
      }
    }
}

object Mailgun {

  case class Message(
      to: EmailAddress,
      subject: String,
      text: String,
      htmlBody: Option[Frag] = none,
      from: Option[String] = none,
      replyTo: Option[String] = none,
      tag: Option[String] = none,
      retriesLeft: Int = 3
  )

  object txt {

    def serviceNote(implicit lang: Lang): String =
      s"""
${trans.common_note.literalTo(lang, List("http://haichess.com")).render}

${trans.common_contact.literalTo(lang, List("http://haichess.com/contact")).render}"""
  }

  object html {

    val itemscope = attr("itemscope").empty
    val itemtype = attr("itemtype")
    val itemprop = attr("itemprop")
    val emailMessage = div(itemscope, itemtype := "http://schema.org/EmailMessage")
    val pDesc = p(itemprop := "description")
    val potentialAction = div(itemprop := "potentialAction", itemscope, itemtype := "http://schema.org/ViewAction")

    def metaName(cont: String) = meta(itemprop := "name", content := cont)

    val publisher = div(itemprop := "publisher", itemscope, itemtype := "http://schema.org/Organization")
    val noteContact = a(itemprop := "url", href := "http://haichess.com/contact")(
      span(itemprop := "name")("haichess.com/contact")
    )

    def serviceNote(implicit lang: Lang) = publisher(
      small(
        trans.common_note.literalTo(lang, List(Mailgun.html.noteLink)),
        " ",
        trans.common_contact.literalTo(lang, List(noteContact))
      )
    )

    def standardEmail(body: String)(implicit lang: Lang): Frag =
      emailMessage(
        pDesc(nl2brUnsafe(body)),
        publisher
      )

    val noteLink = a(
      itemprop := "url",
      href := "http://haichess.com/"
    )(span(itemprop := "name")("haichess.com"))

    def url(u: String)(implicit lang: Lang) = frag(
      meta(itemprop := "url", content := u),
      p(a(itemprop := "target", href := u)(u)),
      p(trans.common_orPaste.literalTo(lang))
    )

    private[Mailgun] def wrap(subject: String, body: Frag): Frag = frag(
      raw(
        s"""<!doctype html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <title>${escapeHtml(subject)}</title>
  </head>
  <body>"""
      ),
      body,
      raw(
        """
  </body>
</html>"""
      )
    )
  }

}
