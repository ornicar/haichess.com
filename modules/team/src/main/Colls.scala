package lila.team

import lila.db.dsl.Coll

private final class Colls(
    val team: Coll,
    val request: Coll,
    val invite: Coll,
    val member: Coll,
    val tag: Coll,
    val rating: Coll
)
