package views.html.offlineContest

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.offlineContest.{ OffContest, OffPlayer, OffRound, OffBoard, OffForbidden, OffManualPairingSource }
import controllers.routes

object modal {

  def playerChoose(
    contest: OffContest,
    clazzs: List[(String, String)],
    teamTags: List[lila.team.Tag],
    allPlayers: List[OffPlayer.AllPlayerWithUser],
    players: List[OffPlayer.PlayerWithUser]
  )(implicit ctx: Context) = frag(
    div(cls := "modal-content player-choose none")(
      h2("选择棋手"),
      st.form(cls := "member__search")(
        table(
          tr(
            td("主办方"),
            td(colspan := 3)(
              contest.typ match {
                case OffContest.Type.Public | OffContest.Type.TeamInner => teamLinkById(contest.organizer, false)
                case OffContest.Type.ClazzInner => clazzLinkById(contest.organizer)
              }
            )
          ),
          (contest.typ == OffContest.Type.Public || contest.typ == OffContest.Type.TeamInner) option tr(
            td("班级"),
            td(colspan := 3)(mselect("clazz", clazzs))
          ),
          tr(
            td("俱乐部等级分"),
            td(input(tpe := "number", st.id := "member_minScore", min := 0, max := 2800, value := 0)),
            td("至"),
            td(input(tpe := "number", st.id := "member_maxScore", min := 0, max := 2800, value := 2800))
          ),
          tr(
            td("棋协级别"),
            td(mselect("level", lila.user.FormSelect.Level.levelWithRating)),
            td("性别"),
            td(mselect("sex", lila.user.FormSelect.Sex.list))
          ),
          tr(
            td,
            td(colspan := 3)(
              a(cls := "button small search")("查询")
            )
          )
        )
      ),
      postForm(cls := "form3", action := routes.OffContest.playerChoose(contest.id))(
        form3.hidden("players", players.map(_.userId).mkString(",")),
        div(cls := "transfer")(
          div(cls := "transfer-panel left")(
            div(cls := "transfer-panel-head")(s"备选(${allPlayers.size})"),
            div(cls := "transfer-panel-list")(
              table(cls := "transfer-table")(
                tbody(
                  allPlayers.map { pwu =>
                    tr(
                      td(input(tpe := "checkbox", id := s"chk_${pwu.userId}", value := pwu.userId, dataAttr := pwu.json.toString())),
                      td(cls := "name")(pwu.realName | pwu.user.username, s"（${pwu.user.username}）"),
                      td(0)
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
            div(cls := "transfer-panel-head")(s"已选(${players.size})"),
            div(cls := "transfer-panel-list")(
              table(cls := "transfer-table")(
                tbody(
                  players.map { pwu =>
                    tr(
                      td(input(tpe := "checkbox", id := s"chk_${pwu.userId}", name := "player", value := pwu.userId, dataAttr := pwu.json.toString())),
                      td(cls := "name")(pwu.realName | pwu.user.username, s"（${pwu.user.username}）"),
                      td(0)
                    )
                  }
                )
              )
            )
          )
        ),
        form3.actions(
          a(cls := "cancel small")("取消"),
          form3.submit("保存", klass = "small")
        )
      )
    )
  )

  def externalPlayer(c: OffContest)(implicit ctx: Context) = frag(
    div(cls := "modal-content player-external none")(
      h2("临时棋手"),
      postForm(cls := "form3", action := routes.OffContest.externalPlayer(c.id))(
        //form3.group(form("externals"), raw("棋手姓名"), help = raw("每行一个，可以添加多个临时棋手").some)(form3.textarea(_)()),
        table(
          tr(
            th("姓名（必填）："),
            td(input(st.name := "username")(minlength := 2, maxlength := 20))
          ),
          tr(
            th("等级分（选填）："),
            td(input(tpe := "number", st.name := "teamRating", min := 0, max := 2800, value := 0))
          )
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("保存", klass = "small")
        )
      )
    )
  )

  def playerForbidden(contest: OffContest, players: List[OffPlayer.PlayerWithUser], forbidden: Option[OffForbidden])(implicit ctx: Context) = frag(
    div(cls := "modal-content contest-forbidden none")(
      h2("回避设置"),
      postForm(cls := "form3", action := routes.OffContest.forbiddenApply(contest.id, forbidden.map(_.id)))(
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
                      td(cls := "name")(pwu.realNameOrUsername, if (pwu.player.external) "" else s"（${pwu.user.username}）")
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
                      td(cls := "name")(pwu.realNameOrUsername, if (pwu.player.external) "" else s"（${pwu.user.username}）")
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

  def manualAbsent(contest: OffContest, round: OffRound, players: List[OffPlayer.PlayerWithUser])(implicit ctx: Context) = frag(
    div(cls := "modal-content contest-absent none")(
      h2("弃权设置"),
      postForm(cls := "form3", action := routes.OffContest.manualAbsent(contest.id, round.no))(
        div(cls := "transfer")(
          div(cls := "transfer-panel left")(
            div(cls := "transfer-panel-head")("参赛棋手"),
            div(cls := "transfer-panel-search")(
              input(tpe := "text", cls := "transfer-search", placeholder := "搜索")
            ),
            div(cls := "transfer-panel-list")(
              form3.hidden("joins", players.filterNot(_.player.absent).map(_.playerId).mkString(",")),
              table(cls := "transfer-table")(
                tbody(
                  players.filterNot(_.player.absent).map { pwu =>
                    tr(
                      td(input(tpe := "checkbox", id := s"chk_${pwu.userId}", value := pwu.playerId, dataAttr := pwu.json.toString())),
                      td(b(pwu.no)),
                      td(cls := "name")(pwu.realNameOrUsername, if (pwu.player.external) "" else s"（${pwu.user.username}）")
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
            div(cls := "transfer-panel-head")("弃权棋手"),
            div(cls := "transfer-panel-search")(
              input(tpe := "text", cls := "transfer-search", placeholder := "搜索")
            ),
            div(cls := "transfer-panel-list")(
              form3.hidden("absents", players.filter(pwu => pwu.player.manualAbsent && !pwu.player.absentOr).map(_.playerId).mkString(",")),
              table(cls := "transfer-table")(
                tbody(
                  players.filter(pwu => pwu.player.manualAbsent && !pwu.player.absentOr).map { pwu =>
                    tr(
                      td(input(tpe := "checkbox", id := s"chk_${pwu.userId}", value := pwu.playerId, dataAttr := pwu.json.toString())),
                      td(b(pwu.no)),
                      td(cls := "name")(pwu.realNameOrUsername, if (pwu.player.external) "" else s"（${pwu.user.username}）")
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

  def manualPairing(c: OffContest, r: OffRound, boards: List[OffBoard], players: List[OffPlayer.PlayerWithUser], source: OffManualPairingSource)(implicit ctx: Context) = frag(
    div(cls := "modal-content manual-pairing none")(
      h2("与...交换"),
      postForm(cls := "form3", action := routes.OffContest.manualPairing(c.id, r.no))(
        p(cls := "is-gold", dataIcon := "")("手动调整对阵可能会影响后续匹配，请谨慎操作！"),
        form3.hidden("contestId", c.id),
        !source.isBye option form3.hidden("source", s"""{"isBye":0, "board": "${source.board_.id}", "color": ${source.color_.fold(1, 0)}}"""),
        source.isBye option form3.hidden("source", s"""{"isBye":1, "player": "${source.player_.id}"}"""),
        div(cls := "manual-source")(
          table(cls := "slist")(
            tbody(
              !source.isBye option {
                val player = findPlayer(source.board_.player(source.color_).no, players)
                tr(
                  td(source.board_.no),
                  td("#", player.no),
                  td(player.realNameOrUsername),
                  td(source.color_.fold("白方", "黑方"))
                )
              },
              source.isBye option {
                val player = findPlayer(source.player_.no, players)
                tr(
                  td("-"),
                  td("-"),
                  td(player.realNameOrUsername),
                  td("-")
                )
              }
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
                  val white = findPlayer(board.whitePlayer.no, players)
                  val black = findPlayer(board.blackPlayer.no, players)
                  tr(
                    td(board.no),
                    td(cls := List("white" -> true, "disabled" -> (source.board.??(_.is(board)) && source.color ?? (_.name == "white"))))(
                      label(`for` := white.playerId, cls := "user-label")("#", white.no, nbsp, white.realNameOrUsername),
                      nbsp,
                      input(tpe := "radio", id := white.playerId, name := "user-radio", value := s"""{"isBye": 0, "board": "${board.id}", "color": 1}""", (source.board.??(_.is(board)) && source.color ?? (_.name == "white")) option disabled)
                    ),
                    td(cls := List("black" -> true, "disabled" -> (source.board.??(_.is(board)) && source.color ?? (_.name == "black"))))(
                      label(`for` := black.playerId)("#", black.no, nbsp, black.realNameOrUsername),
                      nbsp,
                      input(tpe := "radio", id := black.playerId, name := "user-radio", value := s"""{"isBye": 0, "board": "${board.id}", "color": 0}""", (source.board.??(_.is(board)) && source.color ?? (_.name == "black")) option disabled)
                    )
                  )
                }
              },
              players.filter(_.player.isBye(r.no)).map { pwu =>
                tr(title := "轮空")(
                  td("-"),
                  td(cls := List("white" -> true, "disabled" -> source.isBye))(
                    label(`for` := pwu.playerId, cls := "user-label")("#", pwu.no, nbsp, pwu.realNameOrUsername),
                    nbsp,
                    input(tpe := "radio", id := pwu.playerId, name := "user-radio", value := s"""{"isBye": 1, "player": "${pwu.playerId}"}""", source.isBye option disabled)
                  ),
                  td(cls := List("black" -> true))("-")
                )
              }
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

  private def findPlayer(no: OffPlayer.No, players: List[OffPlayer.PlayerWithUser]): OffPlayer.PlayerWithUser =
    players.find(_.player.no == no) err s"can not find player：$no"

  def manualResult(c: OffContest, r: OffRound, b: OffBoard)(implicit ctx: Context) = frag(
    div(cls := "modal-content manual-result none")(
      h2("设置成绩"),
      postForm(cls := "form3", action := routes.OffContest.manualResult(c.id, b.id))(
        select(name := "result")(
          option(value := "1")("1-0（白方胜）"),
          option(value := "-")("1/2-1/2（平局）"),
          option(value := "0")("0-1（黑方胜）")
        ),
        form3.actions(
          a(cls := "cancel")("取消"),
          form3.submit("保存", klass = "small")
        )
      )
    )
  )

  private def mselect(name: String, choice: List[(String, String)]) = {
    select(st.id := s"member_$name")(
      ("", "") +: choice map {
        case (value, name) => option(st.value := value)(name)
      }
    )
  }

}
