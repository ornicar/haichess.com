package lila.coach

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

object CoachProfileForm {

  def edit(coach: Coach) = Form(mapping(
    "available" -> boolean,
    "profile" -> mapping(
      "picturePath" -> optional(text(minLength = 5, maxLength = 150)),
      "headline" -> optional(text(minLength = 4, maxLength = 150)),
      "languages" -> optional(text(minLength = 2, maxLength = 20)),
      "hourlyRate" -> optional(text(minLength = 2, maxLength = 20)),
      "description" -> optional(richText),
      "playingExperience" -> optional(richText),
      "teachingExperience" -> optional(richText),
      "otherExperience" -> optional(richText),
      "skills" -> optional(richText),
      "methodology" -> optional(richText),
      "youtubeVideos" -> optional(nonEmptyText),
      "youtubeChannel" -> optional(nonEmptyText),
      "publicStudies" -> optional(nonEmptyText)
    )(CoachProfile.apply)(CoachProfile.unapply)
  )(Data.apply)(Data.unapply)) fill Data(
    available = coach.available.value,
    profile = coach.profile
  )

  case class Data(
      available: Boolean,
      profile: CoachProfile
  ) {

    def apply(coach: Coach) = coach.copy(
      available = Coach.Available(available),
      profile = profile,
      updatedAt = DateTime.now
    )
  }

  import CoachProfile.RichText

  private implicit val richTextFormat = lila.common.Form.formatter.stringFormatter[RichText](_.value, RichText.apply)
  private def richText = of[RichText]
}
