package lila.user

import org.specs2.mutable.Specification

class UserTest extends Specification {

  def canSignup(str: User.ID) =
    User.newUsernamePrefix.pattern.matcher(str).matches && User.newUsernameSuffix.pattern.matcher(str).matches/* && User.newUsernameChars.pattern.matcher(str).matches*/

  "username regex" in {
    import User.couldBeUsername
    "bad prefix: can login" in {
      couldBeUsername("aa000") must beTrue
/*      couldBeUsername("0foo") must beTrue
      couldBeUsername("_foo") must beFalse
      couldBeUsername("-foo") must beFalse*/
    }

    /*
    "bad prefix: cannot signup" in {
      canSignup("A000") must beFalse
      canSignup("啊0foo下") must beFalse
      canSignup("收到_foo") must beFalse
      canSignup("-foo阿斯蒂芬") must beFalse
    }

    "bad suffix" in {
      couldBeUsername("a_") must beFalse
      couldBeUsername("a_") must beFalse
    }

    "OK things" in {
      couldBeUsername("g-foo") must beTrue
      couldBeUsername("G_FOo") must beTrue
      couldBeUsername("g-foo") must beTrue
    }*/
  }

}
