package lila.offlineContest

import play.api.data._
import play.api.data.Forms._
import lila.common.Form._
import lila.user.User
import lila.game.Game

final class DataForm {

  import DataForm._

  def contest(user: User, id: Option[OffContest.ID]) = Form(mapping(
    "name" -> nonEmptyText(minLength = 2, maxLength = 30),
    "groupName" -> optional(nonEmptyText(minLength = 2, maxLength = 30)),
    "logo" -> optional(text(minLength = 5, maxLength = 150)),
    "typ" -> stringIn(OffContest.Type.list),
    "teamRated" -> boolean,
    "organizer" -> nonEmptyText(minLength = 6, maxLength = 8),
    "rule" -> stringIn(OffContest.Rule.list),
    "rounds" -> number(min = 1, max = 16),
    "swissBtss" -> list(stringIn(OffBtss.list)),
    "roundRobinBtss" -> list(stringIn(OffBtss.list))
  )(OffContestSetup.apply)(OffContestSetup.unapply)
    //.verifying("比赛名称重复", !_.validName(id).awaitSeconds(2))
    .verifying("非认证俱乐部不能创建公开赛", _.validPublic(user)))

  def contestDefault(user: User) = contest(user, None) fill OffContestSetup.default

  def contestOf(user: User, c: OffContest) = contest(user, c.id.some) fill OffContestSetup(
    name = c.name,
    groupName = c.groupName,
    logo = c.logo,
    typ = c.typ.id,
    teamRated = c.teamRated,
    organizer = c.organizer,
    rule = c.rule.id,
    rounds = c.rounds,
    swissBtss = c.swissBtss.list.map(_.id),
    roundRobinBtss = c.roundRobinBtss.list.map(_.id)
  )

  val manualPairingForm = Form(mapping(
    "source" -> manualPairingPlayerMapping,
    "target" -> manualPairingPlayerMapping
  )(ManualPairing.apply)(ManualPairing.unapply))

  def manualPairingPlayerMapping = mapping(
    "isBye" -> numberIn(booleanChoices),
    "board" -> optional(nonEmptyText(minLength = 8, maxLength = 8)),
    "color" -> optional(numberIn(booleanChoices)),
    "player" -> optional(nonEmptyText(minLength = 10, maxLength = 50))
  )(ManualPairingPlayer.apply)(ManualPairingPlayer.unapply)

  def forbidden = Form(mapping(
    "name" -> nonEmptyText(minLength = 2, maxLength = 20),
    "playerIds" -> nonEmptyText(minLength = 2, maxLength = 600)
  )(ForbiddenData.apply)(ForbiddenData.unapply))

  def forbiddenOf(f: OffForbidden) = forbidden fill ForbiddenData(f.name, f.playerIds.mkString(","))

}

object DataForm {

  val booleanChoices = Seq(0 -> "否", 1 -> "是")

}

case class OffContestSetup(
    name: String,
    groupName: Option[String],
    logo: Option[String],
    typ: String,
    teamRated: Boolean,
    organizer: String,
    rule: String,
    rounds: Int,
    swissBtss: List[String],
    roundRobinBtss: List[String]
) {

  def toContest(user: User, myTeams: List[(String, String)], myClazzs: List[(String, String)]): OffContest = OffContest.make(
    by = user.id,
    name = name,
    groupName = groupName,
    logo = logo,
    typ = OffContest.Type(typ),
    teamRated = teamRated,
    organizer = organizer,
    rule = OffContest.Rule(rule),
    rounds = rounds,
    swissBtss = OffBtsss(swissBtss.map(OffBtss(_))),
    roundRobinBtss = OffBtsss(roundRobinBtss.map(OffBtss(_)))
  )

  def roundList(contestId: OffContest.ID): List[OffRound] =
    (1 to rounds) map { no =>
      OffRound.make(
        no = no,
        contestId = contestId
      )
    } toList

  def validName(id: Option[OffContest.ID]): Fu[Boolean] = OffContestRepo.nameExists(name.trim, groupName.map(_.trim), id)
  def validPublic(user: User) = {
    OffContest.Type(typ) match {
      case OffContest.Type.Public => user.isTeam
      case _ => true
    }
  }
}

object OffContestSetup {

  def default: OffContestSetup = {
    OffContestSetup(
      name = "",
      groupName = None,
      logo = None,
      typ = "public",
      teamRated = false,
      organizer = "",
      rule = OffContest.Rule.Swiss.id,
      rounds = 5,
      swissBtss = OffBtss.swissDefault.map(_.id),
      roundRobinBtss = OffBtss.roundRobinDefault.map(_.id)
    )
  }
}

case class ManualPairing(source: ManualPairingPlayer, target: ManualPairingPlayer)
case class ManualPairingPlayer(isBye: Int, board: Option[Game.ID], color: Option[Int], player: Option[OffPlayer.ID]) {
  def board_ = board.get
  def color_ = color.get
  def player_ = player.get
  def isBye_ = isBye == 1
}

case class ForbiddenData(name: String, playerIds: String) {

  def toForbidden(contestId: String, forbidden: Option[OffForbidden]) = {
    forbidden.fold(
      OffForbidden.make(
        name = name,
        contestId = contestId,
        playerIds = toList
      )
    ) { fb =>
        fb.copy(
          name = name,
          playerIds = toList
        )
      }
  }

  def toList = playerIds.split(",").toList

}