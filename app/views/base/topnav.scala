package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object topnav {

  private def linkTitle(url: String, name: Frag)(implicit ctx: Context) =
    if (ctx.blind) h3(name) else a(href := url)(name)

  def apply()(implicit ctx: Context) = st.nav(id := "topnav", cls := "hover")(
    st.section(
      linkTitle(routes.Home.home.toString, frag(
        span(cls := "play")(trans.play()),
        span(cls := "home")("haichess.com")
      )),
      div(role := "group")(
        /*        if (ctx.noBot) a(href := "/lobby?any#hook")(trans.createAGame())
        else a(href := "/lobby?any#friend")(trans.playWithAFriend()),*/
        if (ctx.noBot) a(href := routes.Lobby.home)(trans.createAGame())
        else a(href := routes.Lobby.home)(trans.playWithAFriend()),
        ctx.noBot option frag(
          a(href := routes.Tournament.home())(trans.tournaments()),
          ctx.isAuth option a(href := routes.Contest.home)("比赛"),
          a(href := routes.Simul.home)(trans.simultaneousExhibitions())
        ),
        a(href := routes.Tv.games)("当前对局")
      )
    ),
    st.section(
      linkTitle(routes.Puzzle.home.toString, trans.learnMenu()),
      div(role := "group")(
        ctx.noBot option frag(
          a(href := routes.Puzzle.home)(trans.training()),
          a(href := routes.Puzzle.themePuzzleHome)("主题战术"),
          a(href := routes.PuzzleRush.show)("战术冲刺"),
          a(href := routes.Practice.index)("练习")
        ),
        a(href := routes.Study.allDefault(1))("研习"),
        ctx.isAuth option a(href := routes.Recall.home())("记谱"),
        /*,
        a(href := routes.Coach.allDefault(1))(trans.coaches())*/
        ctx.noBot option frag(
          a(href := routes.Learn.index)(trans.chessBasics()),
          a(href := routes.Coordinate.home)(trans.coordinates.coordinates())
        )
      )
    ),
    /*    st.section(
      linkTitle(routes.Tv.index.toString, trans.watch()),
      div(role := "group")(
        a(href := routes.Tv.index)("嗨棋 TV"),
        a(href := routes.Tv.games)(trans.currentGames()),
        a(href := routes.Streamer.index())("主播"),
        a(href := routes.Relay.index())("广播"),
        ctx.noBot option a(href := routes.Video.index)(trans.videoLibrary())
      )
    ),
    st.section(
      linkTitle(routes.User.list.toString, trans.community()),
      div(role := "group")(
        a(href := routes.User.list)(trans.players()),
        NotForKids(frag(
          a(href := routes.Team.home())(trans.teams()),
          a(href := routes.ForumCateg.index)(trans.forum())
        ))
      )
    ),*/
    st.section(
      linkTitle(routes.UserAnalysis.index.toString, trans.tools()),
      div(role := "group")(
        a(href := routes.UserAnalysis.index)(trans.analysis()),
        a(href := s"${routes.UserAnalysis.index}#explorer")(trans.openingExplorer()),
        a(href := routes.Editor.index)(trans.boardEditor()),
        a(href := routes.Search.index())(trans.advancedSearch()),
        ctx.me.map { me =>
          isGranted(_.Coach) || me.hasTeam option a(href := routes.OffContest.home())("比赛编排")
        }
      )
    ),
    st.section(
      linkTitle(routes.Importer.importGame.toString, "资源"),
      div(role := "group")(
        a(href := routes.Importer.importGame)("导入"),
        a(href := routes.Resource.gameImported())("对局"),
        a(href := routes.Resource.puzzleImported())("战术题")
      )
    ),
    ctx.me.map { me =>
      st.section(
        linkTitle(routes.User.gamesAll(me.username).toString, "我的"),
        div(role := "group")(
          a(href := routes.User.gamesAll(me.username))("动态"),
          a(href := routes.Relation.following(me.username))("好友"),
          !ctx.kid option a(href := routes.Message.inbox())("信箱"),
          a(href := routes.Clazz.current())("班级"),
          NotForKids(
            frag(
              a(href := routes.Team.mine())("俱乐部"),
              a(href := routes.ForumCateg.index)("讨论区")
            )
          ),
          a(href := routes.Errors.puzzle(1))("错题库"),
          a(href := routes.Insight.index(me.username))("数据洞察")
        )
      )
    },
    ctx.me.map { me =>
      isGranted(_.Coach) option st.section(
        linkTitle(routes.Coach.showById(me.id).toString, "教练"),
        div(role := "group")(
          a(href := routes.Coach.showById(me.id))("个人主页"),
          a(href := routes.Clazz.current())("班级"),
          a(href := routes.Coach.approvedStuList())("学员"),
          a(href := routes.Course.timetable(0))("课程表"),
          isGranted(_.Coach) option a(href := routes.MemberCard.page(1, none, none))("会员卡")
        )
      )
    },
    ctx.me.map { me =>
      me.hasTeam option st.section(
        linkTitle(routes.Team.show(me.teamIdValue).toString, "俱乐部"),
        div(role := "group")(
          NotForKids(frag(
            a(href := routes.Team.members(me.teamIdValue, 1))("成员"),
            a(href := teamForumUrl(me.teamIdValue))("讨论区"),
            a(href := routes.Team.edit(me.teamIdValue))("资料"),
            a(href := routes.TeamCertification.certification(me.teamIdValue))("认证"),
            a(href := routes.Team.setting(me.teamIdValue))("设置"),
            isGranted(_.Team) option a(href := routes.MemberCard.page(1))("会员卡")
          ))
        )
      )
    }
  )
}
