package views.html.offlineContest

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.offlineContest.{ OffBoard, OffContest, OffForbidden, OffPlayer, OffRound, OffScoreSheet }
import controllers.routes

object show {

  private val dataTab = attr("data-tab")
  def apply(
    c: OffContest,
    rounds: List[OffRound],
    players: List[OffPlayer.PlayerWithUser],
    boards: List[OffBoard],
    forbiddens: List[OffForbidden],
    scoreSheets: List[OffScoreSheet]
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"比赛 ${c.name}",
    moreCss = cssTag("offlineContest"),
    moreJs = frag(
      printTag,
      tableDnDTag,
      cookieTag,
      flatpickrTag,
      delayFlatpickrStart,
      jsTag("offlineContest.show.js")
    )
  ) {
      main(cls := "page-small contest-show", dataId := c.id)(
        div(cls := "box box-pad head")(
          div(cls := "head__info")(
            baseInfo(c)
          ),
          div(cls := "head__enter")(
            submitButton(cls := "button button-green enter disabled", disabled)(c.status.name)
          ),
          div(cls := "head__action")(
            c.isCreated option postForm(st.action := routes.OffContest.start(c.id))(
              submitButton(cls := List("button button-green start confirm" -> true, "disabled" -> (c.nbPlayers < 2)), (c.nbPlayers < 2) option disabled, title := "确认开始比赛？")("开始比赛")
            ),
            c.isCreated option a(cls := "button button-empty update", href := routes.OffContest.updateForm(c.id), title := "编辑比赛")("编辑"),
            c.isCreated option postForm(st.action := routes.OffContest.remove(c.id))(
              submitButton(cls := "button button-red button-empty remove confirm", title := "删除比赛将不可恢复，是否确认？")("删除")
            ),
            c.isStarted option postForm(st.action := routes.OffContest.cancel(c.id))(
              submitButton(cls := "button button-red button-empty cancel confirm", title := "确认取消比赛？")("取消")
            )
          )
        ),
        div(cls := "box box-pad flow")(
          div(cls := "tabs")(
            div(dataTab := "enter", cls := List("active running" -> isEnterTabActive(c)))("棋手管理"),
            div(dataTab := "forbidden", cls := List("disabled" -> isForbiddenTabDisabled(c)))("回避设置"),
            rounds.map { round =>
              div(dataTab := s"round${round.no}", cls := List("active running" -> isRoundTabActive(c, round.no), "disabled" -> isRoundTabDisabled(c, round.no)))(s"第${round.no}轮")
            },
            div(dataTab := "score", cls := List("active running" -> isScoreTabActive(c), "disabled" -> isScoreTabDisabled(c)))("成绩册")
          ),
          div(cls := "panels")(
            div(cls := List("panel enter" -> true, "active" -> isEnterTabActive(c)))(enterTab(c, players)),
            div(cls := List("panel forbidden" -> true))(forbiddenTab(c, forbiddens, players)),
            rounds.map { round =>
              div(cls := List(s"panel round round${round.no}" -> true, "active" -> isRoundTabActive(c, round.no)))(roundTab(c, round, players, boards))
            },
            div(cls := List("panel score" -> true, "active" -> isScoreTabActive(c)))(scoreTab(c, players, scoreSheets))
          )
        )
      )
    }

  private def baseInfo(c: OffContest)(implicit ctx: Context) =
    table(
      tr(
        td(
          img(cls := "logo", src := c.logo.fold(staticUrl("images/contest.svg")) { l => dbImageUrl(l) })
        ),
        td(
          div(cls := "contest-name")(c.fullName),
          div(cls := "organizer")("主办方：", c.typ match {
            case OffContest.Type.Public | OffContest.Type.TeamInner => teamLinkById(c.organizer, false)
            case OffContest.Type.ClazzInner => clazzLinkById(c.organizer)
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
        td("报名人数：", c.nbPlayers)
      )
    )

  private def enterTab(c: OffContest, players: List[OffPlayer.PlayerWithUser])(implicit ctx: Context) = frag(
    div(cls := "enter-actions")(
      (c.isCreated || c.isStarted) option a(cls := "button small modal-alert player-choose", href := routes.OffContest.playerChooseForm(c.id))("选择棋手"),
      (c.isCreated || c.isStarted) option a(cls := "button small modal-alert player-external", href := routes.OffContest.externalPlayerForm(c.id))("增加临时棋手"),
      (c.nbPlayers >= 2) option a(cls := "button small print-players")("打印花名册")
    ),
    div(cls := "print-area")(
      div(cls := "waiting none")(spinner),
      printTitle(c, none, "花名册"),
      table(cls := List("slist" -> true, "unsortable" -> c.isOverStarted))(
        thead(
          tr(
            th("序号"),
            th("姓名"),
            th("系统账号"),
            th(cls := "no-print")("俱乐部等级分"),
            th(cls := "no-print")("操作")
          )
        ),
        tbody(
          players.map { pwu =>
            tr(st.id := pwu.playerId, dataId := pwu.playerId)(
              td(cls := "no")(pwu.no),
              td(pwu.realNameOrUsername),
              td(if (pwu.player.external) "-" else userLink(pwu.user, withBadge = false)),
              td(cls := "no-print")(pwu.player.teamRating | 0),
              td(cls := "action no-print")(
                postForm(action := routes.OffContest.removeOrKickPlayer(pwu.player.id))(
                  c.playerRemoveable option button(name := "action", value := "remove", cls := "button button-empty small button-red confirm player-remove", title := "确认移除？")("移除"),
                  c.playerKickable option button(name := "action", value := "kick", cls := List("button button-empty small button-red confirm player-kick" -> true, "disabled" -> pwu.player.absentOr), title := "确认退赛？", pwu.player.absentOr option disabled)(
                    if (pwu.player.absentOr) "已退赛" else "退赛"
                  )
                )
              )
            )
          }
        )
      )
    )
  )

  private def forbiddenTab(c: OffContest, forbiddens: List[OffForbidden], players: List[OffPlayer.PlayerWithUser])(implicit ctx: Context) = frag(
    div(cls := "forbidden-actions")(
      c.isStarted option a(cls := "button small modal-alert", href := routes.OffContest.forbiddenCreateForm(c.id))("新建回避组")
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
            td(forbidden.withPlayer(players).map(_._2.realNameOrUsername).mkString(", ")),
            c.isStarted option td(cls := "action")(
              a(cls := "button button-empty small modal-alert", href := routes.OffContest.forbiddenUpdateForm(c.id, forbidden.id))("编辑"),
              postForm(action := routes.OffContest.removeForbidden(c.id, forbidden.id))(
                button(cls := "button button-empty small button-red confirm", title := "确认删除？")("删除")
              )
            )
          )
        }
      )
    )
  )

  private def roundTab(contest: OffContest, round: OffRound, players: List[OffPlayer.PlayerWithUser], boards: List[OffBoard])(implicit ctx: Context) = {
    frag(
      div(cls := "round-actions")(
        (contest.isStarted && round.isCreated) option a(cls := "button small modal-alert absent", href := routes.OffContest.manualAbsentForm(contest.id, round.no))("弃权设置"),
        (contest.isStarted && round.isCreated) option postForm(st.action := routes.OffContest.pairing(contest.id, round.no))(
          submitButton(cls := "button small pairing", title := "确认生成对战表？")("生成对战表")
        ),
        (contest.isStarted && round.isPairing) option postForm(st.action := routes.OffContest.publishPairing(contest.id, round.no))(
          submitButton(cls := "button small publish-pairing confirm", title := "确认发布对战表？")("发布对战表")
        ),
        (contest.isStarted && round.isPublished) option a(cls := "button small print-board-sheet", dataId := round.no)("打印对战表"),
        (contest.isStarted && round.isPublished) option postForm(st.action := routes.OffContest.publishResult(contest.id, round.no))(
          submitButton(cls := "button small publish-result", title := "确认发布成绩？")("发布成绩")
        ),
        round.isPublishResult option a(cls := "button small print-round-score", dataId := round.no)("打印本轮成绩")
      ),
      div(cls := "print-area round" + round.no)(
        printTitle(contest, round.some, "对战表"),
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
              (contest.isStarted && round.isPublished) option th(cls := "no-print")("操作")
            )
          ),
          tbody(
            round.isOverPairing option boards.filter(_.roundNo == round.no).map { board =>
              {
                val white = findPlayer(board.whitePlayer.no, players)
                val black = findPlayer(board.blackPlayer.no, players)
                tr(
                  td(cls := "no")(s"#${board.no}"),
                  td(board.whitePlayer.no),
                  td(cls := "manual")(
                    white.realNameOrUsername,
                    (contest.isStarted && round.isPairing) option div(cls := "actions")(
                      a(cls := "button button-empty small modal-alert manual-pairing", dataIcon := "B", title := "调整对阵", href := routes.OffContest.manualPairingNotBeyForm(contest.id, board.id, true))
                    )
                  ),
                  td(white.player.roundScore(round.no)),
                  td(cls := List("result nowrap" -> true, "start" -> board.isStarted, "finish" -> board.isFinished))(b(board.resultShow)),
                  td(black.player.roundScore(round.no)),
                  td(cls := "manual")(
                    black.realNameOrUsername,
                    (contest.isStarted && round.isPairing) option div(cls := "actions")(
                      a(cls := "button button-empty small modal-alert manual-pairing", dataIcon := "B", title := "调整对阵", href := routes.OffContest.manualPairingNotBeyForm(contest.id, board.id, false))
                    )
                  ),
                  td(board.blackPlayer.no),
                  (contest.isStarted && round.isPublished) option td(cls := "no-print")(
                    a(cls := "button button-empty small modal-alert manualResult", href := routes.OffContest.manualResultForm(contest.id, board.id))("设置成绩")
                  )
                )
              }
            },
            players.filter(_.player.noBoard(round.no)).sortWith((p1, p2) => p1.player.roundOutcomeSort(round.no) > p2.player.roundOutcomeSort(round.no)).map { playerWithUser =>
              {
                val player = playerWithUser.player
                val result = player.roundOutcomeFormat(round.no)
                tr(title := result)(
                  td(cls := "no")("-"),
                  td(player.no),
                  td(cls := "manual")(
                    playerWithUser.realNameOrUsername,
                    (contest.isStarted && round.isPairing && player.isBye(round.no)) option div(cls := "actions")(
                      a(cls := "button button-empty small modal-alert manual-pairing", dataIcon := "B", title := "调整对阵", href := routes.OffContest.manualPairingBeyForm(contest.id, round.id, player.id))
                    )
                  ),
                  td(player.roundScore(round.no)),
                  td(cls := "result nowrap absent")(b(result)),
                  td("-"),
                  td("-"),
                  td("-"),
                  (contest.isStarted && round.isPublished) option td(cls := "no-print")("-")
                )
              }
            }
          )
        )
      )
    )
  }

  private def scoreTab(c: OffContest, players: List[OffPlayer.PlayerWithUser], scoreSheets: List[OffScoreSheet])(implicit ctx: Context) = frag(
    div(cls := "score-actions")(
      c.isOverStarted option a(cls := "button small print-score")("打印成绩册")
    ),
    div(cls := "print-area")(
      printTitle(c, none, "成绩册"),
      table(cls := "slist")(
        thead(
          tr(
            th("名次"),
            th("（序号）棋手"),
            th("积分"),
            c.btsss.filterNot(_.id == "no").map(btss => th(btss.name))
          )
        ),
        tbody(
          scoreSheets.map { scoreSheet =>
            {
              val playerWithUser = findPlayer(scoreSheet.playerNo, players)
              tr(
                td(scoreSheet.rank),
                td(s"（${scoreSheet.playerNo}）", playerWithUser.realNameOrUsername),
                td(b(scoreSheet.score)),
                scoreSheet.btssScores.filterNot(_.btss.id == "no").map(btss => td(btss.score))
              )
            }
          }
        )
      )
    )
  )

  private def isEnterTabActive(c: OffContest)(implicit ctx: Context) = c.isCreated
  private def isRoundTabActive(c: OffContest, rno: Int)(implicit ctx: Context) = c.isStarted && c.currentRound == rno
  private def isScoreTabActive(c: OffContest)(implicit ctx: Context) = c.isFinishedOrCanceled
  private def isForbiddenTabDisabled(c: OffContest)(implicit ctx: Context) = c.isCreated
  private def isRoundTabDisabled(c: OffContest, rno: Int)(implicit ctx: Context) = c.isCreated || rno > c.currentRound
  private def isScoreTabDisabled(c: OffContest)(implicit ctx: Context) = c.isCreated
  private def findPlayer(no: OffPlayer.No, players: List[OffPlayer.PlayerWithUser]): OffPlayer.PlayerWithUser =
    players.find(_.player.no == no) err s"can not find player：$no"

  private def printTitle(c: OffContest, r: Option[OffRound], title: String) = div(cls := "printTitle none")(
    div(cls := "cName")(c.fullName, nbsp, span(cls := "subName")(title)),
    r.map { round =>
      div(cls := "rName")(s"第 ${round.no} 轮")
    }
  )

}
