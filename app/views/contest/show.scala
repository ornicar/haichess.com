package views.html.contest

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.contest.{ Board, Contest, Invite, InviteWithUser, Player, PlayerWithUser, Request, RequestWithUser, Round, ScoreSheet, Forbidden }
import controllers.routes

object show {

  private val dataTab = attr("data-tab")
  def apply(
    c: Contest,
    rounds: List[Round],
    players: List[PlayerWithUser],
    boards: List[Board],
    requests: List[RequestWithUser],
    invites: List[InviteWithUser],
    forbiddens: List[Forbidden],
    scoreSheets: List[ScoreSheet],
    myRequest: Option[Request],
    myInvite: Option[Invite]
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"比赛 ${c.name}",
    moreCss = cssTag("contest.show"),
    moreJs = frag(
      tableDnDTag,
      cookieTag,
      flatpickrTag,
      jsTag("contest.show.js")
    )
  ) {
      main(cls := "page-small contest-show", dataId := c.id)(
        div(cls := "box box-pad head")(
          div(cls := "head__info")(
            baseInfo(c)
          ),
          div(cls := "head__board")(
            (chess.StartingPosition.initial.fen != c.position.fen) option
              (chess.format.Forsyth << c.position.fen).map { situation =>
                span(
                  cls := s"mini-board cg-wrap parse-fen is2d ${c.variant.key}",
                  dataColor := situation.color.name,
                  dataFen := c.position.fen
                )(cgWrapContent)
              }
          ),
          div(cls := "head__enter")(
            findStartBoard(boards).filter(b => !b.appt || (b.appt && b.apptComplete)).map { b =>
              val color = ctx.me.fold("white")(u => b.colorOfById(u.id).name)
              a(cls := "button glowing enter", href := routes.Round.watcher(b.id, color))("比赛已经开始，点击进入")
            } getOrElse {
              val isDisabled = enterButtonStatus(c, myRequest, myInvite)
              val md = myInvite.fold("GET")(_ => "POST")
              val ac = myInvite.fold(routes.Contest.joinForm(c.id))(iv => routes.Contest.inviteProcess(iv.id))
              st.form(st.action := ac.toString, method := md)(
                submitButton(cls := List("button button-green enter" -> true, "disabled" -> !isDisabled), !isDisabled option disabled)(enterButtonText(c, myRequest, myInvite, players))
              )
            }
          ),
          isCreator(c) option div(cls := "head__action")(
            c.isCreated option frag(
              postForm(st.action := routes.Contest.publish(c.id))(
                submitButton(
                  cls := List("button button-green publish confirm" -> true, "disabled" -> c.shouldEnterStop), c.shouldEnterStop option disabled,
                  title := (if (c.shouldEnterStop) "已经过了报名截止时间" else "是否确认发布？")
                )("发布")
              ),
              a(cls := "button button-empty update", href := routes.Contest.updateForm(c.id), title := "编辑比赛")("编辑"),
              postForm(st.action := routes.Contest.remove(c.id))(
                submitButton(cls := "button button-red button-empty remove confirm", title := "删除比赛将不可恢复，是否确认？")("删除")
              )
            ),
            findPlayer(players).map { pwu =>
              if (belongTo(pwu)) {
                if (!pwu.player.absentOr && c.quitable) {
                  postForm(st.action := routes.Contest.quit(c.id))(
                    submitButton(cls := "button button-red button-empty quit confirm", title := "是否确认退出比赛？")("退赛")
                  )
                } else frag()
              } else frag()
            },
            c.inviteable option a(cls := "button button-empty modal-alert invite", href := routes.Contest.inviteForm(c.id), title := "邀请")("邀请"),
            (c.isPublished || c.isEnterStopped || c.isStarted) option postForm(st.action := routes.Contest.autoPairing(c.id))(
              /*              c.autoPairing option submitButton(name := "autoPairing", value := "0", cls := "button button-empty auto confirm", title := "取消自动编排和发布成绩？")("取消自动"),
              !c.autoPairing option submitButton(name := "autoPairing", value := "1", cls := "button button-empty cancelAuto confirm", title := "自动编排和发布成绩？")("自动")*/

              span(cls := "form-check-input", title := (if (c.autoPairing) "当前为自动模式，点击切换为手动模式" else "当前为手动模式，点击切换为自动模式"))(
                st.input(
                  st.id := "autoPairing",
                  tpe := "checkbox",
                  cls := "form-control cmn-toggle",
                  c.autoPairing option checked
                ),
                label(`for` := "autoPairing")
              )
            ),
            (c.isOverPublished && !c.isFinishedOrCanceled) option postForm(st.action := routes.Contest.cancel(c.id))(
              submitButton(cls := "button button-red button-empty cancel confirm", title := "是否确认取消比赛？")("取消")
            )
          ),
          findPlayer(players).map { pwu =>
            if (belongTo(pwu) && !isCreator(c)) {
              div(cls := "head__action")(
                if (!pwu.player.absentOr && c.quitable) {
                  postForm(st.action := routes.Contest.quit(c.id))(
                    submitButton(cls := "button button-red button-empty quit confirm", title := "是否确认退出比赛？")("退赛")
                  )
                } else frag()
              )
            } else frag()
          }
        ),
        div(cls := "box box-pad flow")(
          div(cls := "tabs")(
            div(dataTab := "rule", cls := List("active running" -> isRuleTabActive(c)))("竞赛规则"),
            isCreator(c) option div(dataTab := "enter", cls := List("active running" -> isEnterTabActive(c)))("报名管理"),
            isCreator(c) option div(dataTab := "forbidden", cls := List("disabled" -> isForbiddenTabDisabled(c)))("回避设置"),
            c.roundList.map { rno =>
              val rd = findRound(rno, rounds)
              div(dataTab := s"round$rno", cls := List("active running" -> isRoundTabActive(c, rno, rounds), "disabled" -> isRoundTabDisabled(c, rno, rounds)))(
                div(s"第${rno}轮"),
                div(rd.actualStartsAt.toString("MM-dd HH:mm"))
              )
            },
            div(dataTab := "score", cls := List("active running" -> isScoreTabActive(c), "disabled" -> isScoreTabDisabled(c)))("成绩册")
          ),
          div(cls := "panels")(
            div(cls := List("panel rule" -> true, "active" -> isRuleTabActive(c)))(rule(c, rounds)),
            isCreator(c) option div(cls := List("panel enter" -> true, "active" -> isEnterTabActive(c)))(
              enter(c, rounds, players, requests, invites)
            ),
            isCreator(c) option div(cls := List("panel forbidden" -> true))(forbiddenTab(c, forbiddens, players)),
            c.roundList.map { rno =>
              div(cls := List(s"panel round round$rno" -> true, "active" -> isRoundTabActive(c, rno, rounds)))(
                round(c, rounds, players, boards, rno)
              )
            },
            div(cls := List("panel score" -> true, "active" -> isScoreTabActive(c)))(
              score(c, rounds, players, scoreSheets)
            )
          )
        )
      )
    }

  private def baseInfo(c: Contest)(implicit ctx: Context) =
    table(
      tr(
        td(
          img(cls := "logo", src := c.logo.fold(staticUrl("images/contest.svg")) { l => dbImageUrl(l) })
        ),
        td(
          div(cls := "contest-name")(c.fullName, nbsp, isCreator(c) option a(cls := "clone", href := routes.Contest.clone(c.id), title := "复制")("复制")),
          div(cls := "organizer")("主办方：", c.typ match {
            case Contest.Type.Public | Contest.Type.TeamInner => teamLinkById(c.organizer, false)
            case Contest.Type.ClazzInner => clazzLinkById(c.organizer)
          })
        )
      ),
      tr(
        td(c.status.name),
        td(c.typ.name)
      ),
      tr(
        td,
        td(c.rule.name, nbsp, c.rounds, "轮")
      ),
      tr(
        td,
        td(c.variant.name, nbsp, c.clock.toString)
      ),
      tr(
        td,
        td("比赛时间：", c.startsAt.toString("yyyy-MM-dd HH:mm"), " 至 ", c.finishAt.toString("yyyy-MM-dd HH:mm"))
      ),
      c.appt option tr(
        td,
        td("自由约棋：", "是")
      ),
      tr(
        td,
        td("报名截止：", c.deadlineAt.toString("yyyy-MM-dd HH:mm"))
      ),
      tr(
        td,
        td("报名人数：", c.nbPlayers, " / ", c.maxPlayers)
      ),
      c.attachments.map { path =>
        tr(
          td,
          td("比赛规程：", attachments(path))
        )
      },
      tr(
        td,
        td("　报名费：", c.enterCost)
      )
    )

  private def rule(c: Contest, rounds: List[Round])(implicit ctx: Context) = frag(
    div(cls := "s1")(
      div(cls := "h1")("一、竞赛通用规则"),
      div(cls := "s2")(
        div(cls := "h2")("1、公平竞赛规则"),
        ul(
          li("1.1 棋手必须进行公平的竞赛，禁止使用电脑引擎分析，或由其他人进行辅助"),
          li("1.2 裁判：主办方有权对比赛任何一盘棋做反作弊调查与裁决"),
          li("1.3 仲裁：棋手对任何一盘棋的结果或裁决有异议，可联系主办方进行仲裁")
        )
      ),
      div(cls := "s2")(
        div(cls := "h2")("2、文明竞赛规则"),
        ul(
          li("2.1 棋手应按时参加比赛"),
          li("2.2 积极争取好的比赛结果，不故意输棋或故意拖延走棋时间"),
          li("2.3 比赛中，禁止出现不文明、不道德的语言或行为")
        )
      ),
      div(cls := "s2")(
        div(cls := "h2")("3、比赛受到不可抗拒因素或者国家政策影响，主办方有权修改比赛时间调整赛事进程")
      )
    ),
    div(cls := "s1")(
      div(cls := "h1")(s"二、", c.groupName | "", "组别规则"),
      div(cls := "s2")(
        div(cls := "h2")(s"1、比赛时间：自 ${c.startsAt.toString("yyyy-MM-dd HH:mm")} 至 ${c.finishAt.toString("yyyy-MM-dd HH:mm")}")
      ),
      div(cls := "s2")(
        div(cls := "h2")(s"2、比赛项目：${c.variant.name}")
      ),
      div(cls := "s2")(
        div(cls := "h2")(s"3、用时：每方 ${c.clock.limitString} 分钟，每步棋加 ${c.clock.incrementSeconds} 秒")
      ),
      div(cls := "s2")(
        div(cls := "h2")(s"4、比赛采用${c.rule.name}赛制，共 ${c.rounds} 轮"),
        ul(
          rounds.zipWithIndex.map {
            case (r, i) => li("第 ", i + 1, " 轮：", r.startsAt.toString("yyyy-MM-dd HH:mm"))
          }
        ),
        c.appt option div(cls := "h2")(b("比赛编排后，棋手自由约定比赛时间")),
        div(cls := "h2")(b("注："), "最终轮次和时间根据报名人数，以最终编排结果为准")
      ),
      div(cls := "s2")(
        div(cls := "h2")(s"5、迟到：比赛开始 ${c.canLateMinute} 分钟仍未开棋的，视为弃权，判负")
      ),
      div(cls := "s2")(
        div(cls := "h2")(s"6、累计 ${c.canQuitNumber} 轮弃权按弃赛处理，弃赛不退费")
      ),
      div(cls := "s2")(
        div(cls := "h2")("7、对于棋手可自行约定时间的比赛，如果约棋失败，默认双方和棋，按主办方指定方式确定比赛结果")
      )
    ),
    div(cls := "s1")(
      div(cls := "h1")("三、报名要求"),
      div(cls := "s2")(
        div(cls := "h2")(s"1、比赛类型为${c.typ.name}")
      ),
      div(cls := "s2")(
        div(cls := "h2")("2、参赛选手需要满足")
      ),
      ul(
        /*        c.conditions.teamMember.isDefined option li("属于俱乐部：", c.conditions.teamMember.fold("无限制")(_.teamName)),
        c.conditions.clazzMember.isDefined option li("属于班级：", c.conditions.clazzMember.fold("无限制")(_.clazzName)),*/
        li("性别：", c.conditions.sex.fold("无限制")(_.s)),
        li(
          "年龄：",
          if (c.conditions.minAge.isEmpty && c.conditions.maxAge.isEmpty) "无限制"
          else c.conditions.minAge.fold("")(minAge => s"大于 ${minAge.age} 岁"), nbsp, c.conditions.maxAge.fold("")(maxAge => s"小于 ${maxAge.age} 岁")
        ),
        li(
          "级别：",
          if (c.conditions.minLevel.isEmpty && c.conditions.maxLevel.isEmpty) "无限制"
          else c.conditions.minLevel.fold("")(minLevel => s"大于 ${minLevel.level}"), nbsp, c.conditions.maxLevel.fold("")(maxLevel => s"小于 ${maxLevel.level}")
        ),
        li(
          "等级分：",
          if (c.conditions.minRating.isEmpty && c.conditions.maxRating.isEmpty) "无限制"
          else c.conditions.minRating.fold("")(minRating => s"大于 ${minRating.rating} 分"), nbsp, c.conditions.maxRating.fold("")(maxRating => s"小于 ${maxRating.rating} 分")
        )
      ),
      div(cls := "s2")(
        div(cls := "h2")(s"3、报名费用：${c.enterCost} 元")
      )
    ),
    div(cls := "s1")(
      div(cls := "h1")("四、附加说明"),
      div(cls := "s2")(
        c.description | "无"
      )
    ),
    div(cls := "s1")(
      div(cls := "h1")("五、免责条款"),
      div(cls := "s2")(
        "比赛组织、管理由竞赛主办方负责，包括但不限于比赛信息咨询、报名、裁判、仲裁、编排、结果发布、奖励等，如需要问题，请直接联系主办方；对以上具体比赛服务，haichess.com 仅提供系统服务，不对具体的问题负责，如果在比赛中遇到技术问题，可随时联系客服协助解决。"
      )
    )
  )

  private def enter(c: Contest, rounds: List[Round], players: List[PlayerWithUser], requests: List[RequestWithUser], invites: List[InviteWithUser])(implicit ctx: Context) =
    frag(
      !invites.forall(_.processed) option table(cls := "slist invites")(
        tbody(
          invites.map { invite =>
            tr(cls := List("processed none" -> invite.processed))(
              td(userLink(invite.user, withBadge = false)),
              td(invite.profile.realName | "-"),
              td(c.perfLens(invite.user.perfs).intRating),
              td(invite.profile.ofSex.fold("-")(_.name)),
              td(invite.profile.age.fold("-")(_.toString)),
              td(invite.profile.ofLevel.name),
              td(invite.status.name),
              td(momentFromNow(invite.date)),
              td(cls := "action")(
                !invite.processed option postForm(action := routes.Contest.inviteRemove(invite.id))(
                  button(cls := "button button-empty small button-red confirm", title := "是否确认移除？")("移除")
                )
              )
            )
          }
        )
      ),
      !requests.forall(_.processed) option table(cls := "slist requests")(
        tbody(
          requests.map { request =>
            tr(cls := List("processed none" -> request.processed))(
              td(userLink(request.user, withBadge = false)),
              td(request.profile.realName | "-"),
              td(c.perfLens(request.user.perfs).intRating),
              td(request.profile.ofSex.fold("-")(_.name)),
              td(request.profile.age.fold("-")(_.toString)),
              td(request.profile.ofLevel.name),
              td(div(cls := "nowrap-ellipsis", style := "width: 100px;", title := request.message)(request.message)),
              td(request.status.name),
              td(momentFromNow(request.date)),
              td(cls := "process")(
                !request.processed option postForm(cls := "process-request", action := routes.Contest.joinProcess(request.id))(
                  button(name := "process", cls := "button button-empty button-red small", value := "decline")(trans.decline()),
                  button(name := "process", cls := List("button button-green small" -> true, "disabled" -> c.isPlayerFull), value := "accept", c.isPlayerFull option disabled)(trans.accept())
                )
              )
            )
          }
        )
      ),
      div(cls := "waiting none")(spinner),
      table(cls := List("slist players" -> true, "unsortable" -> (!c.isPublished && !(c.isEnterStopped && rounds.exists(r => r.no == c.currentRound && r.isCreated)))))(
        thead(
          tr(
            th("序号"),
            th("账号"),
            th("姓名"),
            th("等级分"),
            th("性别"),
            th("年龄"),
            th("级别"),
            th("操作")
          )
        ),
        tbody(
          players.map { player =>
            tr(st.id := player.player.id, dataId := player.player.id, cls := List("mine" -> ctx.me.?? { user => player.user.id == user.id }))(
              td(player.no),
              td(userLink(player.user, withBadge = false)),
              td(player.profile.realName | "-"),
              td(c.perfLens(player.user.perfs).intRating),
              td(player.profile.ofSex.fold("-")(_.name)),
              td(player.profile.age.fold("-")(_.toString)),
              td(player.profile.ofLevel.name),
              td(cls := "action")(
                postForm(action := routes.Contest.removeOrKickPlayer(player.player.id))(
                  c.playerRemoveable option button(name := "action", value := "remove", cls := "button button-empty button-red small confirm player-remove", title := "是否确认移除？")("移除"),
                  c.playerKickable option button(name := "action", value := "kick", cls := List("button button-empty button-red small confirm player-kick" -> true, "disabled" -> player.player.absentOr), title := "是否确认退赛？", player.player.absentOr option disabled)(
                    if (player.player.absentOr) "已退赛" else "退赛"
                  )
                )
              )
            )
          }
        )
      )
    )

  private def forbiddenTab(c: Contest, forbiddens: List[Forbidden], players: List[PlayerWithUser])(implicit ctx: Context) = frag(
    div(cls := "forbidden-actions")(
      (isCreator(c) && (c.isPublished || c.isEnterStopped || c.isStarted)) option a(cls := "button small modal-alert", href := routes.Contest.forbiddenCreateForm(c.id))("新建回避组")
    ),
    table(cls := "slist")(
      thead(
        tr(
          th("组名"),
          th("棋手"),
          c.isStarted option th("操作")
        )
      ),
      tbody(
        forbiddens.map { forbidden =>
          tr(
            td(forbidden.name),
            td(forbidden.withPlayer(players).map(_._2.username).mkString(", ")),
            c.isStarted option td(cls := "action")(
              a(cls := "button button-empty small modal-alert", href := routes.Contest.forbiddenUpdateForm(c.id, forbidden.id))("编辑"),
              postForm(action := routes.Contest.removeForbidden(c.id, forbidden.id))(
                button(cls := "button button-empty small button-red confirm", title := "确认删除？")("删除")
              )
            )
          )
        }
      )
    )
  )

  val dataContestId = attr("data-contest-id")
  val dataRoundNo = attr("data-round-no")
  private def round(c: Contest, rounds: List[Round], players: List[PlayerWithUser], boards: List[Board], rno: Int)(implicit ctx: Context) = {
    val round = findRound(rno, rounds)
    frag(
      (isCreator(c) && !c.autoPairing) option div(
        div(cls := "round-actions")(
          ((c.isEnterStopped || c.isStarted) && round.isCreated) option a(cls := "button small modal-alert absent", href := routes.Contest.manualAbsentForm(c.id, rno))("弃权设置"),
          ((c.isEnterStopped || c.isStarted) && round.isCreated) option postForm(st.action := routes.Contest.pairing(c.id, rno), dataContestId := c.id)(
            submitButton(cls := "button small pairing", title := "是否确认生成对战表？")("生成对战表")
          ),
          ((c.isEnterStopped || c.isStarted) && round.isPairing) option postForm(st.action := routes.Contest.publishPairing(c.id, rno), dataContestId := c.id)(
            submitButton(cls := "button small publish-pairing", title := "是否确认发布对战表？")("发布对战表")
          ),
          ((c.isEnterStopped || c.isStarted) && round.isFinished) option postForm(st.action := routes.Contest.publishResult(c.id, rno))(
            submitButton(cls := "button small publish-result confirm", title := "是否确认发布成绩？")("发布成绩")
          ),
          (c.isEnterStopped || c.isStarted) && (round.isCreated || round.isPairing) option div(cls := "round-starts")(
            label("本轮开始时间："),
            st.input(cls := "flatpickr", dataEnableTime := true, datatime24h := true, dataContestId := c.id, dataRoundNo := rno, name := "roundStartsTime", value := round.actualStartsAt.toString("yyyy-MM-dd HH:mm"))
          )
        )
      ),
      table(cls := "slist")(
        thead(
          tr(
            th("台号"),
            th("序号"),
            th("白方"),
            th("积分"),
            th("结果"),
            th("积分"),
            th("黑方"),
            th("序号"),
            th("操作")
          )
        ),
        tbody(
          round.isOverPairing option boards.filter(_.roundNo == rno).map {
            board =>
              {
                val white = findPlayer(board.whitePlayer.no, players)
                val black = findPlayer(board.blackPlayer.no, players)
                val color = ctx.me.fold("white")(u => board.colorOfById(u.id).name)
                tr(cls := List("mine" -> ctx.me.?? { user => board.contains(user.id) }))(
                  td(cls := "no")(s"#${board.no}"),
                  td(board.whitePlayer.no),
                  td(
                    div(cls := List("manual" -> !c.autoPairing))(
                      userLink(white.user, withBadge = false),
                      (isCreator(c) && !c.autoPairing && (c.isEnterStopped || c.isStarted) && round.isPairing) option div(cls := "actions")(
                        a(cls := "button button-empty small modal-alert manual-pairing", dataIcon := "B", title := "调整对阵", href := routes.Contest.manualPairingNotBeyForm(c.id, board.id, true))
                      )
                    )
                  ),
                  td(white.player.roundScore(rno)),
                  td(
                    div(cls := "nowrap")(board.resultShow),
                    (board.isCreated && ((board.appt && board.apptComplete) || round.startsAt != board.startsAt)) option div(board.startsAt.toString("MM-dd HH:mm"))
                  ),
                  td(black.player.roundScore(rno)),
                  td(
                    div(cls := List("manual" -> !c.autoPairing))(
                      userLink(black.user, withBadge = false),
                      (isCreator(c) && !c.autoPairing && (c.isEnterStopped || c.isStarted) && round.isPairing) option div(cls := "actions")(
                        a(cls := "button button-empty small modal-alert manual-pairing", dataIcon := "B", title := "调整对阵", href := routes.Contest.manualPairingNotBeyForm(c.id, board.id, false))
                      )
                    )
                  ),
                  td(board.blackPlayer.no),
                  td(
                    c.isOverPublished option frag(
                      if (board.appt) {
                        if (board.apptComplete) {
                          a(cls := "button button-empty small", href := routes.Round.watcher(board.id, color))(if (ctx.me.??(u => board.contains(u.id))) "进入" else "观看")
                        } else {
                          if (ctx.me.??(u => board.contains(u.id))) {
                            if ((c.isEnterStopped || c.isStarted) && (round.isPublished || round.isStarted) && board.isCreated) {
                              a(cls := "button button-empty small", href := routes.Appt.form(board.id))("约棋")
                            } else frag()
                          } else frag()
                        }
                      } else a(cls := "button button-empty small", href := routes.Round.watcher(board.id, color))(if (ctx.me.??(u => board.contains(u.id))) "进入" else "观看")
                    ),
                    nbsp,
                    (isCreator(c) && (c.isEnterStopped || c.isStarted) && (round.isPublished || round.isStarted) && board.isCreated) option
                      a(cls := "button button-empty small modal-alert", href := routes.Contest.setBoardTimeForm(board.id))("设置时间"),
                    (isCreator(c) && !c.autoPairing && (c.isStarted || c.isFinished) && round.isFinished) option
                      a(cls := "button button-empty small modal-alert manual-result", href := routes.Contest.manualResultForm(board.contestId, board.id))("设置成绩")
                  )
                )
              }
          },
          players.filter(_.player.noBoard(rno)).sortWith((p1, p2) => p1.player.roundOutcomeSort(rno) > p2.player.roundOutcomeSort(rno)).map {
            playerWithUser =>
              {
                val player = playerWithUser.player
                val result = player.roundOutcomeFormat(rno)
                tr(title := result, cls := List("mine" -> ctx.me.?? { user => player.userId == user.id }))(
                  td("-"),
                  td(player.no),
                  td(cls := List("manual" -> player.isBye(rno)))(
                    userLink(playerWithUser.user, withBadge = false),
                    (isCreator(c) && !c.autoPairing && (c.isEnterStopped || c.isStarted) && round.isPairing && player.isBye(rno)) option div(cls := "actions")(
                      a(cls := "button button-empty small modal-alert manual-pairing", dataIcon := "B", title := "调整对阵", href := routes.Contest.manualPairingBeyForm(c.id, round.id, player.id))
                    )
                  ),
                  td(player.roundScore(rno)),
                  td(cls := "nowrap")(result),
                  td("-"),
                  td("-"),
                  td("-"),
                  td("-")
                )
              }
          }
        )
      )
    )
  }

  private def score(c: Contest, rounds: List[Round], players: List[PlayerWithUser], scoreSheets: List[ScoreSheet])(implicit ctx: Context) = frag(
    (isCreator(c) && !c.autoPairing) option div(cls := "score-actions")(
      (c.isStarted && c.allRoundFinished) option postForm(st.action := routes.Contest.publishScoreAndFinish(c.id))(
        submitButton(cls := "button small publish-result confirm", title := "是否确认发布成绩？")("发布成绩")
      )
    ),
    table(cls := "slist")(
      thead(
        tr(
          th("名次"),
          th("序号"),
          th("棋手"),
          th("积分"),
          c.btsss.filterNot(_.id == "no").map(btss => th(btss.name)),
          th("操作")
        )
      ),
      tbody(
        scoreSheets.map { scoreSheet =>
          {
            val playerWithUser = findPlayer(scoreSheet.playerNo, players)
            tr(cls := List("mine" -> ctx.me.?? { user => playerWithUser.player.userId == user.id }))(
              td(if ((isCreator(c) || c.isFinishedOrCanceled) && scoreSheet.cancelled) "-" else scoreSheet.rank),
              td(scoreSheet.playerNo),
              td(userLink(playerWithUser.user, withBadge = false)),
              td(strong(scoreSheet.score)),
              scoreSheet.btssScores.filterNot(_.btss.id == "no").map(btss => td(btss.score)),
              td(style := "display: flex;")(
                (isCreator(c) && !c.autoPairing && c.isStarted && c.allRoundFinished) option postForm(action := routes.Contest.cancelScore(scoreSheet.contestId, scoreSheet.id))(
                  submitButton(cls := List("button button-empty button-red small confirm score-cancel" -> true, "disabled" -> scoreSheet.cancelled), title := "取消比赛成绩将不可恢复，是否确认操作？", scoreSheet.cancelled option disabled)(if (scoreSheet.cancelled) "已取消" else "取消成绩")
                ),
                a(cls := "button button-empty small modal-alert score-detail", href := routes.Contest.scoreDetail(c.id, scoreSheet.roundNo, scoreSheet.playerUid))("详情")
              )
            )
          }
        }
      )
    )
  )

  private def isRuleTabActive(c: Contest)(implicit ctx: Context) = c.isCreated || (c.isPublished && !isCreator(c))
  private def isEnterTabActive(c: Contest)(implicit ctx: Context) = c.isPublished && isCreator(c)
  private def isRoundTabActive(c: Contest, r: Int, rounds: List[Round])(implicit ctx: Context) = {
    val rd = findRound(r, rounds)
    !c.isFinishedOrCanceled && !c.allRoundFinished && (rd.isPublished || rd.isStarted || ((c.isEnterStopped || c.isStarted) && !c.autoPairing && c.currentRound == r))
  }
  private def isScoreTabActive(c: Contest)(implicit ctx: Context) = c.isFinished || c.isCanceled || c.allRoundFinished
  private def isRoundTabDisabled(c: Contest, r: Int, rounds: List[Round])(implicit ctx: Context) = {
    val rd = findRound(r, rounds)
    (rd.isCreated || rd.isPairing) && !(!c.autoPairing && c.currentRound == r)
  }
  private def isScoreTabDisabled(c: Contest)(implicit ctx: Context) = !c.isOverStarted
  private def isCreator(c: Contest)(implicit ctx: Context): Boolean = ctx.me ?? c.isCreator

  private def enterButtonText(c: Contest, myRequest: Option[Request], myInvite: Option[Invite], players: List[PlayerWithUser])(implicit ctx: Context): String = c.status match {
    case Contest.Status.Created => "筹备中"
    case Contest.Status.Published =>
      c.isPlayerFull match {
        case true => "名额已满"
        case false => {
          def defaultText = {
            findPlayer(players) match {
              case None =>
                myRequest match {
                  case None => "报名"
                  case Some(r) => r.status match {
                    case Request.RequestStatus.Invited => "等待审核"
                    case Request.RequestStatus.Joined => "已报名"
                    case Request.RequestStatus.Refused => inviteText("您被拒绝加入比赛", myInvite)
                  }
                }
              case Some(p) => if (p.player.absentOr) "已退赛" else "已报名"
            }
          }
          inviteText(defaultText, myInvite)
        }
      }

    case Contest.Status.EnterStopped => {
      def defaultText = {
        findPlayer(players) match {
          case None => "报名截止"
          case Some(p) => if (p.player.absentOr) "已退赛" else "报名截止"
        }
      }
      inviteText(defaultText, myInvite)
    }
    case Contest.Status.Started => {
      def defaultText = {
        findPlayer(players) match {
          case None => "比赛中"
          case Some(p) => if (p.player.absentOr) "已退赛" else "比赛中"
        }
      }
      inviteText(defaultText, myInvite)
    }
    case Contest.Status.Finished => "比赛结束"
    case Contest.Status.Canceled => "比赛取消"
  }

  private def inviteText(defaultText: String, myInvite: Option[Invite]): String =
    myInvite match {
      case None => defaultText
      case Some(iv) => iv.status match {
        case Invite.InviteStatus.Invited => "确认报名"
        case Invite.InviteStatus.Joined => defaultText
        case Invite.InviteStatus.Refused => defaultText + "-您已拒绝加入比赛"
      }
    }

  private def enterButtonStatus(c: Contest, myRequest: Option[Request], myInvite: Option[Invite]) = c.status match {
    case Contest.Status.Created => false
    case Contest.Status.Published => myRequest match {
      case None =>
        if (!c.isPlayerFull) {
          myInvite match {
            case None => true
            case Some(iv) => iv.status match {
              case Invite.InviteStatus.Invited => true
              case _ => false
            }
          }
        } else false
      case Some(_) => inviteStatus(myInvite)
    }
    case Contest.Status.EnterStopped => inviteStatus(myInvite)
    case Contest.Status.Started => inviteStatus(myInvite)
    case _ => false
  }

  private def inviteStatus(myInvite: Option[Invite]): Boolean =
    myInvite match {
      case None => false
      case Some(iv) => iv.status match {
        case Invite.InviteStatus.Invited => true
        case _ => false
      }
    }

  private def attachments(filePath: String)(implicit ctx: Context) = {
    val fileName = filePath.split("/").toList.reverse.head
    def dbFileUrl(path: String) = s"//${lila.api.Env.current.Net.AssetDomain}/file/$path"
    a(href := dbFileUrl(filePath))(fileName)
  }

  private def findPlayer(no: Player.No, players: List[PlayerWithUser]): PlayerWithUser =
    players.find(_.player.no == no) err s"can not find player：$no"

  private def findRound(no: Round.No, rounds: List[Round]): Round =
    rounds.find(_.no == no) err s"can not find round：$no"

  private def findStartBoard(boards: List[Board])(implicit ctx: Context): Option[Board] =
    ctx.me.?? { user =>
      boards.find(b => b.contains(user.id) && b.isStarted)
    }

  private def findPlayer(players: List[PlayerWithUser])(implicit ctx: Context): Option[PlayerWithUser] =
    ctx.me.?? { user =>
      players.find(pwu => pwu.player.userId == user.id)
    }

  private def belongTo(pwu: PlayerWithUser)(implicit ctx: Context): Boolean =
    ctx.me.?? { user =>
      pwu.player.is(user.id)
    }
  private def isForbiddenTabDisabled(c: Contest)(implicit ctx: Context) = c.isCreated
}
