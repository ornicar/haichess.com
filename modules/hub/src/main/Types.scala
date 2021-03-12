package lila.hub

package object lightTeam {
  type TeamId = String
  type TeamName = String
  type TeamIdList = List[TeamId]
  type TeamIdsWithNames = List[(TeamId, TeamName)]
}

package object lightClazz {
  type ClazzId = String
  type ClazzName = String
  type ClazzIdList = List[ClazzId]
  type ClazzIdsWithNames = List[(ClazzId, ClazzName)]
}
