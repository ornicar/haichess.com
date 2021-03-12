package lila.clazz

import lila.user.User
import lila.team.Team
import org.joda.time.DateTime
import ornicar.scalalib.Random
import Clazz._

case class Clazz(
    _id: ID,
    name: String,
    color: String,
    coach: CoachID,
    team: Option[String],
    clazzType: ClazzType,
    weekClazz: Option[WeekClazz],
    trainClazz: Option[TrainClazz],
    students: Option[Students],
    stopped: Boolean,
    createdAt: DateTime,
    updatedAt: DateTime,
    createdBy: User.ID,
    updatedBy: User.ID,
    deleted: Option[Boolean] = None
) {

  def id = _id

  def studentList: List[(String, Student)] = students.fold(List.empty[(String, Student)])(_.toList)

  def joinedStudentList: List[(String, Student)] = studentList.filter(_._2.joined)

  def studentCount = joinedStudentList.size

  def studentEmpty = studentCount == 0

  def studentsId: List[String] = joinedStudentList.map(_._1)

  def findStudent(user: User) = joinedStudentList.find(_._1 == user.id)

  def editable = clazzType match {
    case ClazzType.Week => weekClazz.??(_.dateTimeBegin.isAfterNow && !stopped && studentEmpty)
    case ClazzType.Train => trainClazz.??(_.dateTimeBegin.isAfterNow && !stopped && studentEmpty)
  }

  def deleteAble = studentEmpty

  def isCreator(user: String) = user == createdBy

  def belongTo(user: String) = isCreator(user) || isStudent(user)

  def isStudent(user: String) = joinedStudentList.exists(_._1 == user)

  def teamOrDefault = team | "-"

  def isCoach(user: Option[User]) = user.??(_.id == coach)

  def toCourse(id: ID, coach: CoachID): List[Course] = {
    clazzType match {
      case ClazzType.Week => weekClazz.map(_.toCourseFromWeek(id, coach))
      case ClazzType.Train => trainClazz.map(_.toCourseFromTrain(id, coach))
    }
  } err s"can not convert to course of clazz ${name}"

  def times: Int = {
    clazzType match {
      case ClazzType.Week => weekClazz.map(_.times)
      case ClazzType.Train => trainClazz.map(_.times)
    }
  } err s"can not find times of clazz ${name}"

}

object Clazz {

  type ID = String
  type CoachID = User.ID

  sealed abstract class ClazzType(val id: String, val name: String)
  object ClazzType {
    case object Week extends ClazzType("week", "周定时")
    case object Train extends ClazzType("train", "集训")

    val all = List(Week, Train)
    val byId = all map { v => (v.id, v) } toMap
    def apply(id: String): ClazzType = byId get id err s"Bad ClazzType $id"
  }

  def make(
    user: User,
    name: String,
    color: String,
    team: Option[String],
    clazzType: ClazzType,
    weekClazz: Option[WeekClazz],
    trainClazz: Option[TrainClazz]
  ): Clazz = new Clazz(
    _id = genId,
    name = name,
    color = color,
    coach = user.id,
    team = team,
    clazzType = clazzType,
    weekClazz = weekClazz,
    trainClazz = trainClazz,
    students = None,
    stopped = false,
    createdAt = DateTime.now,
    updatedAt = DateTime.now,
    createdBy = user.id,
    updatedBy = user.id
  )

  def genId = Random nextString 8

  case class ClazzWithCoach(clazz: Clazz, coach: User) {

    def isStudent(user: User.ID) = clazz.isStudent(user)
    def isCoach(user: User.ID) = clazz.coach == user
  }

  case class ClazzWithTeam(clazz: Clazz, team: Option[Team]) {
    val teamName = team.fold("-")(_.name)
  }

}
