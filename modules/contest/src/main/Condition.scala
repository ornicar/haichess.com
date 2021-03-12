package lila.contest

import lila.common.Lang
import lila.hub.lightTeam._
import lila.hub.lightClazz._
import lila.i18n.I18nKeys
import lila.rating.PerfType
import lila.user.{ User, Title }

sealed trait Condition {

  def name(lang: Lang): String

  def withVerdict(verdict: Condition.Verdict) = Condition.WithVerdict(this, verdict)
}

object Condition {

  trait FlatCond {

    def apply(user: User, contest: Contest): Condition.Verdict
  }

  type GetMaxRating = PerfType => Fu[Int]

  sealed abstract class Verdict(val accepted: Boolean)
  case object Accepted extends Verdict(true)
  case class Refused(reason: Lang => String) extends Verdict(false)

  case class WithVerdict(condition: Condition, verdict: Verdict)

  case object Titled extends Condition with FlatCond {
    def name(lang: Lang) = "仅称号棋手"
    def apply(user: User, contest: Contest) =
      if (user.title.exists(_ != Title.LM)) Accepted
      else Refused(name _)
  }

  case class NbRatedGame(perf: Option[PerfType], nb: Int) extends Condition with FlatCond {

    def apply(user: User, contest: Contest) =
      if (user.hasTitle) Accepted
      else perf match {
        case Some(p) if user.perfs(p).nb >= nb => Accepted
        case Some(p) => Refused { lang =>
          val missing = nb - user.perfs(p).nb
          I18nKeys.needNbMorePerfGames.pluralTxtTo(lang, missing, List(missing, p.name))
        }
        case None if user.count.rated >= nb => Accepted
        case None => Refused { lang =>
          val missing = nb - user.count.rated
          I18nKeys.needNbMoreGames.pluralTxtTo(lang, missing, List(missing))
        }
      }

    def name(lang: Lang) = perf match {
      case None => I18nKeys.moreThanNbRatedGames.pluralTxtTo(lang, nb, List(nb))
      case Some(p) => I18nKeys.moreThanNbPerfRatedGames.pluralTxtTo(lang, nb, List(nb, p.name))
    }
  }

  case class MaxRating(perf: PerfType, rating: Int) extends Condition {

    def apply(getMaxRating: GetMaxRating)(user: User): Fu[Verdict] =
      if (user.perfs(perf).provisional) fuccess(Refused { lang =>
        I18nKeys.yourPerfRatingIsProvisional.literalTxtTo(lang, perf.name)
      })
      else if (user.perfs(perf).intRating > rating) fuccess(Refused { lang =>
        I18nKeys.yourPerfRatingIsTooHigh.literalTxtTo(lang, List(perf.name, user.perfs(perf).intRating))
      })
      else getMaxRating(perf) map {
        case r if r <= rating => Accepted
        case r => Refused { lang =>
          I18nKeys.yourTopWeeklyPerfRatingIsTooHigh.literalTxtTo(lang, List(perf.name, r))
        }
      }

    def maybe(user: User): Boolean =
      !user.perfs(perf).provisional && user.perfs(perf).intRating <= rating

    def name(lang: Lang) = I18nKeys.ratedLessThanInPerf.literalTxtTo(lang, List(rating, perf.name))
  }

  case class MinRating(perf: PerfType, rating: Int) extends Condition with FlatCond {

    def apply(user: User, contest: Contest) =
      if (user.hasTitle) Accepted
      else if (user.perfs(perf).provisional) Refused { lang =>
        I18nKeys.yourPerfRatingIsProvisional.literalTxtTo(lang, perf.name)
      }
      else if (user.perfs(perf).intRating < rating) Refused { lang =>
        I18nKeys.yourPerfRatingIsTooLow.literalTxtTo(lang, List(perf.name, user.perfs(perf).intRating))
      }
      else Accepted

    def name(lang: Lang) = I18nKeys.ratedMoreThanInPerf.literalTxtTo(lang, List(rating, perf.name))
  }

  case class MinLevel(lv: String) extends Condition with FlatCond {

    val level = lila.user.FormSelect.Level.byKey(lv)

    def apply(user: User, contest: Contest) =
      if (user.profileOrDefault.ofLevel.order >= level.order) Accepted
      else Refused(name _)

    def name(lang: Lang) = s"级别必须大于等于${level.name}"
  }

  case class MaxLevel(lv: String) extends Condition with FlatCond {

    val level = lila.user.FormSelect.Level.byKey(lv)

    def apply(user: User, contest: Contest) =
      if (user.profileOrDefault.ofLevel.order <= level.order) Accepted
      else Refused(name _)

    def name(lang: Lang) = s"级别必须小于等于${level.name}"
  }

  case class MinAge(age: Int) extends Condition with FlatCond {
    def apply(user: User, contest: Contest) =
      if (user.profileOrDefault.age ?? (_ >= age)) Accepted
      else Refused(name _)

    def name(lang: Lang) = s"年龄必须大于等于${age}"
  }

  case class MaxAge(age: Int) extends Condition with FlatCond {

    def apply(user: User, contest: Contest) =
      if (user.profileOrDefault.age ?? (_ <= age)) Accepted
      else Refused(name _)

    def name(lang: Lang) = s"年龄必须小于等于${age}"
  }

  case class Sex(s: String) extends Condition with FlatCond {
    val sex = lila.user.FormSelect.Sex.byKey(s)
    def apply(user: User, contest: Contest) =
      if (user.profileOrDefault.ofSex ?? (_.key == sex.key)) Accepted
      else Refused(name _)

    def name(lang: Lang) = s"仅限${sex.name}生，请确认个人资料限制"
  }

  case class TeamMember(teamId: TeamId, teamName: TeamName) extends Condition {
    def name(lang: Lang) = I18nKeys.mustBeInTeam.literalTxtTo(lang, List(teamName))
    def apply(user: User, getUserTeamIds: User => Fu[TeamIdList]) =
      getUserTeamIds(user) map { userTeamIds =>
        if (userTeamIds contains teamId) Accepted
        else Refused { lang => I18nKeys.youAreNotInTeam.literalTxtTo(lang, List(teamName)) }
      }
  }

  case class ClazzMember(clazzId: ClazzId, clazzName: ClazzName) extends Condition {
    def name(lang: Lang) = s"您必须在班级 $clazzName 中"
    def apply(user: User, getUserClazzIds: User => Fu[ClazzIdList]) =
      getUserClazzIds(user) map { userClazzIds =>
        if (userClazzIds contains clazzId) Accepted
        else Refused { _ => s"您不在班级 $clazzName 中" }
      }
  }

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      maxLevel: Option[MaxLevel],
      minLevel: Option[MinLevel],
      maxAge: Option[MaxAge],
      minAge: Option[MinAge],
      sex: Option[Sex],
      titled: Option[Titled.type],
      teamMember: Option[TeamMember],
      clazzMember: Option[ClazzMember]
  ) {

    lazy val list: List[Condition] = List(nbRatedGame, maxRating, minRating, maxLevel, minLevel, maxAge, minAge, sex, titled, teamMember, clazzMember).flatten

    def relevant = list.nonEmpty

    def ifNonEmpty = list.nonEmpty option this

    def withVerdicts(
      contest: Contest,
      user: User,
      getMaxRating: GetMaxRating,
      getUserTeamIds: User => Fu[TeamIdList],
      getUserClazzIds: User => Fu[ClazzIdList]
    ): Fu[All.WithVerdicts] =

      list.map {
        case c: MaxRating => c(getMaxRating)(user) map c.withVerdict
        case c: FlatCond => fuccess(c withVerdict c(user, contest))
        case c: TeamMember => c(user, getUserTeamIds) map { c withVerdict _ }
        case c: ClazzMember => c(user, getUserClazzIds) map { c withVerdict _ }
      }.sequenceFu map All.WithVerdicts.apply

    def accepted = All.WithVerdicts(list.map { WithVerdict(_, Accepted) })

    def sameMaxRating(other: All) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
    def sameMinRating(other: All) = minRating.map(_.rating) == other.minRating.map(_.rating)
    def sameRatings(other: All) = sameMaxRating(other) && sameMinRating(other)
    def similar(other: All) = sameRatings(other) && titled == other.titled && teamMember == other.teamMember
    def isRatingLimited = maxRating.isDefined || minRating.isDefined
  }

  object All {
    val empty = All(
      nbRatedGame = none,
      maxRating = none,
      minRating = none,
      maxLevel = none,
      minLevel = none,
      maxAge = none,
      minAge = none,
      sex = none,
      titled = none,
      teamMember = none,
      clazzMember = none
    )

    case class WithVerdicts(list: List[WithVerdict]) extends AnyVal {
      def relevant = list.nonEmpty
      def accepted = list.forall(_.verdict.accepted)
    }
  }

  final class Verify(historyApi: lila.history.HistoryApi) {

    def apply(
      contest: Contest,
      user: User,
      getUserTeamIds: User => Fu[TeamIdList],
      getUserClazzIds: User => Fu[ClazzIdList]
    ): Fu[All.WithVerdicts] = {
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      contest.conditions.withVerdicts(contest, user, getMaxRating, getUserTeamIds, getUserClazzIds)
    }

    def canEnter(
      user: User,
      getUserTeamIds: User => Fu[TeamIdList],
      getUserClazzIds: User => Fu[ClazzIdList]
    )(contest: Contest): Fu[Boolean] =
      apply(contest, user, getUserTeamIds, getUserClazzIds).map(_.accepted)
  }

  object BSONHandlers {
    import reactivemongo.bson._
    import lila.rating.BSONHandlers.perfTypeKeyHandler
    private implicit val NbRatedGameHandler = Macros.handler[NbRatedGame]
    private implicit val MaxRatingHandler = Macros.handler[MaxRating]
    private implicit val MinRatingHandler = Macros.handler[MinRating]
    private implicit val MaxAgeHandler = Macros.handler[MaxAge]
    private implicit val MinAgeHandler = Macros.handler[MinAge]
    private implicit val MaxLevelHandler = Macros.handler[MaxLevel]
    private implicit val MinLevelHandler = Macros.handler[MinLevel]
    private implicit val SexHandler = Macros.handler[Sex]
    private implicit val TitledHandler = new BSONHandler[BSONValue, Titled.type] {
      def read(x: BSONValue) = Titled
      def write(x: Titled.type) = BSONBoolean(true)
    }
    private implicit val TeamMemberHandler = Macros.handler[TeamMember]
    private implicit val ClazzMemberHandler = Macros.handler[ClazzMember]
    implicit val AllBSONHandler = Macros.handler[All]
  }

  object JSONHandlers {
    import play.api.libs.json._
    private implicit val perfTypeWriter: OWrites[PerfType] = OWrites { pt =>
      Json.obj("key" -> pt.key, "name" -> pt.name)
    }

    def verdictsFor(verdicts: All.WithVerdicts, lang: Lang) = Json.obj(
      "list" -> verdicts.list.map {
        case WithVerdict(cond, verd) => Json.obj(
          "condition" -> (cond name lang),
          "verdict" -> (verd match {
            case Refused(reason) => reason(lang)
            case Accepted => JsString("ok")
          })
        )
      },
      "accepted" -> verdicts.accepted
    )
  }

  object DataForm {
    import play.api.data.Forms._
    import lila.common.Form._
    val perfAuto = "auto" -> "自动"
    val perfChoices = perfAuto :: PerfType.nonPuzzle.map { pt =>
      pt.key -> pt.name
    }
    val nbRatedGames = Seq(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
    val nbRatedGameChoices = options(nbRatedGames, "%d 盘") map {
      case (0, name) => (0, "无限制")
      case x => x
    }

    case class NbRatedGameSetup(perf: Option[String], nb: Int) {
      def convert(tourPerf: PerfType): Option[NbRatedGame] = nb > 0 option NbRatedGame(
        if (perf has perfAuto._1) tourPerf.some else PerfType(~perf),
        nb
      )
    }
    object NbRatedGameSetup {
      def apply(x: NbRatedGame): NbRatedGameSetup = NbRatedGameSetup(x.perf.map(_.key), x.nb)
    }
    case class RatingSetup(perf: Option[String], rating: Option[Int]) {
      def actualRating = rating.filter(r => r > 600 && r < 3000)
      def convert[A](tourPerf: PerfType)(f: (PerfType, Int) => A): Option[A] =
        actualRating map { r =>
          f(perf.flatMap(PerfType.apply) | tourPerf, r)
        }
    }
    object RatingSetup {
      def apply(v: (Option[PerfType], Option[Int])): RatingSetup = RatingSetup(v._1.map(_.key), v._2)
    }
    val maxRatings = List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1000).reverse
    val maxRatingChoices = ("", "不限") :: options(maxRatings, "%d 分").toList.map { case (k, v) => k.toString -> v }
    val minRatings = List(1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300, 2400, 2500, 2600)
    val minRatingChoices = ("", "不限") :: options(minRatings, "%d 分").toList.map { case (k, v) => k.toString -> v }
    val levelChoices = ("", "不限") :: lila.user.FormSelect.Level.list.filterNot(_._1 == "-")
    val sexChoices = ("", "不限") :: lila.user.FormSelect.Sex.list

    case class TeamMemberSetup(teamId: Option[TeamId]) {
      def convert(teams: Map[TeamId, TeamName]): Option[TeamMember] =
        teamId flatMap { id =>
          teams.get(id) map { TeamMember(id, _) }
        }
    }
    object TeamMemberSetup {
      def apply(x: TeamMember): TeamMemberSetup = TeamMemberSetup(x.teamId.some)
    }

    case class ClazzMemberSetup(clazzId: Option[ClazzId]) {
      def convert(clazzs: Map[ClazzId, ClazzName]): Option[ClazzMember] =
        clazzId flatMap { id =>
          clazzs.get(id) map { ClazzMember(id, _) }
        }
    }
    object ClazzMemberSetup {
      def apply(x: ClazzMember): ClazzMemberSetup = ClazzMemberSetup(x.clazzId.some)
    }

    val nbRatedGame = mapping(
      "perf" -> optional(text.verifying(perfChoices.toMap.contains _)),
      "nb" -> numberIn(nbRatedGameChoices)
    )(NbRatedGameSetup.apply)(NbRatedGameSetup.unapply)

    val maxRating = mapping(
      "perf" -> optional(text.verifying(perfChoices.toMap.contains _)),
      "rating" -> optional(numberIn(minRatings))
    )(RatingSetup.apply)(RatingSetup.unapply)

    val minRating = mapping(
      "perf" -> optional(text.verifying(perfChoices.toMap.contains _)),
      "rating" -> optional(numberIn(maxRatings))
    )(RatingSetup.apply)(RatingSetup.unapply)

    val teamMember = mapping(
      "teamId" -> optional(text)
    )(TeamMemberSetup.apply)(TeamMemberSetup.unapply)

    val clazzMember = mapping(
      "clazzId" -> optional(text)
    )(ClazzMemberSetup.apply)(ClazzMemberSetup.unapply)

    val all = mapping(
      "nbRatedGame" -> optional(nbRatedGame),
      "maxRating" -> maxRating,
      "minRating" -> minRating,
      "maxLevel" -> optional(text.verifying(lila.user.FormSelect.Level.keySet contains _)),
      "minLevel" -> optional(text.verifying(lila.user.FormSelect.Level.keySet contains _)),
      "maxAge" -> optional(number(min = 1, max = 100)),
      "minAge" -> optional(number(min = 1, max = 100)),
      "sex" -> optional(text.verifying(lila.user.FormSelect.Sex.keySet contains _)),
      "titled" -> optional(boolean),
      "teamMember" -> optional(teamMember),
      "clazzMember" -> optional(clazzMember)
    )(AllSetup.apply)(AllSetup.unapply)
      .verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        nbRatedGame: Option[NbRatedGameSetup],
        maxRating: RatingSetup,
        minRating: RatingSetup,
        maxLevel: Option[String],
        minLevel: Option[String],
        maxAge: Option[Int],
        minAge: Option[Int],
        sex: Option[String],
        titled: Option[Boolean],
        teamMember: Option[TeamMemberSetup],
        clazzMember: Option[ClazzMemberSetup]
    ) {

      def validRatings = (minRating.actualRating, maxRating.actualRating) match {
        case (Some(min), Some(max)) => min < max
        case _ => true
      }

      def convert(perf: PerfType, teams: Map[String, String], clazzs: Map[String, String]) = All(
        nbRatedGame.flatMap(_ convert perf),
        maxRating.convert(perf)(MaxRating.apply),
        minRating.convert(perf)(MinRating.apply),
        maxLevel map MaxLevel,
        minLevel map MinLevel,
        maxAge map MaxAge,
        minAge map MinAge,
        sex map Sex,
        ~titled option Titled,
        teamMember.flatMap(_ convert teams),
        clazzMember.flatMap(_ convert clazzs)
      )
    }

    object AllSetup {

      val default = AllSetup(
        nbRatedGame = none,
        maxRating = RatingSetup(none, none),
        minRating = RatingSetup(none, none),
        maxLevel = none,
        minLevel = none,
        maxAge = none,
        minAge = none,
        sex = none,
        titled = none,
        teamMember = none,
        clazzMember = none
      )

      def apply(all: All): AllSetup = AllSetup(
        nbRatedGame = all.nbRatedGame.map(NbRatedGameSetup.apply),
        maxRating = RatingSetup(all.maxRating.map(_.perf.key), all.maxRating.map(_.rating)),
        minRating = RatingSetup(all.minRating.map(_.perf.key), all.minRating.map(_.rating)),
        maxLevel = all.maxLevel.map(_.lv),
        minLevel = all.minLevel.map(_.lv),
        maxAge = all.maxAge.map(_.age),
        minAge = all.minAge.map(_.age),
        sex = all.sex.map(_.s),
        titled = all.titled has Titled option true,
        teamMember = all.teamMember.map(TeamMemberSetup.apply),
        clazzMember = all.clazzMember.map(ClazzMemberSetup.apply)
      )
    }
  }
}
