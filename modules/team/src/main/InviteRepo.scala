package lila.team

import lila.db.dsl._

object InviteRepo {

  // dirty
  private val coll = Env.current.colls.invite

  import BSONHandlers._

  type ID = String

  def exists(teamId: ID, userId: ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def find(teamId: ID, userId: ID): Fu[Option[Invite]] =
    coll.uno[Invite](selectId(teamId, userId))

  def countByTeam(teamId: ID): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def countByTeams(teamIds: List[ID]): Fu[Int] =
    coll.countSel(teamsQuery(teamIds))

  def findByTeam(teamId: ID): Fu[List[Invite]] =
    coll.list[Invite](teamQuery(teamId))

  def findByTeams(teamIds: List[ID]): Fu[List[Invite]] =
    coll.list[Invite](teamsQuery(teamIds))

  def selectId(teamId: ID, userId: ID) = $id(Invite.makeId(teamId, userId))
  def teamQuery(teamId: ID) = $doc("team" -> teamId)
  def teamsQuery(teamIds: List[ID]) = $doc("team" $in teamIds)

  def getByUserId(userId: lila.user.User.ID) =
    coll.find($doc("user" -> userId)).list[Invite]()

  def remove(id: ID) = coll.remove($id(id))
}
