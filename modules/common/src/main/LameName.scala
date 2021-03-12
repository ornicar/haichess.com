package lila.common

import scala.io.Source

object LameName {

  // 保留的
  def reserved(name: String) = {
    val n = name.replaceIf('_', "").replaceIf('-', "").toLowerCase
    anyMatchReservedName(n) || anyExactReservedName(n)
  }

  // 敏感的
  def sensitive(name: String) = {
    val n = name.replaceIf('_', "").replaceIf('-', "").toLowerCase
    anyMatchSensitiveName(n) || anyExactSensitiveName(n) || lameTitlePrefix.matcher(name).lookingAt
  }

  def anyMatchReservedName(name: String) = matchReserved.pattern.matcher(name).find
  def anyMatchSensitiveName(name: String) = matchSensitive.pattern.matcher(name).find
  def anyExactReservedName(name: String) = exactReserved.contains(name)
  def anyExactSensitiveName(name: String) = exactSensitive.contains(name)

  private val lameTitlePrefix =
    "[Ww]?+[NCFIGl1L]M|(?i:w?+[ncfigl1])m[-_A-Z0-9]".r.pattern

  //-------------------------------------------------------------------------------

  // 模糊匹配保留词
  private def matchReserved = {
    val words =
      Source.fromFile("data/lame/系统中文.txt").getLines() ++
        Source.fromFile("data/lame/系统拼音.txt").getLines()
    words.map(_.toLowerCase).mkString("|").r
  }

  // 模糊匹配敏感词
  private def matchSensitive = {
    val words =
      Source.fromFile("data/lame/领导人a.txt").getLines() ++
        Source.fromFile("data/lame/脏话中文.txt").getLines()
    words.map(_.toLowerCase).mkString("|").r
  }

  // 精确匹配保留词
  private def exactReserved = {
    val words =
      Source.fromFile("data/lame/棋手中文.txt").getLines() ++
        Source.fromFile("data/lame/棋手拼音.txt").getLines()
    words.map(_.toLowerCase)
  }

  // 精确匹配敏感词
  private def exactSensitive = {
    val words =
      Source.fromFile("data/lame/脏话拼音.txt").getLines() ++
        Source.fromFile("data/lame/领导人b.txt").getLines()
    words.map(_.toLowerCase)
  }

}
