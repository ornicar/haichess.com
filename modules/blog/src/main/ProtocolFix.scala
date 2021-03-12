package lila.blog

object ProtocolFix {

  private val RemoveRegex = """http://(\w{2}\.)?+lichess\.org""".r
  def remove(html: String) = RemoveRegex.replaceAllIn(html, _ => "//haichess.com")

  private val AddRegex = """(https?+:)?+(//)?+(\w{2}\.)?+lichess\.org""".r
  def add(html: String) = AddRegex.replaceAllIn(html, _ => "https://haichess.com")
}
