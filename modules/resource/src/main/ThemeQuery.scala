package lila.resource

object ThemeQuery {

  val phase = List("Opening" -> "开局", "MiddleGame" -> "中局", "EndingGame" -> "残局")

  val moveFor = List("DeZi" -> "得子", "ShaWang" -> "杀王", "QiuHe" -> "求和", "BingShengBian" -> "兵升变", "FangYu" -> "防守")

  val pieceColor = List("White" -> "白方", "Black" -> "黑方")

  val subject = List("QianZi" -> "牵制", "ZhuoShuang" -> "捉双", "ShuangChongGongJi" -> "双重攻击", "ShanJi" -> "闪击", "ChuanJi" -> "串击", "ShuangJiang" -> "双将", "YinRu" -> "引入", "YinLi" -> "引离", "TouShi" -> "透视", "LanJie" -> "拦截", "TengNuo" -> "腾挪", "DuSe" -> "堵塞", "QuGan" -> "驱赶", "GuoMen" -> "过门", "XiaoChuBaoHu" -> "消除保护", /*"GuoZai" -> "过载",*/ "BiZouLieZhao" -> "逼走劣着", "GuoDuZhao" -> "过渡着", /*"DengZhao" -> "等着",*/ "WeiKun" -> "围困", "FangYu" -> "防御", "MenSha" -> "闷杀")

  val chessGame = List( /*"YiSeGeXiang" -> "异色格象", "TongSeGeXiang" -> "同色格象",*/ "ShuangXiang" -> "双象", /*"CeYiXiang" -> "侧翼象", */ "ShuangChe" -> " 双车", /*"TiChe" -> "提车",*/ "CiDiXianChe" -> "次底线车", "DiXian" -> "底线", "GuoZai" -> "过载", /*"MenSha" -> "闷杀",*/ "BeiKunDeZi" -> "被困的子", /* "QianShao" -> "前哨", "XuanZi" -> "悬子", "FengChe" -> "风车",*/ "XieXianChongDie" -> "斜线重叠", "ChuiZhiChongDie" -> "垂直重叠", "YiShouGongJiDeWang" -> "易受攻击的王", "NiXiangYiWei" -> "逆向易位", "ZhongXinWang" -> "中心王", /* "DuiWang" -> "对王", "TianChuang" -> "天窗",*/ "TongLuBing" -> "通路兵" /*, "LianBing" -> "连兵", "DieBing" -> "叠兵", "GuBing" -> "孤兵"*/ )

  val comprehensive = List("JiBenChiZi" -> "基本吃子", "YiBuSha" -> "一步杀", "LiangBuSha" -> "两步杀", "SanBuSha" -> "三步杀", "SiBuYiShangSha" -> "四步以上杀", "JianHua" -> "简化", "BingShengBian" -> "兵升变", "TeSuShengBian" -> "特殊升变", "ChiGuoLuBing" -> "吃过路兵", "ChangJiang" -> "长将/长捉", /*"ChangZhuo" -> "长捉",*/ "QiZiMouHe" -> "弃子谋和", "WeiXieShaWang" -> "威胁杀王")

  val strength = List("Queen" -> "后", "Rook" -> "车", "Bishop" -> "象", "Knight" -> "马", "Pawn" -> "兵", "King" -> "王")

  def parseLabel(key: Option[String], theme: List[(String, String)]) = key map { k =>
    theme.filter(k == _._1).map(_._2)
  }

  def parseArrayLabel(array: Option[List[String]], theme: List[(String, String)]) = array map { arr =>
    theme.filter(t => arr.contains(t._1)).map(_._2)
  }
}