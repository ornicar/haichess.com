package lila.team

import lila.common.paginator._
import lila.common.MaxPerPage
import lila.db.dsl._
import lila.db.paginator._
import lila.user.UserRepo
import org.joda.time.DateTime

private[team] final class PaginatorBuilder(
    coll: Colls,
    maxPerPage: MaxPerPage,
    maxUserPerPage: MaxPerPage
) {

  import BSONHandlers._

  def popularTeams(page: Int, text: Option[String] = None): Fu[Paginator[Team]] = Paginator(
    adapter = new Adapter(
      collection = coll.team,
      selector = TeamRepo.enabledQuery ++ text.?? { t =>
        $or(
          $doc("_id" -> t.trim),
          $doc("name" -> t.trim)
        )
      },
      projection = $empty,
      sort = TeamRepo.sortPopular
    ),
    page,
    maxPerPage
  )

  def teamMembers(team: Team, page: Int, searchData: MemberSearch): Fu[Paginator[MemberWithUser]] = Paginator(
    adapter = new TeamAdapter(team, searchData),
    page,
    maxPerPage
  )

  private final class TeamAdapter(team: Team, searchData: MemberSearch) extends AdapterLike[MemberWithUser] {

    val nbResults = fuccess(team.nbMembers)

    def slice(offset: Int, length: Int): Fu[Seq[MemberWithUser]] = for {
      docs ← buildSelector(team.id, searchData)
      members ← coll.member.find(docs).sort($doc("role" -> 1, "_id" -> 1)).list[Member]()
      users ← UserRepo.find(buildUserSelector(searchData, members.map(_.user))).sort($doc("_id" -> 1)).list[lila.user.User]()
    } yield {
      users.map { u =>
        MemberWithUser(members.find(m => m.user == u.id) err s"can not find member ${u.id}", u)
      }.sortBy(_.member.role.sort).slice(offset, offset + length)
    }

    private def buildSelector(teamId: String, searchData: MemberSearch) = {
      TagRepo.findByTeam(teamId).map { tags =>
        var doc = $doc("team" -> teamId)
        searchData.clazzId.foreach(c =>
          doc = doc ++ $doc("clazzIds" -> c))
        searchData.username.foreach(u =>
          doc = doc ++ $or($doc("user" $regex (u, "i")), $doc("mark" $regex (u, "i"))))
        searchData.role.foreach(r =>
          doc = doc ++ $doc("role" -> r))
        searchData.fields.foreach(f =>
          f.value.foreach(v =>
            if (tags.exists(t => t.field == f.field && t.typ == Tag.Type.Text)) {
              doc = doc ++ $doc(s"tags.${f.field}.value" $regex (v, "i"))
            } else {
              doc = doc ++ $doc(s"tags.${f.field}.value" -> v)
            }))
        searchData.rangeFields.foreach(rf => {
          var range = $doc()
          rf.min.foreach(v =>
            range = range ++ $gte(v))
          rf.max.foreach(v =>
            range = range ++ $lte(v))
          if (!range.isEmpty) {
            doc = doc ++ $doc(s"tags.${rf.field}.value" -> range)
          }
        })
        doc
      }
    }

    private def buildUserSelector(searchData: MemberSearch, userIds: List[lila.user.User.ID]) = {
      var doc = $doc("_id" $in userIds)
      searchData.age.foreach { a =>
        doc = doc ++ $doc("profile.birthyear" -> (DateTime.now.getYear - a))
      }
      searchData.sex.foreach { s =>
        doc = doc ++ $doc("profile.sex" -> s)
      }
      searchData.level.foreach { l =>
        doc = doc ++ $doc("profile.levels" -> $doc("level" -> l, "current" -> 1))
      }
      doc
    }

    private def sorting = $sort desc "date"
  }
}
