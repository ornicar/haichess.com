package lila.memo

import scala.concurrent.duration._
import play.api.mvc.{ RequestHeader, Result, Results }
import ornicar.scalalib.Zero
import lila.common.{ HTTPRequest, IpAddress }

object UploadRateLimit {

  private lazy val rateLimitPerIP = new RateLimit[IpAddress](
    credits = 20,
    duration = 1 day,
    name = "Upload per IP",
    key = "upload.ip"
  )

  private lazy val rateLimitPerUser = new RateLimit[String](
    credits = 20,
    duration = 1 day,
    name = "Upload per user",
    key = "upload.user"
  )

  def rateLimit(username: String, req: RequestHeader)(run: => Fu[Result]): Fu[Result] = {
    implicit val limitedDefault = Zero.instance[Fu[Result]](fuccess(Results.TooManyRequest("Too many requests, try again later.")))
    rateLimitPerUser(username, cost = 1) {
      rateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = 1) {
        run
      }
    }
  }

}
