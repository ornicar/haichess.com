package lila

import lila.clazz.Clazz.ClazzWithCoach

package object clazz extends PackageObject {

  private[clazz] def logger = lila.log("clazz")

  import ornicar.scalalib.Zero

  implicit final val withCoachZero = Zero.instance[ClazzWithCoach](null)

  case class StudentHomeworkCreate(homeworks: List[HomeworkStudent])

}
