package lila.resource

import play.api.data._
import play.api.data.Forms._
import lila.common.Form._

object DataForm {

  object puzzle {

    val liked = Form(mapping(
      "tags" -> optional(list(nonEmptyText)),
      "order" -> optional(numberIn(Sorting.orders))
    )(LikedData.apply)(LikedData.unapply)) fill LikedData()

    val imported = Form(mapping(
      "tags" -> optional(list(nonEmptyText)),
      "order" -> optional(numberIn(Sorting.orders))
    )(ImportedData.apply)(ImportedData.unapply)) fill ImportedData()

    val theme = Form(mapping(
      "idMin" -> optional(number),
      "idMax" -> optional(number),
      "ratingMin" -> optional(number(min = 600, max = 2800)),
      "ratingMax" -> optional(number(min = 600, max = 2800)),
      "stepsMin" -> optional(number(min = 1, max = 100)),
      "stepsMax" -> optional(number(min = 1, max = 100)),
      "tags" -> optional(list(nonEmptyText)),
      "phase" -> optional(list(stringIn(ThemeQuery.phase))),
      "moveFor" -> optional(list(stringIn(ThemeQuery.moveFor))),
      "pieceColor" -> optional(list(stringIn(ThemeQuery.pieceColor))),
      "subject" -> optional(list(stringIn(ThemeQuery.subject))),
      "strength" -> optional(list(stringIn(ThemeQuery.strength))),
      "chessGame" -> optional(list(stringIn(ThemeQuery.chessGame))),
      "comprehensive" -> optional(list(stringIn(ThemeQuery.comprehensive))),
      "order" -> optional(numberIn(Sorting.orders))
    )(ThemeData.apply)(ThemeData.unapply)) fill ThemeData()

    case class LikedData(
        tags: Option[List[String]] = None,
        order: Option[Int] = Sorting.default.order.some
    ) {

      val sortOrder = order getOrElse (Sorting.default.order)
    }

    case class ImportedData(
        tags: Option[List[String]] = None,
        order: Option[Int] = Sorting.default.order.some
    ) {

      val sortOrder = order getOrElse (Sorting.default.order)
    }

    case class ThemeData(
        idMin: Option[Int] = None,
        idMax: Option[Int] = None,
        ratingMin: Option[Int] = None,
        ratingMax: Option[Int] = None,
        stepsMin: Option[Int] = None,
        stepsMax: Option[Int] = None,
        tags: Option[List[String]] = None,
        phase: Option[List[String]] = None,
        moveFor: Option[List[String]] = None,
        pieceColor: Option[List[String]] = None,
        subject: Option[List[String]] = None,
        strength: Option[List[String]] = None,
        chessGame: Option[List[String]] = None,
        comprehensive: Option[List[String]] = None,
        order: Option[Int] = Sorting.default.order.some
    ) {

      val sortOrder = order getOrElse (Sorting.default.order)
    }

  }

  object game {
    val liked = Form(single(
      "tags" -> optional(list(nonEmptyText))
    ))

    val imported = Form(single(
      "tags" -> optional(list(nonEmptyText))
    ))

  }

  object capsule {

    def capsuleFormOf(capsule: Capsule) =
      capsuleForm fill CapsuleData(name = capsule.name, tags = capsule.tags.mkString(",").some, desc = capsule.desc)

    def capsuleForm = Form(mapping(
      "name" -> nonEmptyText(minLength = 2, maxLength = 20),
      "tags" -> optional(nonEmptyText),
      "desc" -> optional(nonEmptyText)
    )(CapsuleData.apply)(CapsuleData.unapply))

    case class CapsuleData(
        name: String,
        tags: Option[String],
        desc: Option[String]
    ) {

      def makeTags =
        tags.map { tg =>
          val t = tg.trim()
          if (t.isEmpty) List.empty else t.split(",").toList
        } | List.empty
    }

    def capsuleSearchForm = Form(mapping(
      "name" -> optional(nonEmptyText),
      "enabled" -> optional(numberIn(enabledSelect)),
      "tags" -> optional(list(nonEmptyText))
    )(CapsuleSearchData.apply)(CapsuleSearchData.unapply))

    case class CapsuleSearchData(
        name: Option[String] = None,
        enabled: Option[Int] = None,
        tags: Option[List[String]] = None
    )

    val enabledSelect = List(0 -> "锁定", 1 -> "活动")

    val puzzleOrder = Form(single("order" -> optional(numberIn(Sorting.orders))))
  }

}

