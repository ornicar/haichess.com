package views.html.contest

import play.api.data.Form
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.contest.{ Condition, Contest, DataForm }
import lila.hub.lightClazz._
import play.api.libs.json._
import controllers.routes

object form {

  val dataTeamInner = attr("data-team-inner")
  val dataClazzInner = attr("data-clazz-inner")
  val dataPublicPublic = attr("data-public")

  def create(form: Form[_], teams: List[lila.team.Team], clazzs: ClazzIdsWithNames)(implicit ctx: Context) =
    layout(
      form,
      teams,
      clazzs,
      "创建比赛",
      routes.Contest.create
    )

  def update(id: Contest.ID, form: Form[_], teams: List[lila.team.Team], clazzs: ClazzIdsWithNames)(implicit ctx: Context) =
    layout(
      form,
      teams,
      clazzs,
      "编辑比赛",
      routes.Contest.update(id),
      id.some
    )

  def clone(form: Form[_], teams: List[lila.team.Team], clazzs: ClazzIdsWithNames)(implicit ctx: Context) =
    layout(
      form,
      teams,
      clazzs,
      "复制比赛",
      routes.Contest.create
    )

  private val dataTab = attr("data-tab")
  private def errorTabActive(form: Form[_], tabName: String): Boolean =
    if (form.errors.isEmpty) false
    else form.errors.head.key.startsWith(tabName)

  private def layout(form: Form[_], teams: List[lila.team.Team], clazzs: ClazzIdsWithNames, title: String, url: play.api.mvc.Call, id: Option[Contest.ID] = None)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = cssTag("contest.form"),
    moreJs = frag(
      flatpickrTag,
      singleUploaderTag,
      singleFileUploaderTag,
      jsTag("contest.form.js")
    )
  )(main(cls := "page-small")(
      div(cls := "contest__form box box-pad", dataPublicPublic := JsArray(teams.filter(_.certified).map(t => Json.obj("id" -> t.id, "name" -> t.name))).toString, dataTeamInner := JsArray(teams.map(t => Json.obj("id" -> t.id, "name" -> t.name))).toString, dataClazzInner := JsArray(clazzs.map(t => Json.obj("id" -> t._1, "name" -> t._2))).toString)(
        h1(title),
        postForm(cls := "form3", action := url)(
          div(cls := "tabs")(
            div(dataTab := "basics", cls := List("active" -> (form.errors.isEmpty || form.errors.headOption ?? (_.key.isEmpty) || errorTabActive(form, "basics"))))("1.基本信息"),
            div(dataTab := "rounds", cls := List("active" -> errorTabActive(form, "rounds")))("2.轮次配置"),
            div(dataTab := "conditions", cls := List("active" -> errorTabActive(form, "conditions")))("3.报名配置"),
            div(dataTab := "others", cls := List("active" -> errorTabActive(form, "others")))("4.其他")
          ),
          div(cls := "panels")(
            div(cls := List("panel basics" -> true, "active" -> (form.errors.isEmpty || form.errors.headOption ?? (_.key.isEmpty) || errorTabActive(form, "basics"))))(
              div(cls := "top")(
                form3.group(form("basics.logo"), raw("Logo"), klass = "logo")(form3.singleImage(_, "上传LOGO"))
              ),
              form3.split(
                form3.group(form("basics.name"), raw("比赛名称"), half = true)(form3.input(_)),
                form3.group(form("basics.groupName"), raw("组别（可选）"), half = true)(form3.input(_))
              ),
              form3.split(
                {
                  def available = ctx.me.?? { user =>
                    List(
                      Contest.Type.Public -> (isGranted(_.Team, user) || isGranted(_.ManageContest, user)),
                      Contest.Type.TeamInner -> (user.hasTeam || isGranted(_.ManageContest, user)),
                      Contest.Type.ClazzInner -> (isGranted(_.Coach, user) || isGranted(_.ManageContest, user))
                    ).filter(_._2).map { t => (t._1.id -> t._1.name) }
                  }
                  val baseField = form("basics.typ")
                  val field = ctx.req.queryString get "team" flatMap (_.headOption) match {
                    case None => baseField
                    case Some(team) => baseField.copy(value = "team-inner".some)
                  }
                  form3.group(field, raw("比赛类型"), half = true)(form3.select(_, available))
                },
                form3.group(form("basics.organizer"), raw("主办方"), half = true)(form3.select(_, teams.map(t => (t.id -> t.name))))
              ),
              form3.split(
                form3.checkbox(form("basics.rated"), raw("有积分"), half = true, help = raw("对局将会计分<br>并影响棋手的积分").some),
                form3.group(form("basics.variant"), raw("比赛项目"), half = true)(form3.select(_, translatedVariantChoices.map(x => x._1 -> x._2)))
              ),
              form3.group(form("basics.position"), raw("起始位置"), klass = "starts-position") { field =>
                val fieldVal = field.value
                val url = fieldVal.fold(routes.Editor.index)(f => routes.Editor.load(f)).url
                frag(
                  div(cls := "group-child")(
                    st.select(st.id := form3.id(field), name := field.name, cls := "form-control")(
                      option(value := chess.StartingPosition.initial.fen, fieldVal.has(chess.StartingPosition.initial.fen) option selected)(chess.StartingPosition.initial.name),
                      option(value := fieldVal, fieldVal.??(f => !chess.StartingPosition.allWithInitial.exists(_.fen == f)) option selected, dataId := "option-load-fen")("载入局面"),
                      chess.StartingPosition.categories.map { categ =>
                        optgroup(attr("label") := categ.name)(
                          categ.positions.map { v =>
                            option(value := v.fen, fieldVal.has(v.fen) option selected)(v.fullName)
                          }
                        )
                      }
                    )
                  ),
                  div(cls := "group-child")(
                    input(
                      cls := List("form-control position-paste" -> true, "none" -> fieldVal.??(f => chess.StartingPosition.allWithInitial.exists(_.fen == f))),
                      placeholder := "在此处粘贴FEN棋谱", value := fieldVal
                    )
                  ),
                  div(cls := "group-child")(
                    a(cls := List("board-link" -> true, "none" -> fieldVal.has(chess.StartingPosition.initial.fen)), target := "_blank", href := url)(
                      div(cls := "preview")(
                        fieldVal.map { f =>
                          (chess.format.Forsyth << f).map { situation =>
                            div(
                              cls := "mini-board cg-wrap parse-fen is2d",
                              dataColor := situation.color.name,
                              dataFen := f
                            )(cgWrapContent)
                          }
                        }
                      )
                    )
                  )
                )
              },
              form3.group(form("basics.rule"), raw("赛制"), half = true)(form3.select(_, Contest.Rule.list)),
              form3.split(
                form3.group(form("basics.clockTime"), raw("初始时间"), half = true)(form3.select(_, DataForm.clockTimeChoices)),
                form3.group(form("basics.clockIncrement"), raw("时间增量"), half = true)(form3.select(_, DataForm.clockIncrementChoices))
              ),
              form3.split(
                form3.group(form("basics.startsAt"), raw("比赛开始时间"), half = true)(form3.flatpickr(_)),
                form3.group(form("basics.finishAt"), raw("比赛结束时间"), half = true)(form3.flatpickr(_))
              )
            ),
            div(cls := List("panel rounds" -> true, "active" -> errorTabActive(form, "rounds")))(
              form3.split(
                form3.group(form("#"), raw("轮次间隔"), half = true)(_ => div(cls := "round-space")(
                  form3.groupNoLabel(form("rounds.spaceDay"))(form3.select(_, DataForm.roundSpaceDayChoices)),
                  form3.groupNoLabel(form("rounds.spaceHour"))(form3.select(_, DataForm.roundSpaceHourChoices)),
                  form3.groupNoLabel(form("rounds.spaceMinute"))(form3.select(_, DataForm.roundSpaceMinuteChoices))
                ))
              ),
              form3.split(
                form3.group(form("rounds.appt"), raw("自由约棋"), half = true)(form3.select(_, DataForm.booleanChoices)),
                form3.group(form("rounds.apptDeadline"), raw("约棋截止时间"), half = true, klass = "none")(form3.select(_, DataForm.apptDeadlineMinuteChoices))
              ),
              form3.split(
                form3.group(form("rounds.rounds"), raw("轮次"), half = true)(form3.input(_, typ = "number")),
                div(cls := "form-group form-half")(
                  label(cls := "form-label")(nbsp),
                  div(cls := "form-control")(
                    a(cls := "button button-generate")("生成轮次时间")
                  )
                )
              ),
              div(
                label(cls := "form-label")("轮次开始时间"),
                div(cls := "round-generate")(
                  (0 to 15) map { i =>
                    if (form(s"rounds.list[$i].startsAt").value.isDefined) {
                      form3.split(
                        form3.group(form(s"rounds.list[$i].startsAt"), raw(s"第 ${i + 1} 轮"), half = true)(form3.flatpickr(_))
                      )
                    } else frag()
                  }
                )
              )
            ),
            div(cls := List("panel conditions" -> true, "active" -> errorTabActive(form, "conditions")))(
              form3.hidden(form("conditions.all.teamMember.teamId")),
              form3.hidden(form("conditions.all.clazzMember.clazzId")),
              form3.group(form("conditions.deadline"), raw("报名截止时间"))(form3.select(_, DataForm.deadlineMinuteChoices)),
              form3.split(
                form3.group(form("conditions.enterCost"), raw("报名费（元）"), half = true)(form3.input(_, typ = "number")),
                form3.group(form("conditions.enterApprove"), raw("报名是否审核"), half = true)(form3.select(_, DataForm.booleanChoices))
              ),
              form3.split(
                form3.group(form("conditions.all.minLevel"), raw("最小级别"), half = true)(form3.select(_, Condition.DataForm.levelChoices)),
                form3.group(form("conditions.all.maxLevel"), raw("最大级别"), half = true)(form3.select(_, Condition.DataForm.levelChoices))
              ),
              form3.split(
                form3.group(form("conditions.all.minRating.rating"), raw("最小等级分"), half = true)(form3.select(_, Condition.DataForm.maxRatingChoices)),
                form3.group(form("conditions.all.maxRating.rating"), raw("最大等级分"), half = true)(form3.select(_, Condition.DataForm.minRatingChoices))
              ),
              form3.split(
                form3.group(form("conditions.minPlayers"), raw("人数下限"), half = true, help = frag("报名人数少于报名下限将自动取消比赛").some)(form3.input(_, typ = "number")),
                form3.group(form("conditions.maxPlayers"), raw("人数上限"), half = true)(form3.input(_, typ = "number"))
              ),
              form3.split(
                form3.group(form("conditions.all.minAge"), raw("最小年龄"), half = true)(form3.input(_, typ = "number")),
                form3.group(form("conditions.all.maxAge"), raw("最大年龄"), half = true)(form3.input(_, typ = "number"))
              ),
              form3.group(form("conditions.all.sex"), raw("性别"))(form3.select(_, Condition.DataForm.sexChoices))
            ),
            div(cls := List("panel others" -> true, "active" -> errorTabActive(form, "others")))(
              div(cls := "form-group")(
                label(cls := "form-label")("破同分规则"),
                div(
                  div(cls := "btss-wrap")(
                    label("瑞士制"),
                    div(cls := "btss-list")(
                      span(cls := "btss")("对手分"),
                      span(cls := "btss")("中间分"),
                      span(cls := "btss")("胜局数")
                    ),
                    form3.hidden("others.swissBtss[0]", "opponent"),
                    form3.hidden("others.swissBtss[1]", "mid"),
                    form3.hidden("others.swissBtss[2]", "vict")
                  ) /*,
                  div(cls := "btss-wrap")(
                    label("循环赛"),
                    div(cls := "btss-list")(
                      span(cls := "btss")("直胜"),
                      span(cls := "btss")("胜局数"),
                      span(cls := "btss")("索伯分")
                    ),
                    form3.hidden("others.roundRobinBtss[0]", "res"),
                    form3.hidden("others.roundRobinBtss[1]", "vict"),
                    form3.hidden("others.roundRobinBtss[2]", "sauber")
                  )*/
                )
              ),
              form3.split(
                form3.group(form("others.hasPrizes"), raw("是否发放奖金"), half = true)(form3.select(_, DataForm.booleanChoices)),
                form3.group(form("others.autoPairing"), raw("自动编排并发布成绩"), half = true)(form3.select(_, DataForm.booleanChoices))
              ),
              form3.split(
                form3.group(form("others.canLateMinute"), raw("允许迟到时间"), half = true)(form3.input(_, typ = "number")),
                form3.group(form("others.canQuitNumber"), raw("缺席退赛场次"), half = true)(form3.input(_, typ = "number"))
              ),
              form3.group(form("others.attachments"), raw("比赛规程文件")) { f =>
                form3.singleFile(f)
              },
              form3.group(form("others.description"), raw("附加说明"), help = frag("系统目前不支持收付款，如果收取报名费或发放奖金，请在附加说明中进行说明").some)(form3.textarea(_)(rows := 6))
            )
          ),
          form3.globalError(form),
          form3.actions(
            a(href := routes.Contest.ownerPage(None, "", 1))(trans.cancel()),
            form3.submit("保存并预览", icon = "g".some)
          )
        )
      )
    ))

}
