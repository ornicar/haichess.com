package views.html.user.show

import play.api.data.Form
import lila.api.Context
import lila.app.mashup.UserInfo.Angle
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.user.{ FormSelect, User }
import controllers.routes

object header {

  private val dataToints = attr("data-toints")
  private val dataTab = attr("data-tab")

  def apply(
    u: User,
    info: lila.app.mashup.UserInfo,
    angle: lila.app.mashup.UserInfo.Angle,
    social: lila.app.mashup.UserInfo.Social
  )(implicit ctx: Context) = frag(
    div(cls := "box__top user-show__header")(
      userSpan(u, cssClass = "large".some, withPowerTip = false),
      div(cls := List(
        "trophies" -> true,
        "packed" -> (info.countTrophiesAndPerfCups > 7)
      ))(
        views.html.user.bits.perfTrophies(u, info.ranks),
        otherTrophies(u, info),
        u.plan.active option
          a(href := routes.Plan.index, cls := "trophy award patron icon3d", ariaTitle(s"Patron since ${showDate(u.plan.sinceDate)}"))(patronIconChar)
      ),
      u.disabled option span(cls := "closed")("已关闭")
    ),
    div(cls := "user-show__social")(
      div(cls := "number-menu")(
        a(cls := "nm-item", href := routes.Relation.followers(u.username))(
          splitNumber(trans.nbFollowers.pluralSame(info.nbFollowers))
        ),
        info.nbBlockers.map { nb =>
          a(cls := "nm-item")(nb + " 个黑名单")
        },
        u.noBot option a(
          href := routes.UserTournament.path(u.username, "recent"),
          cls := "nm-item tournament_stats",
          dataToints := u.toints
        )(
            splitNumber(trans.nbTournamentPoints.pluralSame(u.toints))
          ),
        a(href := routes.Study.byOwnerDefault(u.username), cls := "nm-item")(
          info.nbStudies + " 研习"
        ),
        /*        a(
          cls := "nm-item",
          href := ctx.noKid option routes.ForumPost.search("user:" + u.username, 1).url
        )(
            splitNumber(trans.nbForumPosts.pluralSame(info.nbPosts))
          ),*/
        (ctx.isAuth && ctx.noKid && !ctx.is(u)) option
          a(cls := "nm-item note-zone-toggle")(
            social.notes.size + " 备注"
          )
      ),
      div(cls := "user-actions btn-rack")(
        (ctx is u) option frag(
          a(cls := "btn-rack__btn", href := routes.Account.profile(None), titleOrText(trans.editProfile.txt()), dataIcon := "%"),
          a(cls := "btn-rack__btn", href := routes.Relation.blocks(), titleOrText(trans.listBlockedPlayers.txt()), dataIcon := "k")
        ),
        isGranted(_.UserSpy) option
          a(cls := "btn-rack__btn mod-zone-toggle", href := routes.User.mod(u.username), titleOrText("Mod zone"), dataIcon := ""),
        a(cls := "btn-rack__btn", href := routes.User.tv(u.username), titleOrText(trans.watchGames.txt()), dataIcon := "1"),
        (ctx.isAuth && !ctx.is(u)) option
          views.html.relation.actions(u.id, relation = social.relation, followable = social.followable, blocked = social.blocked),
        if (ctx is u) a(
          cls := "btn-rack__btn",
          href := routes.Game.exportByUser(u.username),
          titleOrText(trans.exportGames.txt()),
          dataIcon := "x"
        )
        else (ctx.isAuth && ctx.noKid) option a(
          titleOrText(trans.reportXToModerators.txt(u.username)),
          cls := "btn-rack__btn",
          href := s"${routes.Report.form}?username=${u.username}",
          dataIcon := "!"
        )
      )
    ),
    (ctx.noKid && !ctx.is(u)) option div(cls := "note-zone")(
      postForm(action := s"${routes.User.writeNote(u.username)}?note")(
        textarea(name := "text", placeholder := "录入一个只有您和好友可见的备注"),
        submitButton(cls := "button")(trans.send()),
        if (isGranted(_.ModNote)) label(style := "margin-left: 1em;")(
          input(tpe := "checkbox", name := "mod", checked, value := "true", style := "vertical-align: middle;"),
          "仅管理员可见"
        )
        else input(tpe := "hidden", name := "mod", value := "false")
      ),
      social.notes.isEmpty option div("暂时无备注"),
      social.notes.map { note =>
        div(cls := "note")(
          p(cls := "note__text")(richText(note.text)),
          p(cls := "note__meta")(
            userIdLink(note.from.some),
            br,
            momentFromNow(note.date),
            (ctx.me.exists(note.isFrom) && !note.mod) option frag(
              br,
              postForm(action := routes.User.deleteNote(note._id))(
                submitButton(cls := "button-empty button-red confirm button text", style := "float:right", dataIcon := "q")("Delete")
              )
            )
          )
        )
      }
    ),
    ((ctx is u) && u.perfs.bestStandardRating > 2400 && !u.hasTitle && !ctx.pref.hasSeenVerifyTitle) option claimTitle(u),
    isGranted(_.UserSpy) option div(cls := "mod-zone none"),
    angle match {
      case Angle.Games(Some(searchForm)) => views.html.search.user(u, searchForm, routes.User.games(u.username, "search"))
      case _ =>
        val profile = u.profileOrDefault
        div(id := "us_profile")(
          info.ratingChart.ifTrue(!u.lame || ctx.is(u) || isGranted(_.UserSpy)).map { ratingChart =>
            div(cls := "rating-charts")(
              div(cls := "search-bar")(
                span(dataType := "month", dataCount := "1")("1月内"),
                span(cls := "active", dataType := "month", dataCount := "3")("3月内"),
                span(dataType := "month", dataCount := "6")("6月内"),
                span(dataType := "ytd")("今年"),
                span(dataType := "year", dataCount := "1")("1年内"),
                span(dataType := "all")("所有")
              ),
              div(cls := "rating-history")(spinner)
            )
          } getOrElse {
            ctx.is(u) option newPlayer(u)
          },
          div(cls := "profile-side")(
            div(cls := "user-infos")(
              !ctx.is(u) option frag(
                u.engine option div(cls := "warning engine_warning")(
                  span(dataIcon := "j", cls := "is4"),
                  trans.thisPlayerUsesChessComputerAssistance()
                ),
                (u.booster && (u.count.game > 0 || isGranted(_.Hunter))) option div(cls := "warning engine_warning")(
                  span(dataIcon := "j", cls := "is4"),
                  trans.thisPlayerArtificiallyIncreasesTheirRating(),
                  (u.count.game == 0) option """
Only visible to mods. A booster mark without any games is a way to
prevent a player from ever playing (except against boosters/cheaters).
It's useful against spambots. These marks are not visible to the public."""
                )
              ),
              ctx.noKid option frag(
                if (isGranted(_.Admin) || ctx.is(u) || ctx.isMyClassmateOrCoach(u)) {
                  profile.nonEmptyRealName.map { name =>
                    strong(cls := "name") {
                      profile.sex.fold(name) { sex =>
                        name + " (" + FormSelect.Sex.name(sex) + ")"
                      }
                    }
                  }
                } else {
                  strong(cls := "name") {
                    profile.sex.fold(u.username) { sex =>
                      u.username + " (" + FormSelect.Sex.name(sex) + ")"
                    }
                  }
                },
                profile.province.map { _ =>
                  p(cls := "location")(profile.location)
                },
                profile.nonEmptyBio.ifTrue(!u.troll || ctx.is(u)).map { bio =>
                  p(cls := "bio")(richText(shorten(bio, 400), nl2br = false))
                }
              ),
              div(cls := "stats")(
                /*                profile.officialRating.map { r =>
                  div(r.name.toUpperCase, " rating: ", strong(r.rating))
                },*/

                /*                profile.nonEmptyLocation.ifTrue(ctx.noKid).map { l =>
                  span(cls := "location")(l)
                },
                profile.countryInfo.map { c =>
                  span(cls := "country")(
                    img(cls := "flag", src := staticUrl(s"images/flags/${c.code}.png")),
                    " ",
                    c.name
                  )
                },*/
                p(cls := "thin")(trans.memberSince(), " ", showDate(u.createdAt)),
                u.seenAt.map { seen =>
                  p(cls := "thin")(trans.lastSeenActive(momentFromNow(seen)))
                },
                info.completionRatePercent.map { c =>
                  p(cls := "thin")(trans.gameCompletionRate(s"$c%"))
                },
                (ctx is u) option frag(
                  a(href := routes.Account.profile(None), title := trans.editProfile.txt())(
                    trans.profileCompletion(s"${profile.completionPercent}%")
                  ),
                  br,
                  a(href := routes.User.opponents)(trans.favoriteOpponents())
                ),
                info.playTime.map { playTime =>
                  frag(
                    p(trans.tpTimeSpentPlaying(showPeriod(playTime.totalPeriod))),
                    playTime.nonEmptyTvPeriod.map { tvPeriod =>
                      p(trans.tpTimeSpentOnTV(showPeriod(tvPeriod)))
                    }
                  )
                },
                div(cls := "social_links col2")(
                  profile.actualLinks.map { link =>
                    a(href := link.url, target := "_blank", rel := "nofollow")(link.site.name)
                  }
                ) /*,
                div(cls := "teams col2")(
                  info.teamIds.sorted.map { t =>
                    teamLink(t, withIcon = false)
                  }
                )*/
              )
            ),
            (info.insightVisible) option
              a(cls := "insight", href := routes.Insight.index(u.username), dataIcon := "7")(
                span(
                  strong("数据洞察"),
                  em("分析 ", if (ctx.is(u)) "您" else s"${u.username}", "的对局")
                )
              )
          )
        )
    },
    div(cls := "angles number-menu number-menu--tabs menu-box-pop")(
      a(
        dataTab := "activity",
        cls := List(
          "nm-item to-activity" -> true,
          "active" -> (angle == Angle.Activity)
        ),
        href := routes.User.show(u.username)
      )(trans.activity.activity()),
      a(
        dataTab := "games",
        cls := List(
          "nm-item to-games" -> true,
          "active" -> (angle.key == "games")
        ),
        href := routes.User.gamesAll(u.username)
      )(
          trans.nbGames.plural(info.user.count.game, info.user.count.game.localize),
          info.nbs.playing > 0 option
            span(cls := "unread", title := trans.nbPlaying.pluralTxt(info.nbs.playing, info.nbs.playing.localize))(
              info.nbs.playing
            )
        )
    )
  )
}
