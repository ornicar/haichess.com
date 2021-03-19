package lila.puzzle

import lila.user.User
import org.joda.time.DateTime
import java.net.URLDecoder
import lila.resource.ThemeQuery

case class ThemeRecord(
    _id: User.ID,
    puzzleId: PuzzleId,
    updateAt: DateTime
) {

  def id = _id

  /*  def toTags: List[String] = {
    val qs = URLDecoder.decode(queryString, "UTF-8")
    val dataMap = qs.split("&").map { kvString =>
      val kv = kvString.split("=")
      if (kv.length > 1) {
        kv(0) -> kv(1)
      } else {
        kv(0) -> ""
      }
    }.filter(_._2.nonEmpty).groupBy {
      case (k, _) => k.replaceAll("\\[\\d+\\]", "")
    }.mapValues(_.map(_._2))

    List(
      dataMap.get("ratingMin").map(_.toList.map(r => s">=${r}分")),
      dataMap.get("ratingMax").map(_.toList.map(r => s"<=${r}分")),
      dataMap.get("stepsMin").map(_.toList.map(r => s">=${r}步")),
      dataMap.get("stepsMax").map(_.toList.map(r => s"<=${r}步")),
      ThemeQuery.parseArrayLabel(dataMap.get("phase").map(_.toList), ThemeQuery.phase),
      ThemeQuery.parseArrayLabel(dataMap.get("moveFor").map(_.toList), ThemeQuery.moveFor),
      ThemeQuery.parseArrayLabel(dataMap.get("pieceColor").map(_.toList), ThemeQuery.pieceColor),
      ThemeQuery.parseArrayLabel(dataMap.get("subject").map(_.toList), ThemeQuery.subject),
      ThemeQuery.parseArrayLabel(dataMap.get("strength").map(_.toList), ThemeQuery.strength),
      ThemeQuery.parseArrayLabel(dataMap.get("chessGame").map(_.toList), ThemeQuery.chessGame),
      ThemeQuery.parseArrayLabel(dataMap.get("comprehensive").map(_.toList), ThemeQuery.comprehensive),
      dataMap.get("tags").map(_.toList)
    ).filter(_.isDefined).flatMap(_.get)
  }*/

}

object ThemeRecord {

  def make(
    userId: User.ID,
    puzzleId: PuzzleId
  ) = {
    ThemeRecord(
      _id = userId,
      puzzleId = puzzleId,
      updateAt = DateTime.now
    )
  }

  /*  def makeId(queryString: String): String = {
    import java.security.MessageDigest
    val md5 = MessageDigest.getInstance("MD5")
    val encoded = md5.digest(queryString.getBytes)
    encoded.map("%02x".format(_)).mkString
  }*/

}
