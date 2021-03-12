package lila.team

import lila.db.dsl._

object TagRepo {

  // dirty
  private val coll = Env.current.colls.tag

  type ID = String
  import BSONHandlers._

  def byId(id: ID): Fu[Option[Tag]] = coll.byId[Tag](id)

  def findByTeam(teamId: ID): Fu[List[Tag]] =
    coll.list[Tag](teamQuery(teamId) ++ $doc("field" $nin Tag.oldDefault))

  def create(tag: Tag) = coll.insert(tag)

  def remove(id: ID) = coll.remove($id(id))

  def teamQuery(teamId: ID) = $doc("team" -> teamId)

  def editableQuery = $doc("editable" -> true)

}
