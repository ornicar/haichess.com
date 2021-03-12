package lila.puzzle

import org.joda.time.DateTime

case class LightHomework(
    id: String,
    clazzId: String,
    courseId: String,
    clazzName: String,
    week: Int,
    index: Int,
    dateTime: DateTime
)