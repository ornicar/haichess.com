package controllers

import play.api.libs.json._
import play.api.mvc._
import lila.api.Context
import lila.app._
import lila.common.{ HTTPRequest, MaxPerSecond }
import lila.puzzle.{ LightCapsule, LightHomework, PuzzleErrors, PuzzleId, Result, ThemeShow, UserInfos, Puzzle => PuzzleModel }
import lila.user.UserRepo
import views._

object Puzzle extends LilaController {

  private def env = Env.puzzle
  private def member = Env.member.memberActiveRecordApi

  private def renderJson(
    puzzle: PuzzleModel,
    userInfos: Option[UserInfos],
    mode: String,
    themeShow: Option[ThemeShow] = None,
    puzzleErrors: Option[PuzzleErrors] = None,
    capsule: Option[LightCapsule] = None,
    homework: Option[LightHomework] = None,
    showNextPuzzle: Boolean = true,
    rated: Boolean = true,
    voted: Option[Boolean],
    round: Option[lila.puzzle.Round] = None,
    result: Option[Result] = None
  )(implicit ctx: Context): Fu[JsObject] = env.jsonView(
    puzzle = puzzle,
    userInfos = userInfos,
    round = round,
    mode = mode,
    themeShow = themeShow,
    puzzleErrors = puzzleErrors,
    capsule = capsule,
    homework = homework,
    showNextPuzzle = showNextPuzzle,
    rated = rated,
    mobileApi = ctx.mobileApiVersion,
    result = result,
    voted = voted,
    puzzleApi = env.api
  )

  private def puzzleJson(puzzle: PuzzleModel)(implicit ctx: Context) =
    puzzleJson2(puzzle)

  private def puzzleJson2(
    puzzle: PuzzleModel,
    themeShow: Option[ThemeShow] = None,
    puzzleErrors: Option[PuzzleErrors] = None,
    capsule: Option[LightCapsule] = None,
    homework: Option[LightHomework] = None,
    rated: Boolean = true
  )(implicit ctx: Context) =
    env userInfos ctx.me flatMap { infos =>
      renderJson(
        puzzle = puzzle,
        userInfos = infos,
        mode = if (ctx.isAuth) "play" else "try",
        themeShow = themeShow,
        puzzleErrors = puzzleErrors,
        capsule = capsule,
        homework = homework,
        rated = rated,
        voted = none
      )
    }

  private def renderShow(
    puzzle: PuzzleModel,
    mode: String,
    showNextPuzzle: Boolean = true,
    rated: Boolean = true,
    themeShow: Option[ThemeShow] = None,
    puzzleErrors: Option[PuzzleErrors] = None,
    capsule: Option[LightCapsule] = None,
    homework: Option[LightHomework] = None,
    notAccept: Boolean = false
  )(implicit ctx: Context) =
    env userInfos ctx.me flatMap { infos =>
      renderJson(
        puzzle = puzzle,
        userInfos = infos,
        mode = mode,
        themeShow = themeShow,
        puzzleErrors = puzzleErrors,
        capsule = capsule,
        homework = homework,
        showNextPuzzle = showNextPuzzle,
        rated = rated,
        voted = none
      ) map { json =>
        views.html.puzzle.show(puzzle, data = json, pref = env.jsonView.pref(ctx.pref), themeShow, notAccept)
      }
    }

  def daily = Open { implicit ctx =>
    NoBot {
      OptionFuResult(env.daily.get flatMap {
        _.map(_.id) ?? env.api.puzzle.find
      }) { puzzle =>
        negotiate(
          html = renderShow(puzzle, "play") map { Ok(_) },
          api = _ => puzzleJson(puzzle) map { Ok(_) }
        ) map { NoCache(_) }
      }
    }
  }

  def home = Auth { implicit ctx => me =>
    NoBot {
      member.isPuzzleContinue(me) flatMap { continue =>
        if (continue) {
          env.selector.nextPuzzle(ctx.me) flatMap { puzzle =>
            renderShow(puzzle = puzzle, mode = if (ctx.isAuth) "play" else "try") map { Ok(_) }
          }
        } else {
          renderShow(puzzle = PuzzleModel.default, mode = if (ctx.isAuth) "play" else "try", notAccept = true) map { Ok(_) }
        }
      }
    }
  }

  def show(id: PuzzleId, showNextPuzzle: Boolean, rated: Boolean) = Open { implicit ctx =>
    NoBot {
      if (id == 0) {
        renderShow(PuzzleModel.default, mode = "play", showNextPuzzle = showNextPuzzle, rated = rated) map { Ok(_) }
      } else {
        OptionFuOk(env.api.puzzle find id) { puzzle =>
          renderShow(puzzle = puzzle, mode = "play", showNextPuzzle = showNextPuzzle, rated = rated)
        }
      }
    }
  }

  def themePuzzleHome = AuthBody { implicit ctx => me =>
    env.puzzleThemeRecord.lastId(me.id) map { last =>
      Redirect(routes.Puzzle.themePuzzle(last, true))
    }
  }

  def themePuzzle(id: PuzzleId, showDrawer: Boolean, next: Boolean = false) = AuthBody { implicit ctx => me =>
    val searchForm = Env.resource.forms.puzzle.theme
    member.isThemePuzzleContinue(me) flatMap { continue =>
      if (continue) {
        implicit val req = ctx.body
        searchForm.bindFromRequest.fold(
          _ => fuccess(BadRequest),
          data => {
            for {
              tags <- env.resource.themeTags
              prevPuzzle <- env.api.puzzle find id
              puzzle <- next.?? { env.selector.nextThemePuzzle(me, env.resource.themeSearchCondition(data), id) }
              history <- env.puzzleThemeRecord.byId(me.id)
              res <- {
                val pz = if (next) puzzle else prevPuzzle
                pz.fold {
                  prevPuzzle.fold(notFound) { p =>
                    renderShow(puzzle = p, mode = "play", rated = false, themeShow = ThemeShow(id, searchForm fill data, tags, true, history, true)) map {
                      Ok(_)
                    }
                  }
                } { p =>
                  renderShow(puzzle = p, mode = "play", rated = false, themeShow = ThemeShow(p.id, searchForm fill data, tags, false, history, showDrawer)) map {
                    Ok(_)
                  }
                }
              }
            } yield res
          }
        )
      } else {
        renderShow(puzzle = PuzzleModel.default, mode = "play", rated = false, themeShow = ThemeShow(0, searchForm, Set.empty, true, none, true), notAccept = true) map {
          Ok(_)
        }
      }
    }
  }

  def newThemePuzzle(id: PuzzleId) = AuthBody { implicit ctx => me =>
    XhrOnly {
      member.isThemePuzzleContinue(me) flatMap { continue =>
        if (continue) {
          implicit val req = ctx.body
          val searchForm = Env.resource.forms.puzzle.theme
          searchForm.bindFromRequest.fold(
            _ => fuccess(BadRequest),
            data => env.selector.nextThemePuzzle(me, env.resource.themeSearchCondition(data, id.some), id) flatMap {
              case None => notFoundJson("Resource not found")
              case Some(p) => puzzleJson2(p, ThemeShow(id = p.id), rated = false) map { json =>
                Ok(json) as JSON
              }
            }
          )
        } else fuccess(NotAcceptable("每日试用次数超过上限"))
      }
    }
  }

  /*  def themePuzzleHistoryUri(id: String) = Auth { implicit ctx => me =>
    OptionResult(env.puzzleThemeRecord.byId(id)) { record =>
      Ok(Json.obj(
        "minId" -> env.api.pmim,
        "lastId" -> record.puzzleId,
        "uri" -> record.queryString
      ))
    }
  }

  def themePuzzleHistoryRemove(id: String) = Auth { implicit ctx => me =>
    env.puzzleThemeRecord.remove(id, me.id) map (_ => jsonOkResult)
  }*/

  def errorPuzzle(id: PuzzleId) = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    Env.errors.forms.puzzle.bindFromRequest.fold(
      fail => {
        notFound
      },
      data => env.selector.byId(id) flatMap {
        case None => notFound
        case Some(p) => {
          renderShow(puzzle = p, mode = "play", rated = false, puzzleErrors = PuzzleErrors.make(data.rating, data.time)) map {
            Ok(_)
          }
        }
      }
    )
  }

  def newErrorPuzzle(id: PuzzleId) = AuthBody { implicit ctx => me =>
    XhrOnly {
      implicit val req = ctx.body
      Env.errors.forms.puzzle.bindFromRequest.fold(
        fail => {
          fuccess(BadRequest)
        },
        data => get("d").?? { d =>
          if (!d.toBoolean) funit
          else Env.errors.puzzleErrorsApi.removeById(id, me.id)
        } >> Env.errors.puzzleErrorsApi.nextPuzzleErrors(id, me.id, data) flatMap {
          case None => notFoundJson("Resource not found")
          case Some(e) => env.selector.byId(e.puzzleId) flatMap {
            case None => notFound
            case Some(p) => {
              puzzleJson2(puzzle = p, rated = false, puzzleErrors = PuzzleErrors(e.rating, e.createAt).some) map { json =>
                Ok(json) as JSON
              }
            }
          }
        }
      )
    }
  }

  def capsulePuzzle(capsuleId: String) = Auth { implicit ctx => me =>
    Env.resource.capsuleApi.byId(capsuleId).flatMap {
      case None => notFound
      case Some(capsule) => env.selector.nextCapsulePuzzle(None, capsule.puzzlesWithoutRemove.toList).flatMap {
        case None => notFound
        case Some(p) => {
          renderShow(puzzle = p, mode = "play", rated = false, capsule = LightCapsule(capsule.id, capsule.name).some) map {
            Ok(_)
          }
        }
      }
    }
  }

  def newCapsulePuzzle(capsuleId: String, lastPlayed: Option[PuzzleId]) = AuthBody { implicit ctx => me =>
    XhrOnly {
      Env.resource.capsuleApi.byId(capsuleId).flatMap {
        case None => notFoundJson("Resource not found")
        case Some(capsule) => env.selector.nextCapsulePuzzle(lastPlayed, capsule.puzzlesWithoutRemove.toList).flatMap {
          case None => notFoundJson("Resource not found")
          case Some(p) =>
            puzzleJson2(
              puzzle = p, capsule = LightCapsule(capsule.id, capsule.name).some, rated = false
            ) map { json =>
              Ok(json) as JSON
            }
        }
      }
    }
  }

  def homeworkPuzzle(homeworkId: String, startsAt: PuzzleId) = Auth { implicit ctx => me =>
    Env.clazz.homeworkStudentApi.info(homeworkId).flatMap {
      case None => notFound
      case Some(info) => env.selector.currentHomeworkPuzzle(startsAt).flatMap {
        case None => notFound
        case Some(p) => {
          renderShow(
            puzzle = p,
            mode = "play",
            rated = false,
            homework = LightHomework(
              id = info.homework.id,
              clazzId = info.homework.clazzId,
              courseId = info.homework.courseId,
              clazzName = info.clazz.name,
              week = info.course.week,
              index = info.course.index,
              dateTime = info.course.dateTime
            ).some
          ) map {
              Ok(_)
            }
        }
      }
    }
  }

  def newHomeworkPuzzle(homeworkId: String, lastPlayed: PuzzleId) = AuthBody { implicit ctx => me =>
    XhrOnly {
      Env.clazz.homeworkStudentApi.info(homeworkId).flatMap {
        case None => notFoundJson("Resource not found")
        case Some(info) => {
          val puzzles = info.homework.puzzles
          val idArray = puzzles.map { p =>
            p.puzzle.id -> p.isComplete
          }.toArray
          env.selector.nextHomeworkPuzzle(lastPlayed, idArray).flatMap {
            case None => notFoundJson("Resource not found")
            case Some(p) => {
              puzzleJson2(
                puzzle = p,
                homework = LightHomework(
                  id = info.homework.id,
                  clazzId = info.homework.clazzId,
                  courseId = info.homework.courseId,
                  clazzName = info.clazz.name,
                  week = info.course.week,
                  index = info.course.index,
                  dateTime = info.course.dateTime
                ).some,
                rated = false
              ) map { json =>
                  Ok(json) as JSON
                }
            }
          }
        }
      }
    }
  }

  def load(id: PuzzleId) = Open { implicit ctx =>
    NoBot {
      XhrOnly {
        OptionFuOk(env.api.puzzle find id)(puzzleJson) map (_ as JSON)
      }
    }
  }

  // XHR load next play puzzle
  def newPuzzle = Auth { implicit ctx => me =>
    NoBot {
      XhrOnly {
        member.isPuzzleContinue(me) flatMap { continue =>
          if (continue) {
            env.selector.nextPuzzle(ctx.me) flatMap puzzleJson map { json =>
              Ok(json) as JSON
            }
          } else fuccess(NotAcceptable("每日试用次数超过上限"))
        }
      }
    }
  }

  // mobile app BC
  def round(id: PuzzleId) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    OptionFuResult(env.api.puzzle find id) { puzzle =>
      if (puzzle.mate) lila.mon.puzzle.round.mate()
      else lila.mon.puzzle.round.material()
      env.forms.round.bindFromRequest.fold(
        jsonFormError,
        data => {
          val result = Result(data.win == 1)
          ctx.me match {
            case Some(me) => for {
              (round, mode) <- env.finisher(puzzle, me, result, data.seconds, lines = data.linesWithEmpty, mobile = true)
              me2 <- if (mode.rated) UserRepo byId me.id map (_ | me) else fuccess(me)
              infos <- env userInfos me2
              voted <- ctx.me.?? { env.api.vote.value(puzzle.id, _) }
              data <- renderJson(puzzle, infos.some, "view", voted = voted, result = result.some, round = round.some)
            } yield {
              lila.mon.puzzle.round.user()
              val d2 = if (mode.rated) data else data ++ Json.obj("win" -> result.win)
              Ok(d2)
            }
            case None =>
              lila.mon.puzzle.round.anon()
              env.finisher.incPuzzleAttempts(puzzle)
              renderJson(puzzle, none, "view", result = result.some, voted = none) map { data =>
                val d2 = data ++ Json.obj("win" -> result.win)
                Ok(d2)
              }
          }
        }
      ) map (_ as JSON)
    }
  }

  // new API
  def round2(id: PuzzleId) = OpenBody { implicit ctx =>
    NoBot {
      implicit val req = ctx.body
      OptionFuResult(env.api.puzzle find id) { puzzle =>
        if (puzzle.mate) lila.mon.puzzle.round.mate()
        else lila.mon.puzzle.round.material()
        env.forms.round.bindFromRequest.fold(
          jsonFormError,
          data => ctx.me match {
            case Some(me) => for {
              (round, mode) <- env.finisher(
                puzzle = puzzle,
                user = me,
                result = Result(data.win == 1),
                seconds = data.seconds,
                lines = data.linesWithEmpty,
                mobile = lila.api.Mobile.Api.requested(ctx.req),
                homeworkId = data.homeworkId
              )
              me2 <- if (mode.rated) UserRepo byId me.id map (_ | me) else fuccess(me)
              infos <- env userInfos me2
              voted <- ctx.me.?? { env.api.vote.value(puzzle.id, _) }
            } yield {
              lila.mon.puzzle.round.user()
              Ok(Json.obj(
                "user" -> lila.puzzle.JsonView.infos(false)(infos),
                "round" -> lila.puzzle.JsonView.round(round),
                "voted" -> voted
              ))
            }
            case None =>
              lila.mon.puzzle.round.anon()
              env.finisher.incPuzzleAttempts(puzzle)
              Ok(Json.obj("user" -> false)).fuccess
          }
        ) map (_ as JSON)
      }
    }
  }

  def vote(id: PuzzleId) = AuthBody { implicit ctx => me =>
    NoBot {
      implicit val req = ctx.body
      env.forms.vote.bindFromRequest.fold(
        jsonFormError,
        vote => env.api.vote.find(id, me) flatMap {
          v => env.api.vote.update(id, me, v, vote == 1)
        } map {
          case (p, a) =>
            if (vote == 1) lila.mon.puzzle.vote.up()
            else lila.mon.puzzle.vote.down()
            Ok(Json.arr(a.value, p.vote.sum))
        }
      ) map (_ as JSON)
    }
  }

  def like(id: PuzzleId) = AuthBody { implicit ctx => me =>
    NoBot {
      implicit val req = ctx.body
      env.forms.like.bindFromRequest.fold(
        jsonFormError,
        like => env.api.tagger.toggle(id, me.id) map { _ =>
          Ok(Json.arr(-1))
        }
      ) map (_ as JSON)
    }
  }

  def setTag(id: PuzzleId) = AuthBody { implicit ctx => me =>
    NoBot {
      implicit val req = ctx.body
      env.forms.tag.bindFromRequest.fold(
        jsonFormError,
        tags => {
          val tagList = tags.split(",").toList
          env.api.tagger.setTags(id, me.id, tagList) map { _ =>
            Ok(Json.arr(tagList))
          }
        }
      ) map (_ as JSON)
    }
  }
  /* Mobile API: select a bunch of puzzles for offline use */
  def batchSelect = Auth { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = _ => for {
        puzzles <- env.batch.select(
          me,
          nb = getInt("nb") getOrElse 50 atLeast 1 atMost 100,
          after = getInt("after")
        )
        userInfo <- env userInfos me
        json <- env.jsonView.batch(puzzles, userInfo)
      } yield Ok(json) as JSON
    )
  }

  /* Mobile API: tell the server about puzzles solved while offline */
  def batchSolve = AuthBody(BodyParsers.parse.json) { implicit ctx => me =>
    import lila.puzzle.PuzzleBatch._
    ctx.body.body.validate[SolveData].fold(
      err => BadRequest(err.toString).fuccess,
      data => negotiate(
        html = notFound,
        api = _ => for {
          _ <- env.batch.solve(me, data)
          me2 <- UserRepo byId me.id map (_ | me)
          infos <- env userInfos me2
        } yield Ok(Json.obj(
          "user" -> lila.puzzle.JsonView.infos(false)(infos)
        ))
      )
    )
  }

  /* For BC */
  def embed = Action { req =>
    Ok {
      val bg = get("bg", req) | "light"
      val theme = get("theme", req) | "brown"
      val url = s"""${req.domain + routes.Puzzle.frame}?bg=$bg&theme=$theme"""
      s"""document.write("<iframe src='https://$url&embed=" + document.domain + "' class='lichess-training-iframe' allowtransparency='true' frameborder='0' style='width: 224px; height: 264px;' title='Haichess free online chess'></iframe>");"""
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def frame = Action.async { implicit req =>
    env.daily.get map {
      case None => NotFound
      case Some(daily) => html.puzzle.embed(daily)
    }
  }

  def activity = Scoped(_.Puzzle.Read) { req => me =>
    Api.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
      Api.GlobalLinearLimitPerUserOption(me.some) {
        val config = lila.puzzle.PuzzleActivity.Config(
          user = me,
          max = getInt("max", req) map (_ atLeast 1),
          perSecond = MaxPerSecond(20)
        )
        Ok.chunked(env.activity.stream(config)).withHeaders(
          noProxyBufferHeader,
          CONTENT_TYPE -> ndJsonContentType
        ).fuccess
      }
    }
  }

  def isPuzzleContinue() = Auth { implicit ctx => me =>
    member.isPuzzleContinue(me) map { continue =>
      Ok(Json.obj("ok" -> continue))
    }
  }

  def isThemePuzzleContinue() = Auth { implicit ctx => me =>
    member.isThemePuzzleContinue(me) map { continue =>
      Ok(Json.obj("ok" -> continue))
    }
  }

}
