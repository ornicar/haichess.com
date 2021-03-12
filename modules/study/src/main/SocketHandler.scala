package lila.study

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import play.api.libs.functional.syntax._
import play.api.libs.json._

import chess.format.pgn.Glyph
import lila.chat.Chat
import lila.common.ApiVersion
import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.makeMessage
import lila.socket.Socket.{ Sri, SocketVersion }
import lila.socket.{ Handler, AnaMove, AnaDrop, AnaAny }
import lila.tree.Node.{ Shape, Shapes, Comment, Gamebook }
import lila.user.User
import makeTimeout.short

final class SocketHandler(
    hub: lila.hub.Env,
    socketMap: SocketMap,
    chat: ActorSelection,
    api: StudyApi,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler
) {

  import Handler.AnaRateLimit
  import JsonView.shapeReader

  private val InviteLimitPerUser = new lila.memo.RateLimit[User.ID](
    credits = 50,
    duration = 24 hour,
    name = "study invites per user",
    key = "study_invite.user"
  )

  private def moveOrDrop(studyId: Study.Id, m: AnaAny, opts: MoveOpts, sri: Sri, member: StudySocket.Member) =
    AnaRateLimit(sri, member) {
      m.branch match {
        case scalaz.Success(branch) if branch.ply < Node.MAX_PLIES =>
          member push makeMessage("node", m json branch)
          for {
            userId <- member.userId
            chapterId <- m.chapterId
            if opts.write
          } api.addNode(
            userId,
            studyId,
            Position.Ref(Chapter.Id(chapterId), Path(m.path)),
            Node.fromBranch(branch) withClock opts.clock,
            sri,
            opts
          )
        case scalaz.Success(branch) =>
          member push makeMessage("stepFailure", s"ply ${branch.ply}/${Node.MAX_PLIES}")
        case scalaz.Failure(err) =>
          member push makeMessage("stepFailure", err.toString)
      }
    }

  def makeController(
    socket: StudySocket,
    studyId: Study.Id,
    sri: Sri,
    member: StudySocket.Member,
    user: Option[User]
  ): Handler.Controller = ({
    case ("talk", o) => o str "d" foreach { text =>
      member.userId foreach { api.talk(_, studyId, text) }
    }
    case ("anaMove", o) => AnaMove parse o foreach {
      moveOrDrop(studyId, _, MoveOpts parse o, sri, member)
    }
    case ("anaDrop", o) => AnaDrop parse o foreach {
      moveOrDrop(studyId, _, MoveOpts parse o, sri, member)
    }
    case ("setPath", o) => AnaRateLimit(sri, member) {
      reading[AtPosition](o) { position =>
        member.userId foreach { userId =>
          api.setPath(userId, studyId, position.ref, sri)
        }
      }
    }
    case ("deleteNode", o) => AnaRateLimit(sri, member) {
      reading[AtPosition](o) { position =>
        for {
          jumpTo <- (o \ "d" \ "jumpTo").asOpt[String] map Path.apply
          userId <- member.userId
        } api.setPath(userId, studyId, position.ref.withPath(jumpTo), sri) >>
          api.deleteNodeAt(userId, studyId, position.ref, sri)
      }
    }
    case ("promote", o) => AnaRateLimit(sri, member) {
      reading[AtPosition](o) { position =>
        for {
          toMainline <- (o \ "d" \ "toMainline").asOpt[Boolean]
          userId <- member.userId
        } api.promote(userId, studyId, position.ref, toMainline, sri)
      }
    }
    case ("forceVariation", o) => AnaRateLimit(sri, member) {
      reading[AtPosition](o) { position =>
        for {
          force <- (o \ "d" \ "force").asOpt[Boolean]
          userId <- member.userId
        } api.forceVariation(userId, studyId, position.ref, force, sri)
      }
    }
    case ("setRole", o) => AnaRateLimit(sri, member) {
      reading[SetRole](o) { d =>
        member.userId foreach { userId =>
          api.setRole(userId, studyId, d.userId, d.role)
        }
      }
    }
    case ("invite", o) => for {
      byUserId <- member.userId
      username <- o str "d"
    } InviteLimitPerUser(byUserId, cost = 1) {
      api.invite(byUserId, studyId, username, socket,
        onError = err => member push makeMessage("error", err))
    }

    case ("kick", o) => for {
      byUserId <- member.userId
      username <- o str "d"
    } api.kick(byUserId, studyId, username)

    case ("leave", _) => member.userId foreach { userId =>
      api.kick(userId, studyId, userId)
    }

    case ("shapes", o) =>
      reading[AtPosition](o) { position =>
        for {
          shapes <- (o \ "d" \ "shapes").asOpt[List[Shape]]
          userId <- member.userId
        } api.setShapes(userId, studyId, position.ref, Shapes(shapes take 32), sri)
      }

    case ("addChapter", o) =>
      reading[ChapterMaker.Data](o) { data =>
        member.userId foreach { byUserId =>
          val sticky = o.obj("d").flatMap(_.boolean("sticky")) | true
          api.addChapter(byUserId, studyId, data, sticky = sticky, sri)
        }
      }

    case ("setChapter", o) => for {
      byUserId <- member.userId
      chapterId <- o.get[Chapter.Id]("d")
    } api.setChapter(byUserId, studyId, chapterId, sri)

    case ("editChapter", o) =>
      reading[ChapterMaker.EditData](o) { data =>
        member.userId foreach {
          api.editChapter(_, studyId, data, sri)
        }
      }

    case ("descStudy", o) => for {
      desc <- o str "d"
      userId <- member.userId
    } api.descStudy(userId, studyId, desc, sri)

    case ("descChapter", o) =>
      reading[ChapterMaker.DescData](o) { data =>
        member.userId foreach {
          api.descChapter(_, studyId, data, sri)
        }
      }

    case ("deleteChapter", o) => for {
      byUserId <- member.userId
      id <- o.get[Chapter.Id]("d")
    } api.deleteChapter(byUserId, studyId, id, sri)

    case ("clearAnnotations", o) => for {
      byUserId <- member.userId
      id <- o.get[Chapter.Id]("d")
    } api.clearAnnotations(byUserId, studyId, id, sri)

    case ("sortChapters", o) => for {
      byUserId <- member.userId
      ids <- o.get[List[Chapter.Id]]("d")
    } api.sortChapters(byUserId, studyId, ids, sri)

    case ("editStudy", o) => for {
      byUserId <- member.userId
      data <- (o \ "d").asOpt[Study.Data]
    } api.editStudy(byUserId, studyId, data)

    case ("setTag", o) =>
      reading[actorApi.SetTag](o) { setTag =>
        member.userId foreach { byUserId =>
          api.setTag(byUserId, studyId, setTag, sri)
        }
      }

    case ("setComment", o) =>
      reading[AtPosition](o) { position =>
        for {
          userId <- member.userId
          text <- (o \ "d" \ "text").asOpt[String]
        } api.setComment(userId, studyId, position.ref, Comment sanitize text, sri)
      }

    case ("deleteComment", o) =>
      reading[AtPosition](o) { position =>
        for {
          userId <- member.userId
          id <- (o \ "d" \ "id").asOpt[String]
        } api.deleteComment(userId, studyId, position.ref, Comment.Id(id), sri)
      }

    case ("setGamebook", o) =>
      reading[AtPosition](o) { position =>
        for {
          userId <- member.userId
          gamebook <- (o \ "d" \ "gamebook").asOpt[Gamebook].map(_.cleanUp)
        } api.setGamebook(userId, studyId, position.ref, gamebook, sri)
      }

    case ("toggleGlyph", o) =>
      reading[AtPosition](o) { position =>
        for {
          userId <- member.userId
          glyph <- (o \ "d" \ "id").asOpt[Int] flatMap Glyph.find
        } api.toggleGlyph(userId, studyId, position.ref, glyph, sri)
      }

    case ("explorerGame", o) =>
      reading[actorApi.ExplorerGame](o) { data =>
        member.userId foreach { byUserId =>
          api.explorerGame(byUserId, studyId, data, sri)
        }
      }

    case ("like", o) => for {
      byUserId <- member.userId
      v <- (o \ "d" \ "liked").asOpt[Boolean]
    } api.like(studyId, byUserId, v, sri)

    case ("requestAnalysis", o) => for {
      byUserId <- member.userId
      chapterId <- o.get[Chapter.Id]("d")
    } api.analysisRequest(studyId, chapterId, byUserId)

  }: Handler.Controller) orElse evalCacheHandler(sri, member, user) orElse lila.chat.Socket.in(
    chatId = Chat.Id(studyId.value),
    member = member,
    chat = chat,
    canTimeout = Some { suspectId =>
      user.?? { u =>
        api.isContributor(studyId, u.id) >>& !api.isMember(studyId, suspectId)
      }
    },
    publicSource = none // the "talk" event is handled by the study API
  )

  private def reading[A](o: JsValue)(f: A => Unit)(implicit reader: Reads[A]): Unit =
    o obj "d" flatMap { d => reader.reads(d).asOpt } foreach f

  private case class AtPosition(path: String, chapterId: Chapter.Id) {
    def ref = Position.Ref(chapterId, Path(path))
  }
  private implicit val chapterIdReader = stringIsoReader(Chapter.idIso)
  private implicit val chapterNameReader = stringIsoReader(Chapter.nameIso)
  private implicit val atPositionReader = (
    (__ \ "path").read[String] and
    (__ \ "ch").read[Chapter.Id]
  )(AtPosition.apply _)
  private case class SetRole(userId: String, role: String)
  private implicit val SetRoleReader = Json.reads[SetRole]
  private implicit val ChapterDataReader = Json.reads[ChapterMaker.Data]
  private implicit val ChapterEditDataReader = Json.reads[ChapterMaker.EditData]
  private implicit val ChapterDescDataReader = Json.reads[ChapterMaker.DescData]
  private implicit val StudyDataReader = Json.reads[Study.Data]
  private implicit val setTagReader = Json.reads[actorApi.SetTag]
  private implicit val gamebookReader = Json.reads[Gamebook]
  private implicit val explorerGame = Json.reads[actorApi.ExplorerGame]

  def getSocket(studyId: Study.Id) = socketMap getOrMake studyId.value

  def join(
    studyId: Study.Id,
    sri: Sri,
    user: Option[User],
    version: Option[SocketVersion],
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] = {
    val socket = getSocket(studyId)
    join(studyId, sri, user, socket, member => makeController(socket, studyId, sri, member, user = user), version, apiVersion)
  }

  def join(
    studyId: Study.Id,
    sri: Sri,
    user: Option[User],
    socket: StudySocket,
    controller: StudySocket.Member => Handler.Controller,
    version: Option[SocketVersion],
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] =
    socket.ask[StudySocket.Connected](StudySocket.Join(sri, user.map(_.id), user.??(_.troll), version, _)) map {
      case StudySocket.Connected(enum, member) => Handler.iteratee(
        hub,
        controller(member),
        member,
        socket,
        sri,
        apiVersion
      ) -> enum
    }
}
