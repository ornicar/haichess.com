package lila.appt

import play.api.libs.json._

final class JsonView() {

  def apptsJson(appt: List[Appt]) =
    JsArray(
      appt.map(apptJson _)
    )

  def apptJson(appt: Appt): JsObject = Json.obj(
    "id" -> appt.id,
    "game" -> game(appt),
    "position" -> appt.position,
    "minDateTime" -> appt.minDateTime.toString("yyyy-MM-dd HH:mm"),
    "maxDateTime" -> appt.maxDateTime.toString("yyyy-MM-dd HH:mm"),
    "whitePlayerUid" -> appt.whitePlayerUid,
    "blackPlayerUid" -> appt.blackPlayerUid,
    "confirmed" -> appt.confirmed,
    "finalTime" -> appt.finalTime,
    "createBy" -> appt.createBy,
    "record" -> recordJson(appt.currentRecord),
    "source" -> appt.source
  ).add("contest" -> contestJson(appt.contest))

  def contestJson(contest: Option[ApptContest]): Option[JsObject] =
    contest.map { c =>
      Json.obj(
        "id" -> c.id,
        "name" -> c.name,
        "logo" -> c.logo,
        "roundNo" -> c.roundNo,
        "boardNo" -> c.boardNo
      )
    }

  def recordJson(r: ApptRecord): JsObject =
    Json.obj(
      "id" -> r.id,
      "time" -> r.time.toString("yyyy-MM-dd HH:mm"),
      "message" -> r.message,
      "current" -> r.current,
      "whiteStatus" -> r.whiteStatus.id,
      "blackStatus" -> r.blackStatus.id,
      "applyBy" -> r.applyBy
    )

  def game(appt: Appt) = {
    val separator = " • "
    s"${appt.showClock}$separator${if (appt.rated) "有积分" else "无积分"}$separator${appt.variant.name}"
  }

}

