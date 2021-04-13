package controllers

import lila.app._
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import lila.puzzle.{ PuzzleId, PuzzleRushCustomForm, PuzzleRound => PuzzleRoundMode, PuzzleRush => PuzzleRushMode }
import lila.api.Context
import lila.user.UserRepo
import org.joda.time.DateTime
import play.api.mvc._
import views._

object PuzzleRush extends LilaController {

  private def env = Env.puzzle
  private def member = Env.member.memberActiveRecordApi

  def page(page: Int, m: String, o: String) = Auth { implicit ctx => me =>
    val mode = PuzzleRushMode.Mode(m)
    val order = PuzzleRushMode.Order(o)
    env.puzzleRushApi.page(me.id, page, mode, order) map { pager =>
      Ok(views.html.puzzle.rush.list(pager, mode, order))
    }
  }

  def show = Auth { implicit ctx => me =>
    NoBot {
      member.isPuzzleRushContinue(me) flatMap { accept =>
        val mode = get("mode").map(PuzzleRushMode.Mode(_))
        val auto = get("auto").??(_.toBoolean)
        env.puzzleRushApi.startedList(me) map { rushs =>
          Ok(views.html.puzzle.rush.show(
            user = Env.user.jsonView.minimal(me, None),
            pref = env.jsonView.pref(ctx.pref),
            threeMinutesMode = rushs.find(_.mode == PuzzleRushMode.Mode.ThreeMinutes),
            fiveMinutesMode = rushs.find(_.mode == PuzzleRushMode.Mode.FiveMinutes),
            survivalMode = rushs.find(_.mode == PuzzleRushMode.Mode.Survival),
            mode = mode,
            auto = auto,
            notAccept = !accept
          ))
        }
      }
    }
  }

  def showById(rushId: String) = Auth { implicit ctx => me =>
    NoBot {
      OptionFuResult(env.puzzleRushApi.byId(rushId)) { rush =>
        if (rush.mode != PuzzleRushMode.Mode.Survival && rush.outOfTime) {
          env.puzzleRushApi.finish(rush, PuzzleRushMode.Status.Timeout) flatMap { _ =>
            byId(rush, me)
          }
        } else byId(rush, me)
      }
    }
  }

  private def byId(rush: PuzzleRushMode, me: lila.user.User)(implicit ctx: Context): Fu[Result] =
    env.puzzleRoundApi.rushRounds(rush.id, false) flatMap { rounds =>
      roundJson(rounds) map { roundJson =>
        Ok(views.html.puzzle.rush.show(
          user = Env.user.jsonView.minimal(me, None),
          pref = env.jsonView.pref(ctx.pref),
          rush = rushJson(rush.copy(result = PuzzleRushMode.Result(rush, rounds).some)).some,
          rounds = roundJson.some
        ))
      }
    }

  def start(mode: String) = AuthBody { implicit ctx => me =>
    NoBot {
      member.isPuzzleRushContinue(me) flatMap { continue =>
        if (continue) {
          val m = PuzzleRushMode.Mode(mode)
          implicit val req = ctx.body
          PuzzleRushCustomForm.customForm.bindFromRequest.fold(
            jsonFormError,
            data => env.puzzleRushApi.create(
              PuzzleRushMode.make(
                me,
                m,
                if (m == PuzzleRushMode.Mode.Custom) data.some else none
              )
            ) map { pr =>
                Ok(Json.obj(
                  "rushId" -> pr.id
                ))
              }
          ) map (_ as JSON)
        } else {
          fuccess(NotAcceptable("每日试用次数超过上限"))
        }
      }
    }
  }

  def begin(rushId: String) = Auth { implicit ctx => me =>
    NoBot {
      StatusValid(rushId, PuzzleRushMode.Status.Created) { rush =>
        env.puzzleRushApi.begin(rush) inject jsonOkResult
      }
    }
  }

  def finish(rushId: String) = AuthBody { implicit ctx => me =>
    NoBot {
      StatusValid(rushId, PuzzleRushMode.Status.Started) { _ =>
        implicit val req = ctx.body
        Form(single(
          "status" -> number.verifying(PuzzleRushMode.Status.keys contains _)
        )).bindFromRequest.fold(
          jsonFormError,
          s => OptionFuResult(env.puzzleRushApi.byId(rushId)) { rush =>
            env.puzzleRushApi.finish(rush, PuzzleRushMode.Status(s)) map { result =>
              Ok(resultJson(result))
            }
          }
        ) map (_ as JSON)
      }
    }
  }

  def next(rushId: String) = AuthBody { implicit ctx => me =>
    NoBot {
      StatusValid(rushId, PuzzleRushMode.Status.Started) { _ =>
        implicit val req = ctx.body
        PuzzleRushCustomForm.customForm.bindFromRequest.fold(
          jsonFormError,
          data => env.puzzleRushSelector(me, rushId, data) map { p =>
            Ok(env.jsonView.light.make(p))
          }
        ) map (_ as JSON)
      }
    }
  }

  def last(rushId: String) = AuthBody { implicit ctx => me =>
    NoBot {
      StatusValid(rushId, PuzzleRushMode.Status.Started) { _ =>
        implicit val req = ctx.body
        PuzzleRushCustomForm.customForm.bindFromRequest.fold(
          jsonFormError,
          data => env.puzzleRushSelector.last(me, rushId, data) map { p =>
            Ok(env.jsonView.light.make(p))
          }
        ) map (_ as JSON)
      }
    }
  }

  def round(rushId: String, puzzleId: PuzzleId) = AuthBody { implicit ctx => me =>
    NoBot {
      StatusValid(rushId, PuzzleRushMode.Status.Started) { _ =>
        implicit val req = ctx.body
        OptionFuResult(env.api.puzzle find puzzleId) { puzzle =>
          if (puzzle.mate) lila.mon.puzzle.round.mate()
          else lila.mon.puzzle.round.material()
          env.forms.round.bindFromRequest.fold(
            jsonFormError,
            data => env.puzzleRushApi.round(
              rushId = rushId,
              puzzle = puzzle,
              user = me,
              result = lila.puzzle.Result(data.win == 1),
              seconds = data.seconds,
              lines = data.linesWithEmpty,
              timeout = data.timeout,
              mobile = lila.api.Mobile.Api.requested(ctx.req)
            ) inject env.finisher.incPuzzleAttempts(puzzle) inject jsonOkResult
          )
        }
      }
    }
  }

  def rank(mode: String, scope: String, range: String) = Auth { implicit ctx => me =>
    NoBot {
      scope match {
        case "country" => {
          range match {
            case "history" =>
              val date = DateTime.now.toString("yyyyMMdd").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                rankList <- env.puzzleRushRankHistoryApi.rankList(PuzzleRushMode.Mode(mode))
              } yield {
                Ok(rankJson(
                  me,
                  userHisRank,
                  userTdyRank,
                  userHisRank,
                  rankList.map(r => (r.userId, r.win))
                )) as JSON
              }
            case "season" =>
              val now = DateTime.now
              val date = now.toString("yyyyMMdd").toInt
              val season = now.toString("yyyyMM").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                userRank <- env.puzzleRushRankSeasonApi.userRank(PuzzleRushMode.Mode(mode), season, me.id)
                rankList <- env.puzzleRushRankSeasonApi.rankList(PuzzleRushMode.Mode(mode), season)
              } yield {
                Ok(rankJson(
                  me,
                  userHisRank,
                  userTdyRank,
                  userRank,
                  rankList.map(r => (r.userId, r.win))
                )) as JSON
              }
            case "today" =>
              val date = DateTime.now.toString("yyyyMMdd").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                rankList <- env.puzzleRushRankTodayApi.rankList(PuzzleRushMode.Mode(mode), date)
              } yield {
                Ok(rankJson(
                  me,
                  userHisRank,
                  userTdyRank,
                  userTdyRank,
                  rankList.map(r => (r.userId, r.win))
                )) as JSON
              }
          }
        }
        case "level" => {
          range match {
            case "history" =>
              val date = DateTime.now.toString("yyyyMMdd").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                userList <- UserRepo.userIdsSameLevel(me.profileOrDefault.currentLevel.level)
                rankList <- env.puzzleRushRankHistoryApi.rankList(PuzzleRushMode.Mode(mode), userList.some)
              } yield {
                Ok(rankJson(
                  me,
                  userHisRank,
                  userTdyRank,
                  userHisRank,
                  rankList.map(r => (r.userId, r.win))
                )) as JSON
              }
            case "season" =>
              val now = DateTime.now
              val date = now.toString("yyyyMMdd").toInt
              val season = now.toString("yyyyMM").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                userList <- UserRepo.userIdsSameLevel(me.profileOrDefault.currentLevel.level)
                userRank <- env.puzzleRushRankSeasonApi.userRank(PuzzleRushMode.Mode(mode), season, me.id)
                rankList <- env.puzzleRushRankSeasonApi.rankList(PuzzleRushMode.Mode(mode), season, userList.some)
              } yield {
                Ok(rankJson(
                  me,
                  userHisRank,
                  userTdyRank,
                  userRank,
                  rankList.map(r => (r.userId, r.win))
                )) as JSON
              }
            case "today" =>
              val date = DateTime.now.toString("yyyyMMdd").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                userList <- UserRepo.userIdsSameLevel(me.id)
                rankList <- env.puzzleRushRankTodayApi.rankList(PuzzleRushMode.Mode(mode), date, userList.some)
              } yield {
                Ok(rankJson(
                  me,
                  userHisRank,
                  userTdyRank,
                  userTdyRank,
                  rankList.map(r => (r.userId, r.win))
                )) as JSON
              }
          }
        }
        case "friend" => {
          range match {
            case "history" =>
              val date = DateTime.now.toString("yyyyMMdd").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                userSet <- Env.relation.api.fetchFollowing(me.id)
                rankList <- env.puzzleRushRankHistoryApi.rankList(PuzzleRushMode.Mode(mode), (userSet.toList :+ me.id).some)
              } yield {
                Ok(rankJson(
                  me,
                  userHisRank,
                  userTdyRank,
                  userHisRank,
                  rankList.map(r => (r.userId, r.win))
                )) as JSON
              }
            case "season" =>
              val now = DateTime.now
              val date = now.toString("yyyyMMdd").toInt
              val season = now.toString("yyyyMM").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                userSet <- Env.relation.api.fetchFollowing(me.id)
                userRank <- env.puzzleRushRankSeasonApi.userRank(PuzzleRushMode.Mode(mode), season, me.id)
                rankList <- env.puzzleRushRankSeasonApi.rankList(PuzzleRushMode.Mode(mode), season, (userSet.toList :+ me.id).some)
              } yield {
                Ok(rankJson(
                  me,
                  userHisRank,
                  userTdyRank,
                  userRank,
                  rankList.map(r => (r.userId, r.win))
                )) as JSON
              }
            case "today" =>
              val date = DateTime.now.toString("yyyyMMdd").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                userSet <- Env.relation.api.fetchFollowing(me.id)
                rankList <- env.puzzleRushRankTodayApi.rankList(PuzzleRushMode.Mode(mode), date, (userSet.toList :+ me.id).some)
              } yield {
                Ok(rankJson(
                  me,
                  userHisRank,
                  userTdyRank,
                  userTdyRank,
                  rankList.map(r => (r.userId, r.win))
                )) as JSON
              }
          }
        }
        case "personal" => {
          range match {
            case "history" =>
              val date = DateTime.now.toString("yyyyMMdd").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                rankList <- env.puzzleRushApi.rankList(PuzzleRushMode.Mode(mode), me.id)
              } yield {
                Ok(personalRankJson(
                  userHisRank,
                  userTdyRank,
                  rankList.map(r => (r.id, r.endTime.get, r.result.get.win))
                )) as JSON
              }
            case "season" =>
              val now = DateTime.now
              val date = now.toString("yyyyMMdd").toInt
              val season = now.toString("yyyyMM").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                rankList <- env.puzzleRushApi.seasonRankList(PuzzleRushMode.Mode(mode), season, me.id)
              } yield {
                Ok(personalRankJson(
                  userHisRank,
                  userTdyRank,
                  rankList.map(r => (r.id, r.endTime.get, r.result.get.win))
                )) as JSON
              }
            case "today" =>
              val now = DateTime.now
              val date = now.toString("yyyyMMdd").toInt
              for {
                userHisRank <- env.puzzleRushRankHistoryApi.userRank(PuzzleRushMode.Mode(mode), me.id)
                userTdyRank <- env.puzzleRushRankTodayApi.userRank(PuzzleRushMode.Mode(mode), date, me.id)
                rankList <- env.puzzleRushApi.todayRankList(PuzzleRushMode.Mode(mode), now, me.id)
              } yield {
                Ok(personalRankJson(
                  userHisRank,
                  userTdyRank,
                  rankList.map(r => (r.id, r.endTime.get, r.result.get.win))
                )) as JSON
              }
          }
        }
      }
    }
  }

  def BadResult: Fu[Result] = fuccess {
    Forbidden(jsonError(
      s"Forbidden -_-"
    )) as JSON
  }

  private def StatusValid(rushId: String, status: PuzzleRushMode.Status)(f: PuzzleRushMode => Fu[Result])(implicit ctx: Context): Fu[Result] =
    env.puzzleRushApi.byId(rushId) flatMap {
      case None => BadResult
      case Some(r) => if (r.status.is(status) && ctx.me.??(_.id == r.userId)) {
        f(r)
      } else BadResult
    }

  private def rushJson(rush: PuzzleRushMode) = {
    def mode = rush.status match {
      case PuzzleRushMode.Status.Created => "playing"
      case PuzzleRushMode.Status.Started => "playing"
      case _ => "finish"
    }

    Json.obj(
      "id" -> rush.id,
      "mode" -> rush.mode.id,
      "page" -> mode,
      "status" -> rush.status.id,
      "seconds" -> rush.remainingSeconds
    ).add(
        "result" -> rush.result.map { result =>
          resultJson(result)
        }
      )
      .add(
        "condition" -> rush.condition.map { condition =>
          conditionJson(condition)
        }
      )
  }

  private def conditionJson(c: lila.puzzle.CustomCondition) = Json.obj(
    "minutes" -> c.minutes,
    "limit" -> c.limit
  ).add("ratingMin" -> c.ratingMin)
    .add("ratingMax" -> c.ratingMax)
    .add("stepsMin" -> c.stepsMin)
    .add("stepsMax" -> c.stepsMax)
    .add("phase" -> c.phase)
    .add("color" -> c.color)
    .add("selector" -> c.selector)

  private def resultJson(result: PuzzleRushMode.Result) = Json.obj(
    "seconds" -> result.seconds,
    "avgTime" -> result.avgTime,
    "nb" -> result.nb,
    "win" -> result.win,
    "loss" -> result.loss,
    "maxRating" -> result.maxRating,
    "winStreaks" -> result.winStreaks
  )

  private def roundJson(rounds: List[PuzzleRoundMode]): Fu[JsArray] =
    env.api.puzzle.findMany2(rounds.map(_.puzzleId)) map { puzzles =>
      JsArray(
        rounds zip puzzles collect {
          case (round, puzzle) => PuzzleRoundMode.RoundWithPuzzle(round, puzzle).toJson
        }
      )
    }

  private def rankJson(user: lila.user.User, userHisRank: (Int, Int), userTdyRank: (Int, Int), userRank: (Int, Int), rankList: List[(lila.user.User.ID, Int)]) = {
    Json.obj(
      "userHisRank" -> Json.obj(
        "no" -> userHisRank._1,
        "score" -> userHisRank._2
      ),
      "userTdyRank" -> Json.obj(
        "no" -> userTdyRank._1,
        "score" -> userTdyRank._2
      ),
      "userRank" -> Json.obj(
        "no" -> userRank._1,
        "user" -> env.userJsonView.user(user.id),
        "score" -> userRank._2
      ),
      "rankList" -> rankList.zipWithIndex.map {
        case (rank, no) => Json.obj(
          "no" -> (no + 1),
          "user" -> env.userJsonView.user(rank._1),
          "score" -> rank._2
        )
      }
    )
  }

  private def personalRankJson(userHisRank: (Int, Int), userTdyRank: (Int, Int), rankList: List[(PuzzleRushMode.ID, DateTime, Int)]) = {
    Json.obj(
      "userHisRank" -> Json.obj(
        "no" -> userHisRank._1,
        "score" -> userHisRank._2
      ),
      "userTdyRank" -> Json.obj(
        "no" -> userTdyRank._1,
        "score" -> userTdyRank._2
      ),
      "rankList" -> rankList.zipWithIndex.map {
        case (rank, no) => Json.obj(
          "id" -> rank._1,
          "no" -> (no + 1),
          "time" -> rank._2.toString("yyyy/MM/dd HH:mm"),
          "score" -> rank._3
        )
      }
    )
  }

}
