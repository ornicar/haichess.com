package lila.shutup

import org.specs2.mutable._

class DetectTest extends Specification {

  private def find(t: String) = Analyser(t).badWords
  private def ratio(t: String) = Analyser(t).ratio

  "detector" should {
    "find one bad word" in {
      find("fuck") must_== List("fuck")
      find("well fuck me") must_== List("fuck")
    }
    "find many bad words" in {
      find("fucked that shit") must_== List("fucked", "shit")
      find("Beat them cunting nigger faggots with a communist dick") must_==
        List("cunting", "nigger", "faggots", "dick")
    }
    "find no bad words" in {
      find("") must_== Nil
      find("hello there") must_== Nil
      find("A sonnet is a poetic form which originated in Italy; Giacomo Da Lentini is credited with its invention.") must_== Nil
      find("computer analysis") must_== Nil
    }
    "find badly spelled words" in {
      find("fuk") must_== List("fuk")
      find("well fuk me") must_== List("fuk")
      find("foo ashole bar fukd") must_== List("ashole", "fukd")
    }
    "find variants" in {
      find("cunt kunt cunting kawa kunting") must_== List("cunt", "kunt", "cunting", "kunting")
      find("ass as ashole") must_== List("ass", "ashole")
    }
    "find plurals" in {
      find("asses cunts kunts cuntings kawas kuntings") must_== List("asses", "cunts", "kunts", "cuntings", "kuntings")
    }
    "50 shades of fuck" in {
      find("fuck fffuuk fektard feak fak phuk") must_== List("fuck", "fffuuk", "fektard", "fak", "phuk")
    }
    "compute ratio" in {
      ratio("fuck that shit") must_== 2d / 3
      ratio("Beat them cunting nigger faggots with a communist dick") must_== 4d / 9
      ratio("hello there") must_== 0
      ratio("") must_== 0
    }
    "chessbot variations" in {
      find("foo chessbot bar") must_== List("chessbot")
      find("foo chess-bot bar") must_== List("chess-bot")
      find("foo chess_bot bar") must_== List("chess_bot")
      find("foo chess-bot.com bar") must_== List("chess-bot.com")
      find("foo chess_bot_com bar") must_== List("chess_bot_com")
      find("foo chessbotcom bar") must_== List("chessbotcom")
      find("foo http://chess-bot.com bar") must_== List("chess-bot.com")
    }
    "russian chars" in {
      find("s??x") must_== List("sex")
    }
    "russian" in {
      find("???????? ??????????") must_== List("????????", "??????????")
    }
  }
}
