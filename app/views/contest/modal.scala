package views.html.contest

import lila.api.Context
import lila.game.Game
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.contest.{ Board, Contest, Forbidden, ManualPairingSource, Player, PlayerWithUser, Round }
import controllers.routes

object modal {

  def invite(c: Contest)(implicit ctx: Context) = frag(
    div(cls := "modal-content contest-invite none")(
      h2("邀请棋手"),
      postForm(cls := "form3", action := routes.Contest.invite(c.id))(
        p(cls := "info", dataIcon := (""))("请邀请您认识并且希望参与这个比赛的棋手"),
        div(cls := "input-wrapper")(
          form3.hidden("contestId", c.id),
          input(cls := "user-autocomplete", placeholder := "按用户名搜索", name := "username", required, dataTag := "span")
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交", klass = "small")
        )
      )
    )
  )

  def manualAbsent(c: Contest, r: Round, players: List[Player])(implicit ctx: Context) = frag(
    div(cls := "modal-content contest-absent none")(
      h2("弃权设置"),
      postForm(cls := "form3", action := routes.Contest.manualAbsent(c.id, r.no))(
        form3.hidden("contestId", c.id),
        div(cls := "transfer")(
          transferPanel(title = "参赛棋手", clss = "left", players = players.filterNot(_.absent)),
          div(cls := "transfer-buttons")(
            button(cls := "button small arrow-left disabled", dataIcon := "I", disabled),
            button(cls := "button small arrow-right disabled", dataIcon := "H", disabled)
          ),
          transferPanel(title = "弃权棋手", clss = "right", players = players.filter(p => p.manualAbsent && !p.absentOr))
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交", klass = "small")
        )
      )
    )
  )

  private def transferPanel(title: String, clss: String, players: List[Player]) =
    div(cls := List("transfer-panel" -> true, clss -> true))(
      div(cls := "transfer-panel-head")(title),
      div(cls := "transfer-panel-search")(
        input(tpe := "text", cls := "transfer-search", placeholder := "搜索")
      ),
      div(cls := "transfer-panel-list")(
        ul(
          players.map { player =>
            li(cls := "transfer-panel-item")(
              input(tpe := "checkbox", id := player.id, name := "absent", value := player.id),
              nbsp,
              label(`for` := player.id)("#", player.no, nbsp, player.userId)
            )
          }
        )
      )
    )

  def playerForbidden(contest: Contest, players: List[PlayerWithUser], forbidden: Option[Forbidden])(implicit ctx: Context) = frag(
    div(cls := "modal-content contest-forbidden none")(
      h2("回避设置"),
      postForm(cls := "form3", action := routes.Contest.forbiddenApply(contest.id, forbidden.map(_.id)))(
        form3.hidden("playerIds", forbidden.??(_.playerIds.mkString(","))),
        div(cls := "fname")(input(name := "name", value := forbidden.map(_.name), required, minlength := 2, maxlength := 20, placeholder := "组名")),
        div(cls := "transfer")(
          div(cls := "transfer-panel left")(
            div(cls := "transfer-panel-head")("参赛棋手"),
            div(cls := "transfer-panel-search")(
              input(tpe := "text", cls := "transfer-search", placeholder := "搜索")
            ),
            div(cls := "transfer-panel-list")(
              table(cls := "transfer-table")(
                tbody(
                  players.filterNot(p => forbidden.??(_.playerIds.contains(p.playerId))).map { pwu =>
                    tr(
                      td(input(tpe := "checkbox", id := s"chk_${pwu.userId}", value := pwu.playerId, dataAttr := pwu.json.toString())),
                      td(b(pwu.no)),
                      td(cls := "name")(pwu.username)
                    )
                  }
                )
              )
            )
          ),
          div(cls := "transfer-buttons")(
            button(cls := "button small arrow-left disabled", dataIcon := "I", disabled),
            button(cls := "button small arrow-right disabled", dataIcon := "H", disabled)
          ),
          div(cls := "transfer-panel right")(
            div(cls := "transfer-panel-head")("回避棋手"),
            div(cls := "transfer-panel-search")(
              input(tpe := "text", cls := "transfer-search", placeholder := "搜索")
            ),
            div(cls := "transfer-panel-list")(
              table(cls := "transfer-table")(
                tbody(
                  players.filter(p => forbidden.??(_.playerIds.contains(p.playerId))).map { pwu =>
                    tr(
                      td(input(tpe := "checkbox", id := s"chk_${pwu.userId}", value := pwu.playerId, dataAttr := pwu.json.toString())),
                      td(b(pwu.no)),
                      td(cls := "name")(pwu.username)
                    )
                  }
                )
              )
            )
          )
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("保存", klass = "small")
        )
      )
    )
  )

  def manualPairing(c: Contest, r: Round, boards: List[Board], players: List[Player], source: ManualPairingSource)(implicit ctx: Context) = frag(
    div(cls := "modal-content manual-pairing none")(
      h2("与...交换"),
      postForm(cls := "form3", action := routes.Contest.manualPairing(c.id, r.no))(
        p(cls := "is-gold", dataIcon := "")("手动调整对阵可能会影响后续匹配，请谨慎操作！"),
        form3.hidden("contestId", c.id),
        !source.isBye option form3.hidden("source", s"""{"isBye":0, "board": "${source.board_.id}", "color": ${source.color_.fold(1, 0)}}"""),
        source.isBye option form3.hidden("source", s"""{"isBye":1, "player": "${source.player_.id}"}"""),
        div(cls := "manual-source")(
          table(cls := "slist")(
            tbody(
              !source.isBye option tr(
                td(source.board_.no),
                td("#", nbsp, source.board_.player(source.color_).no),
                td(source.board_.player(source.color_).userId),
                td(source.color_.fold("白方", "黑方"))
              ),
              source.isBye option tr(
                td("-"),
                td("-"),
                td(source.player_.userId),
                td("-")
              )
            )
          )
        ),
        div(cls := "manual-filter")(
          input(tpe := "text", cls := "manual-filter-search", placeholder := "搜索")
        ),
        div(cls := "manual-list")(
          table(cls := "slist")(
            thead(
              tr(
                th("台号"),
                th("白方"),
                th("黑方")
              )
            ),
            tbody(
              boards.map { board =>
                {
                  val w = board.whitePlayer
                  val b = board.blackPlayer
                  tr(
                    td(board.no),
                    td(cls := List("white" -> true, "disabled" -> (source.board.??(_.is(board)) && source.color ?? (_.name == "white"))))(
                      label(`for` := w.id, cls := "user-label")("#", w.no, nbsp, w.userId),
                      nbsp,
                      input(tpe := "radio", id := w.id, name := "user-radio", value := s"""{"isBye": 0, "board": "${board.id}", "color": 1}""", (source.board.??(_.is(board)) && source.color ?? (_.name == "white")) option disabled)
                    ),
                    td(cls := List("black" -> true, "disabled" -> (source.board.??(_.is(board)) && source.color ?? (_.name == "black"))))(
                      label(`for` := b.id)("#", b.no, nbsp, b.userId),
                      nbsp,
                      input(tpe := "radio", id := b.id, name := "user-radio", value := s"""{"isBye": 0, "board": "${board.id}", "color": 0}""", (source.board.??(_.is(board)) && source.color ?? (_.name == "black")) option disabled)
                    )
                  )
                }
              },
              players.filter(_.isBye(r.no)).map { player =>
                tr(title := "轮空")(
                  td("-"),
                  td(cls := List("white" -> true, "disabled" -> source.isBye))(
                    label(`for` := player.id, cls := "user-label")("#", player.no, nbsp, player.userId),
                    nbsp,
                    input(tpe := "radio", id := player.id, name := "user-radio", value := s"""{"isBye": 1, "player": "${player.id}"}""", source.isBye option disabled)
                  ),
                  td(cls := List("black" -> true))("-")
                )
              }
            )
          )
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交", klass = "small")
        )
      )
    )
  )

  def manualResult(c: Contest, r: Round, b: Board)(implicit ctx: Context) = frag(
    div(cls := "modal-content manual-result none")(
      h2("设置成绩"),
      postForm(cls := "form3", action := routes.Contest.manualResult(c.id, b.id))(
        select(name := "result")(
          option(value := "1")("1-0（白方胜）"),
          option(value := "0")("0-1（黑方胜）"),
          option(value := "-")("1/2-1/2（平局）")
        ),
        p("手工设置过比赛成绩，必须先发布成绩，在下一轮才能进行编排"),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交", klass = "small")
        )
      )
    )
  )

  def scoreDetail(no: Round.No, player: Player, boards: List[Board])(implicit ctx: Context) = frag(
    div(cls := "modal-content score-detail none")(
      h2("详情"),
      table(cls := "slist")(
        thead(
          tr(
            th("轮次"),
            th("序号"),
            th("白方"),
            th("结果"),
            th("黑方"),
            th("序号"),
            th("操作")
          )
        ),
        tbody(
          (1 to no).toList.map { n =>
            boards.find(_.roundNo == n).map { board =>
              val color = ctx.me.fold("white")(u => board.colorOfById(u.id).name)
              tr(
                td(n),
                td(board.whitePlayer.no),
                td(board.whitePlayer.userId),
                td(strong(board.resultFormat)),
                td(board.blackPlayer.userId),
                td(board.blackPlayer.no),
                td(
                  a(target := "_blank", href := routes.Round.watcher(board.id, color))("查看")
                )
              )
            } getOrElse {
              tr(
                td(n),
                td("-"),
                td(player.userId),
                td(strong(player.roundOutcomeFormat(n))),
                td("-"),
                td("-"),
                td("-")
              )
            }
          }
        )
      )
    )
  )

  def setBoardTime(contest: Contest, round: Round, board: Board)(implicit ctx: Context) = frag(
    div(cls := "modal-content board-time none")(
      h2("时间设置"),
      postForm(cls := "form3", action := routes.Contest.setBoardTime(board.id))(
        div(cls := "form-group")(
          st.input(
            name := "startsAt",
            value := board.startsAt.toString("yyyy-MM-dd HH:mm"),
            cls := "form-control flatpickr",
            dataEnableTime := true,
            datatime24h := true
          )(
              dataMinDate := round.actualStartsAt.toString("yyyy-MM-dd HH:mm"),
              dataMaxDate := maxTimeLimit(contest, round)
            )
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("提交", klass = "small")
        )
      )
    )
  )

  private def maxTimeLimit(contest: Contest, round: Round) = {
    if (!contest.appt) {
      contest.finishAt.toString("yyyy-MM-dd HH:mm")
    } else {
      round.actualStartsAt.plusMinutes(contest.roundSpace).minusMinutes(contest.apptDeadline | 0).toString("yyyy-MM-dd HH:mm")
    }
  }

}
