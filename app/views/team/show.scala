package views.html.team

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.richText
import lila.team.{ Member, Team }
import controllers.routes

object show {

  def apply(t: Team, member: Option[Member], members: Paginator[lila.team.MemberWithUser], info: lila.app.mashup.TeamInfo, error: Option[String] = None)(implicit ctx: Context) =
    bits.layout(
      title = t.name,
      evenMoreJs = frag(
        unsliderTag,
        flatpickrTag,
        jsTag("team.show.js")
      ),
      openGraph = lila.app.ui.OpenGraph(
        title = s"${t.name} team",
        url = s"$netBaseUrl${routes.Team.show(t.id).url}",
        description = shorten(t.description, 152)
      ).some
    )(
        main(cls := "page-menu")(
          bits.menu(none),
          div(cls := "team-show page-menu__content box team-show")(
            div(cls := "box__top")(
              div(cls := "subject")(
                teamLink(t, cssClass = "large".some)
              ),
              div(
                if (t.disabled) span(cls := "closed")("已关闭")
                else trans.nbMembers.plural(t.nbMembers, strong(t.nbMembers.localize))
              )
            ),
            (info.mine || t.enabled) option div(cls := "team-show__content")(
              st.section(cls := "team-show__meta")(
                div("编号", "：", strong(t.id)),
                div("地址", "：", strong(cls := "location")(t.location)),
                div(trans.teamLeader(), "：", userIdLink(t.createdBy.some, withBadge = false)),
                member.map { m =>
                  val link = if (ctx.userId.??(t.isCreator)) routes.Team.ratingDistribution(t.id) else routes.Team.memberRatingDistribution(m.id)
                  t.ratingSettingOrDefault.open option div("等级分：", a(href := link)(strong(m.intRating.map(_.toString) | "暂无")))
                }
              ),
              div(cls := "team-show__members")(
                !info.bestUserIds.isEmpty option st.section(cls := "best-members")(
                  h2(trans.teamBestPlayers()),
                  ol(cls := "userlist best_players")(
                    info.bestUserIds.map { userId =>
                      li(userIdLink(userId.some))
                    }
                  )
                ),
                (info.mine && !info.coachIds.isEmpty) option st.section(cls := "coach-members")(
                  h2("教练"),
                  ol(cls := "userlist")(
                    info.coachIds.map { userId =>
                      li(a(cls := "user-link", href := routes.Coach.showById(userId))(userId))
                    }
                  )
                ),
                st.section(cls := "recent-members")(
                  h2(trans.teamRecentMembers()),
                  div(cls := "userlist infinitescroll")(
                    pagerNext(members, np => routes.Team.show(t.id, np).url),
                    members.currentPageResults.map { member =>
                      div(cls := "paginated")(userLink(member.user))
                    }
                  )
                )
              ),
              st.section(cls := "team-show__desc")(
                div(cls := "description")(richText(t.description)),
                t.envPicture.isDefined option div(cls := "banner")(
                  ul(cls := "items")(
                    t.envPictureOrDefault.map { image =>
                      li(img(src := dbImageUrl(image)))
                    }
                  )
                ),
                /*
                info.createdByMe && t.certified option div(cls := "invites")(
                  h2("邀请教练"),
                  inviteList(info.invites, t, error)
                ),
                */
                info.hasRequests option div(cls := "requests")(
                  h2(info.requests.size, "加入请求"),
                  views.html.team.request.list(info.requests, t.some)
                )
              ),
              st.section(cls := "team-show__actions")(
                div(cls := "join")(
                  (t.enabled && !info.mine && !info.invite.isDefined) option frag(
                    if (info.requestedByMe) strong("您的加入请求正在由俱乐部管理员审核")
                    else ctx.me.??(_.canTeam) option joinButton(t)
                  ),
                  info.invite map { ivt =>
                    postForm(cls := "process-invite", action := routes.Team.inviteProcess(ivt.id))(
                      p(strong(ivt.message)),
                      button(name := "process", cls := "button button-green", value := "accept")(trans.accept()),
                      button(name := "process", cls := "button button-empty button-red", value := "decline")(trans.decline())
                    )
                  },
                  (info.mine && !info.createdByMe) option
                    postForm(cls := "quit", action := routes.Team.quit(t.id))(
                      submitButton(cls := "button button-empty button-red confirm")(trans.quitTeam.txt())
                    )
                ),
                div(cls := "actions")(
                  (info.createdByMe || isGranted(_.Admin)) option frag(
                    div(cls := "creator-action")(
                      a(href := routes.Team.setting(t.id), cls := "button button-empty text", dataIcon := "%")(trans.settings()),
                      a(href := routes.Team.edit(t.id), cls := "button button-empty text", dataIcon := "m")("资料")
                    ),
                    div(cls := "creator-action")(
                      t.enabled option a(href := routes.TeamCertification.certification(t.id), cls := "button button-empty text", dataIcon := "证")("认证"),
                      a(href := routes.Team.members(t.id, 1), cls := "button button-empty text", dataIcon := "r")("成员")
                    ),
                    t.enabled option div(cls := "creator-action")(
                      a(href := s"${routes.Tournament.form()}?team=${t.id}", cls := "button button-empty text", dataIcon := "g")("锦标赛"),
                      a(href := s"${routes.Contest.createForm()}?team=${t.id}", cls := "button button-empty text", dataIcon := "赛")("比赛")
                    )
                  )
                )
              ),
              info.featuredTours.nonEmpty option frag(
                st.section(cls := "team-show__tour team-tournaments")(
                  h2(dataIcon := "赛", cls := "text")("锦标赛/比赛"),
                  tour.widget(info.featuredTours)
                )
              ),
              info.mine option st.section(cls := "team-show__forum")(
                h2(dataIcon := "d", cls := "text")(
                  a(href := teamForumUrl(t.id))(trans.forum()),
                  " (", info.forumNbPosts, ")"
                ),
                info.forumPosts.take(10).map { post =>
                  st.article(
                    p(cls := "meta")(
                      a(href := routes.ForumPost.redirect(post.postId))(post.topicName),
                      em(
                        userIdLink(post.userId, withOnline = false),
                        " ",
                        momentFromNow(post.createdAt)
                      )
                    ),
                    p(shorten(post.text, 200))
                  )
                },
                a(cls := "more", href := teamForumUrl(t.id))(t.name, trans.forum(), "»")
              )
            )
          )
        )
      )

  private def inviteList(invites: List[lila.team.InviteWithUser], t: lila.team.Team, error: Option[String] = None)(implicit ctx: Context) = frag(
    postForm(action := routes.Team.invite(t.id))(
      div(cls := "user-invite")(
        label(`for` := "username")("搜索："),
        input(cls := "user-autocomplete", id := "username", name := "username",
          placeholder := "用户名", autofocus, required, dataTag := "span"),
        submitButton(cls := "button confirm", dataIcon := "E")
      ),
      error.map {
        badTag(_)
      }
    ),
    table(cls := "slist")(
      thead(
        tr(
          th("教练"),
          th("邀请时间"),
          th("邀请消息")
        )
      ),
      tbody(
        invites.map { invite =>
          tr(
            td(userLink(invite.user)),
            td(momentFromNow(invite.date)),
            td(richText(invite.message))
          )
        }
      )
    )
  )

  // handle special teams here
  private def joinButton(t: Team)(implicit ctx: Context) = t.id match {
    case "english-chess-players" => joinAt("https://ecf.chessvariants.training/")
    case "ecf" => joinAt(routes.Team.show("english-chess-players").url)
    case _ => postForm(cls := "inline", action := routes.Team.join(t.id))(
      submitButton(cls := "button button-green")(trans.joinTeam())
    )
  }

  private def joinAt(url: String)(implicit ctx: Context) =
    a(cls := "button button-green", href := url)(trans.joinTeam())
}
