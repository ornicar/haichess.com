package lila.memo

import scala.concurrent.duration._
import play.api.mvc.{ RequestHeader, Result, Results }
import ornicar.scalalib.Zero
import lila.common.{ HTTPRequest, IpAddress, UserCellphone }

object SmsRateLimit {

  private lazy val rateLimitPerIP = new RateLimit[IpAddress](
    credits = 5,
    duration = 1 hour,
    name = "Confirm cellphone per IP",
    key = "cellphone.confirm.ip"
  )

  private lazy val rateLimitPerUser = new RateLimit[String](
    credits = 3,
    duration = 1 hour,
    name = "Confirm cellphone per user",
    key = "cellphone.confirm.user"
  )

  private lazy val rateLimitPerCellphone = new RateLimit[String](
    credits = 3,
    duration = 1 hour,
    name = "Confirm cellphone per mobile",
    key = "cellphone.confirm.cellphone"
  )

  def rateLimit(userCellphone: UserCellphone, req: RequestHeader)(run: => Fu[Result]): Fu[Result] = {
    implicit val limitedDefault = Zero.instance[Fu[Result]](fuccess(Results.TooManyRequest("Too many requests, try again later.")))
    rateLimitPerUser(userCellphone.username, cost = 1) {
      rateLimitPerCellphone(userCellphone.cellphone.value, cost = 1) {
        rateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = 1) {
          run
        }
      }
    }
  }

}
