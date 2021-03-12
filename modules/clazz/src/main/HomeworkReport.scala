package lila.clazz

import lila.user.User
import org.joda.time.DateTime

case class HomeworkReport(
    _id: Homework.ID,
    num: Int,
    common: Map[User.ID, List[HomeworkCommonWithResult]],
    practice: HomeworkPracticeReport,
    updateAt: DateTime,
    createAt: DateTime,
    createBy: User.ID
) {

  def id = _id

}

