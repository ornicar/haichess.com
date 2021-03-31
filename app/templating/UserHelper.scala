package lila.app
package templating

import mashup._
import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.common.{ LightUser }
import lila.i18n.{ I18nKeys => trans }
import lila.rating.{ Perf, PerfType }
import lila.user.{ Title, User, UserContext }
import controllers.routes

trait UserHelper { self: I18nHelper with StringHelper with NumberHelper =>

  def ratingProgress(progress: Int) =
    if (progress > 0) goodTag(cls := "rp")(progress)
    else if (progress < 0) badTag(cls := "rp")(math.abs(progress))
    else emptyFrag

  val topBarSortedPerfTypes: List[PerfType] = List(
    PerfType.Bullet,
    PerfType.Chess960,
    PerfType.Blitz,
    PerfType.KingOfTheHill,
    PerfType.Rapid,
    PerfType.ThreeCheck,
    PerfType.Classical,
    PerfType.Antichess,
    PerfType.Correspondence,
    PerfType.Atomic,
    PerfType.Horde,
    PerfType.Crazyhouse
  )

  def showPerfRating(rating: Int, name: String, nb: Int, provisional: Boolean, icon: Char)(implicit ctx: Context): Frag =
    span(
      title := s"$name ${nb.localize} 场积分对局",
      dataIcon := icon,
      cls := "text"
    )(
        if (nb > 0) frag(rating, provisional option "?")
        else frag(nbsp, nbsp, nbsp, "-")
      )

  def showPerfRating(perfType: PerfType, perf: Perf)(implicit ctx: Context): Frag =
    showPerfRating(perf.intRating, perfType.name, perf.nb, perf.provisional, perfType.iconChar)

  def showPerfRating(u: User, perfType: PerfType)(implicit ctx: Context): Frag =
    showPerfRating(perfType, u perfs perfType)

  def showPerfRating(u: User, perfKey: String)(implicit ctx: Context): Option[Frag] =
    PerfType(perfKey) map { showPerfRating(u, _) }

  def showBestPerf(u: User)(implicit ctx: Context): Option[Frag] = u.perfs.bestPerf map {
    case (pt, perf) => showPerfRating(pt, perf)
  }
  def showBestPerfs(u: User, nb: Int)(implicit ctx: Context): List[Frag] =
    u.perfs.bestPerfs(nb) map {
      case (pt, perf) => showPerfRating(pt, perf)
    }

  def showRatingDiff(diff: Int): Frag = diff match {
    case 0 => span("±0")
    case d if d > 0 => goodTag(s"+$d")
    case d => badTag(s"−${-d}")
  }

  def lightUser(userId: String): Option[LightUser] = Env.user lightUserSync userId
  def lightUser(userId: Option[String]): Option[LightUser] = userId flatMap lightUser

  // def lightUserSync: LightUser.SyncGetter(userId: String): Option[LightUser] = Env.user lightUserSync userId

  def usernameOrId(userId: String) = lightUser(userId).fold(userId)(_.titleName)
  def usernameOrAnon(userId: Option[String]) = lightUser(userId).fold(User.anonymous)(_.titleName)

  def isOnline(userId: String) = Env.user isOnline userId

  def isStreaming(userId: String) = Env.streamer.liveStreamApi isStreaming userId

  def userIdLink(
    userIdOption: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withTitle: Boolean = true,
    truncate: Option[Int] = None,
    params: String = "",
    modIcon: Boolean = false,
    withBadge: Boolean = true
  ): Frag =
    userIdOption.flatMap(lightUser).fold[Frag](User.anonymous) { user =>
      //println(user.id, user.isSilver, user.isGold, user.isCoach, user.isTeam)
      userIdNameLink(
        userId = user.id,
        username = user.name,
        isPatron = user.isPatron,
        title = withTitle ?? user.title map Title.apply,
        head = user.head,
        cssClass = cssClass,
        withOnline = withOnline,
        truncate = truncate,
        params = params,
        modIcon = modIcon,
        silver = user.isSilver,
        gold = user.isGold,
        coach = user.isCoach,
        team = user.isTeam,
        withBadge = withBadge
      )
    }

  def lightUserLink(
    user: LightUser,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withTitle: Boolean = true,
    truncate: Option[Int] = None,
    params: String = ""
  ): Frag = userIdNameLink(
    userId = user.id,
    username = user.name,
    isPatron = user.isPatron,
    title = withTitle ?? user.title map Title.apply,
    head = user.head,
    cssClass = cssClass,
    withOnline = withOnline,
    truncate = truncate,
    params = params,
    modIcon = false,
    silver = user.isSilver,
    gold = user.isGold,
    coach = user.isCoach,
    team = user.isTeam
  )

  def userIdLink(
    userId: String,
    cssClass: Option[String]
  ): Frag = userIdLink(userId.some, cssClass)

  def titleTag(title: Option[Title]): Option[Frag] = title map { t =>
    frag(
      span(
        cls := s"title${(t == Title.BOT) ?? " data-bot"}",
        st.title := Title.titleName(t)
      )(t),
      nbsp
    )
  }
  def titleTag(lu: LightUser): Frag = titleTag(lu.title map Title.apply)

  private def userIdNameLink(
    userId: String,
    username: String,
    isPatron: Boolean,
    cssClass: Option[String],
    withOnline: Boolean,
    truncate: Option[Int],
    title: Option[Title],
    head: Option[String],
    params: String,
    modIcon: Boolean,
    silver: Boolean,
    gold: Boolean,
    coach: Boolean,
    team: Boolean,
    withBadge: Boolean = true
  ): Frag = a(
    cls := userClass(userId, cssClass, withOnline),
    href := userUrl(username, params = params)
  )(
      withOnline ?? headWithLine(head),
      titleTag(title),
      span(cls := "u_name")(truncate.fold(username)(username.take)),
      withBadge option badge(isPatron, modIcon, silver, gold, coach, team)
    )

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withPowerTip: Boolean = true,
    withTitle: Boolean = true,
    withBestRating: Boolean = false,
    withPerfRating: Option[PerfType] = None,
    text: Option[String] = None,
    params: String = "",
    withBadge: Boolean = true
  ): Frag = a(
    cls := userClass(user.id, cssClass, withOnline, withPowerTip),
    href := userUrl(user.username, params)
  )(
      withOnline ?? headIcon(user),
      withTitle option titleTag(user.title),
      span(cls := "u_name")(text | user.username),
      userRating(user, withPerfRating, withBestRating),
      withBadge option badge(user.isPatron, silver = user.isSilver, gold = user.isGold, coach = user.isCoach, team = user.isTeam)
    )

  def userSpan(
    user: User,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withPowerTip: Boolean = true,
    withTitle: Boolean = true,
    withBestRating: Boolean = false,
    withPerfRating: Option[PerfType] = None,
    text: Option[String] = None
  ): Frag = span(
    cls := userClass(user.id, cssClass, withOnline, withPowerTip),
    dataHref := userUrl(user.username)
  )(
      withOnline ?? headIcon(user),
      withTitle option titleTag(user.title),
      span(cls := "u_name")(text | user.username),
      userRating(user, withPerfRating, withBestRating),
      badge(user.isPatron, silver = user.isSilver, gold = user.isGold, coach = user.isCoach, team = user.isTeam)
    )

  def userIdSpanMini(userId: String, withOnline: Boolean = false): Frag = {
    val user = lightUser(userId)
    val name = user.fold(userId)(_.name)
    span(
      cls := userClass(userId, none, withOnline),
      dataHref := userUrl(name)
    )(
        withOnline ?? headWithLine(user ?? (_.head)),
        user.??(u => titleTag(u.title map Title.apply)),
        span(cls := "u_name")(name),
        user.??(u => badge(u.isPatron, silver = u.isSilver, gold = u.isGold, coach = u.isCoach, team = u.isTeam))
      )
  }

  private def renderRating(perf: Perf): Frag = frag(
    " (",
    perf.intRating,
    perf.provisional option "?",
    ")"
  )

  private def userRating(user: User, withPerfRating: Option[PerfType], withBestRating: Boolean): Frag =
    withPerfRating match {
      case Some(perfType) => renderRating(user.perfs(perfType))
      case _ if withBestRating => user.perfs.bestPerf ?? {
        case (_, perf) => renderRating(perf)
      }
      case _ => ""
    }

  private def userUrl(username: String, params: String = "") =
    s"""${routes.User.show(username)}$params"""

  protected def userClass(
    userId: String,
    cssClass: Option[String],
    withOnline: Boolean,
    withPowerTip: Boolean = true
  ): List[(String, Boolean)] =
    (withOnline ?? List((if (isOnline(userId)) "online" else "offline") -> true)) ::: List(
      "user-link" -> true,
      ~cssClass -> cssClass.isDefined,
      "ulpt" -> withPowerTip
    )

  def userGameFilterTitle(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(implicit ctx: UserContext): Frag =
    if (filter == GameFilter.Search) frag(trans.advancedSearch())
    else splitNumber(userGameFilterTitleNoTag(u, nbs, filter))

  def userGameFilterTitleNoTag(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(implicit ctx: UserContext): String = (filter match {
    case GameFilter.All => trans.nbGames.pluralSameTxt(u.count.game)
    case GameFilter.Me => nbs.withMe ?? trans.nbGamesWithYou.pluralSameTxt
    case GameFilter.Rated => trans.nbRated.pluralSameTxt(u.count.rated)
    case GameFilter.Win => trans.nbWins.pluralSameTxt(u.count.win)
    case GameFilter.Loss => trans.nbLosses.pluralSameTxt(u.count.loss)
    case GameFilter.Draw => trans.nbDraws.pluralSameTxt(u.count.draw)
    case GameFilter.Playing => trans.nbPlaying.pluralSameTxt(nbs.playing)
    case GameFilter.Bookmark => trans.nbBookmarks.pluralSameTxt(nbs.bookmark)
    case GameFilter.Imported => trans.nbImportedGames.pluralSameTxt(nbs.imported)
    case GameFilter.Search => trans.advancedSearch.txt()
  })

  def describeUser(user: User) = {
    val name = user.titleUsername
    val nbGames = user.count.game
    val createdAt = org.joda.time.format.DateTimeFormat forStyle "M-" print user.createdAt
    val currentRating = user.perfs.bestPerf ?? {
      case (pt, perf) => s" ${pt.name} 当前积分：${perf.intRating}."
    }
    s"$name 已进行 $nbGames 场对局，从 $createdAt.$currentRating"
  }

  val baseUrl = s"//${lila.api.Env.current.Net.AssetDomain}"

  val patronIconChar = ""
  val lineIconChar = ""
  val patronIcon: Frag = i(cls := "patron", title := "Haichess Patron")
  val moderatorIcon: Frag = i(cls := "moderator", title := "Haichess 管理员")
  val generalIcon: Frag = img(cls := "badge", src := s"$baseUrl/assets/images/icons/general.svg", title := "注册会员")
  val silverIcon: Frag = img(cls := "badge", src := s"$baseUrl/assets/images/icons/silver.svg", title := "银牌会员")
  val goldIcon: Frag = img(cls := "badge", src := s"$baseUrl/assets/images/icons/gold.svg", title := "金牌会员")
  val coachIcon: Frag = img(cls := "badge", src := s"$baseUrl/assets/images/icons/coach.svg", title := "认证教练")
  val teamIcon: Frag = img(cls := "badge", src := s"$baseUrl/assets/images/icons/team.svg", title := "认证俱乐部")

  val defaultHead: String = s"$baseUrl/assets/images/head-default-64.png"
  def headUrl(path: Option[String]) = path.fold(defaultHead) { p =>
    s"$baseUrl/image/$p"
  }

  def headWithLine(head: Option[String]): Frag = div(cls := "head-line")(
    img(cls := "head", src := headUrl(head)),
    i(cls := "line")
  )

  def headIcon(user: LightUser): Frag = headWithLine(user.head)
  def headIcon(user: User): Frag = headWithLine(user.head)
  def badge(
    patron: Boolean = false,
    moderator: Boolean = false,
    silver: Boolean = false,
    gold: Boolean = false,
    coach: Boolean = false,
    team: Boolean = false
  ): Frag = span(cls := "badges")(
    // patron option patronIcon,
    // moderator option moderatorIcon,
    (silver && !gold) option silverIcon,
    gold option goldIcon,
    coach option coachIcon,
    team option teamIcon
  )
}
