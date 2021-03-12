package views.html
package tournament

import play.api.data.{ Field, Form }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import lila.tournament.{ Condition, DataForm }

import controllers.routes

object form {

  def apply(form: Form[_], config: DataForm, me: User, teams: lila.hub.lightTeam.TeamIdsWithNames)(implicit ctx: Context) = views.html.base.layout(
    title = trans.newTournament.txt(),
    moreCss = cssTag("tournament.form"),
    moreJs = frag(
      flatpickrTag,
      jsTag("tournamentForm.js")
    )
  )(main(cls := "page-small")(
      div(cls := "tour__form box box-pad")(
        h1(trans.createANewTournament()),
        postForm(cls := "form3", action := routes.Tournament.create)(
          DataForm.canPickName(me) ?? {
            form3.group(form("name"), trans.name()) { f =>
              div(
                form3.input(f), "赛", br,
                small(cls := "form-help")(
                  trans.safeTournamentName(), br,
                  trans.inappropriateNameWarning(), br,
                  trans.emptyTournamentName(), br
                )
              )
            }
          },
          form3.split(
            form3.checkbox(form("rated"), trans.rated(), help = raw("对局将会计分<br>并影响棋手的积分").some),
            st.input(tpe := "hidden", name := form("rated").name, value := "false"), // hack allow disabling rated
            form3.group(form("variant"), trans.variant(), half = true)(form3.select(_, translatedVariantChoicesWithVariants.map(x => x._1 -> x._2)))
          ),
          form3.group(form("position"), trans.startPosition(), klass = "position")(startingPosition(_)),
          form3.split(
            form3.group(form("clockTime"), raw("初始时间"), half = true)(form3.select(_, DataForm.clockTimeChoices)),
            form3.group(form("clockIncrement"), raw("时间增量"), half = true)(form3.select(_, DataForm.clockIncrementChoices))
          ),
          form3.split(
            form3.group(form("minutes"), trans.duration(), half = true)(form3.select(_, DataForm.minuteChoices)),
            form3.group(form("waitMinutes"), trans.timeBeforeTournamentStarts(), half = true)(form3.select(_, DataForm.waitMinuteChoices))
          ),
          form3.globalError(form),
          fieldset(cls := "conditions")(
            legend(trans.advancedSettings()),
            errMsg(form("conditions")),
            p(
              strong(dataIcon := "!", cls := "text")(trans.recommendNotTouching()),
              " ",
              trans.fewerPlayers(),
              " ",
              a(cls := "show")(trans.showAdvancedSettings())
            ),
            div(cls := "form")(
              form3.group(form("password"), trans.password(), help = raw("将比赛设为私人比赛，并使用密码限制访问").some)(form3.input(_)),
              condition(form, auto = true, teams = teams),
              input(tpe := "hidden", name := form("berserkable").name, value := "false"), // hack allow disabling berserk
              form3.group(form("startDate"), raw("自定义开始日期"), help = raw("""这优先于“比赛开始前的时间”设置""").some)(form3.flatpickr(_))
            )
          ),
          form3.actions(
            a(href := routes.Tournament.home())(trans.cancel()),
            form3.submit(trans.createANewTournament(), icon = "g".some)
          )
        )
      ),
      div(cls := "box box-pad tour__faq")(tournament.faq())
    ))

  private def autoField(auto: Boolean, field: Field)(visible: Field => Frag) = frag(
    if (auto) form3.hidden(field) else visible(field)
  )

  def condition(form: Form[_], auto: Boolean, teams: lila.hub.lightTeam.TeamIdsWithNames)(implicit ctx: Context) = frag(
    form3.split(
      form3.group(form("conditions.nbRatedGame.nb"), raw("最少计分对局"), half = true)(form3.select(_, Condition.DataForm.nbRatedGameChoices)),
      autoField(auto, form("conditions.nbRatedGame.perf")) { field =>
        form3.group(field, raw("In variant"), half = true)(form3.select(_, ("", "Any") :: Condition.DataForm.perfChoices))
      }
    ),
    form3.split(
      form3.group(form("conditions.minRating.rating"), raw("最小积分"), half = true)(form3.select(_, Condition.DataForm.minRatingChoices)),
      autoField(auto, form("conditions.minRating.perf")) { field =>
        form3.group(field, raw("In variant"), half = true)(form3.select(_, Condition.DataForm.perfChoices))
      }
    ),
    form3.split(
      form3.group(form("conditions.maxRating.rating"), raw("最大积分"), half = true)(form3.select(_, Condition.DataForm.maxRatingChoices)),
      autoField(auto, form("conditions.maxRating.perf")) { field =>
        form3.group(field, raw("In variant"), half = true)(form3.select(_, Condition.DataForm.perfChoices))
      }
    ),
    form3.split(
      (ctx.me.exists(_.hasTitle) || isGranted(_.ManageTournament)) ?? {
        form3.checkbox(form("conditions.titled"), raw("仅称号棋手"), help = raw("需要官方头衔才能加入比赛").some, half = true)
      },
      form3.checkbox(form("berserkable"), raw("允许加快棋速"), help = raw("让棋手将他们的时钟时间减半以获得额外的积分").some, half = true)
    ),
    (auto && teams.size > 0) ?? {
      val baseField = form("conditions.teamMember.teamId")
      val field = ctx.req.queryString get "team" flatMap (_.headOption) match {
        case None => baseField
        case Some(team) => baseField.copy(value = team.some)
      }
      form3.group(field, raw("仅俱乐部成员"), half = false)(form3.select(_, List(("", "无限制")) ::: teams))
    }
  )

  def startingPosition(field: Field)(implicit ctx: Context) = st.select(
    id := form3.id(field),
    name := field.name,
    cls := "form-control"
  )(
      option(
        value := chess.StartingPosition.initial.fen,
        field.value.has(chess.StartingPosition.initial.fen) option selected
      )(chess.StartingPosition.initial.name),
      chess.StartingPosition.categories.map { categ =>
        optgroup(attr("label") := categ.name)(
          categ.positions.map { v =>
            option(value := v.fen, field.value.has(v.fen) option selected)(v.fullName)
          }
        )
      }
    )
}
