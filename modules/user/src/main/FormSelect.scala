package lila.user

object FormSelect {

  sealed abstract class Sex(val key: String, val name: String)
  object Sex {
    case object Male extends Sex("male", "男")
    case object Female extends Sex("female", "女")

    val all = List(Male, Female)
    val keySet = all.map(_.key).toSet
    def byKey(key: String) = all.find(_.key == key) err s"Bad Sex $key"
    def name(key: String) = byKey(key).name
    def list = all.map(s => (s.key -> s.name))
  }

  sealed abstract class Level(val key: String, val name: String, val rating: Double, val order: Int)
  object Level {
    case object M1 extends Level("M1", "棋协大师", 1700, 1)
    case object M0 extends Level("M0", "棋协候补大师", 1600, 2)
    case object L1 extends Level("L1", "棋协一级棋士", 1550, 3)
    case object L2 extends Level("L2", "棋协二级棋士", 1500, 4)
    case object L3 extends Level("L3", "棋协三级棋士", 1450, 5)
    case object L4 extends Level("L4", "棋协四级棋士", 1400, 6)
    case object L5 extends Level("L5", "棋协五级棋士", 1350, 7)
    case object L6 extends Level("L6", "棋协六级棋士", 1300, 8)
    case object L7 extends Level("L7", "棋协七级棋士", 1250, 9)
    case object L8 extends Level("L8", "棋协八级棋士", 1200, 10)
    case object L9 extends Level("L9", "棋协九级棋士", 1150, 11)
    case object L10 extends Level("L10", "棋协十级棋士", 1100, 12)
    case object L11 extends Level("L11", "棋协十一级棋士", 1000, 13)
    case object L12 extends Level("L12", "棋协十二级棋士", 950, 14)
    case object L13 extends Level("L13", "棋协十三级棋士", 900, 15)
    case object L14 extends Level("L14", "棋协十四级棋士", 850, 16)
    case object L15 extends Level("L15", "棋协十五级棋士", 800, 17)
    case object Nil extends Level("-", "无定级", 1200, 18)

    val all = List(M1, M0, L1, L2, L3, L4, L5, L6, L7, L8, L9, L10, L11, L12, L13, L14, L15, Nil)
    val keySet = all.map(_.key).toSet

    def byKey(key: String) = all.find(_.key == key) err s"Bad Level $key"
    def list = all.map(l => (l.key -> l.name))
    def levelRating(key: String) = byKey(key).rating
    def levelLabel(key: String) = byKey(key).name
    def levelWithRating = all.map(l => (l.key -> s"${l.name}（${l.rating.toInt}）"))
    def levelLevelUp(key: String) = {
      val levelOrder = byKey(key).order
      all.filter(_.order <= levelOrder).map(l => (l.key -> l.name))
    }
  }

}
