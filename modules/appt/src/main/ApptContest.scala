package lila.appt

import lila.contest.Contest

case class ApptContest(
    id: Contest.ID,
    name: String,
    logo: Option[String],
    roundNo: Int,
    boardNo: Int
) {

}

object ApptContest {

  def make(c: Contest, roundNo: Int, boardNo: Int) = ApptContest(
    id = c.id,
    name = c.fullName,
    logo = c.logo,
    roundNo = roundNo,
    boardNo = boardNo
  )

}
