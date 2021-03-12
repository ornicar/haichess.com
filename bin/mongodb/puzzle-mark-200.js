var mk = {
"phase":{"Opening":"开局","MiddleGame":"中局","EndingGame":"残局"},
"moveFor":{"DeZi":"得子","ShaWang":"杀王","QiuHe":"求和","BingShengBian":"兵升变","FangYu":"防守"},
"subject":{"QianZi":"牵制","ZhuoShuang":"捉双","ShuangChongGongJi":"双重攻击","ShanJi":"闪击","ChuanJi":"串击","ShuangJiang":"双将","YinRu":"引入","YinLi":"引离","TouShi":"透视","LanJie":"拦截","TengNuo":"腾挪","DuSe":"堵塞","QuGan":"驱赶","GuoMen":"过门","XiaoChuBaoHu":"消除保护","GuoZai":"过载","BiZouLieZhao":"逼走劣着","GuoDuZhao":"过渡着","DengZhao":"等着"},
"chessGame":{"YiSeGeXiang":"异色格象","TongSeGeXiang":"同色格象","ShuangXiang":"双象","CeYiXiang":"侧翼象","ShuangChe":" 双车","TiChe":"提车","CiDiXianChe":"次底线车","DiXian":"底线","MenSha":"闷杀","BeiKunDeZi":"被困的子","QianShao":"前哨","XuanZi":"悬子","FengChe":"风车","XieXianChongDie":"斜线重叠","ChuiZhiChongDie":"垂直重叠","YiShouGongJiDeWang":"易受攻击的王","NiXiangYiWei":"逆向易位","ZhongXinWang":"中心王","DuiWang":"对王","TianChuang":"天窗","TongLuBing":"通路兵","LianBing":"连兵","DieBing":"叠兵","GuBing":"孤兵"},
"comprehensive":{"JiBenChiZi":"基本吃子","YiBuSha":"一步杀","LiangBuSha":"两步杀","SanBuSha":"三步杀","SiBuYiShangSha":"四步以上杀","JianHua":"简化","BingShengBian":"兵升变","TeSuShengBian":"特殊升变","ChiGuoLuBing":"吃过路兵","ChangJiang":"长将","ChangZhuo":"长捉","QiZiMouHe":"弃子谋和"}};

var phaseMap = mk.phase;
var moveForMap = mk.moveFor;
var subjectMap = mk.subject;
var chessGameMap = mk.chessGame;
var comprehensiveMap = mk.comprehensive;
db.adm_puzzle_mark_200.find().sort({"puzzle.id": 1}).forEach((data)=> {
    var puzzleId = data.puzzle.id;
　　var id = "http://39.105.36.137:18080/marking/" + data.mark.id;
　　var fen = getFen(data.game.treeParts, data.puzzle);
　　var mark = data.mark;
　　var user = mark.user;
    var phase = isEmpty(mark.phase) ? "-" : phaseMap[mark.phase];
    var moveFor = getValues(moveForMap, mark.moveFor);
    var subject = getValues(subjectMap, mark.subject);
    var chessGame = getValues(chessGameMap, mark.chessGame);
    var comprehensive = getValues(comprehensiveMap, mark.comprehensive);
    var tag = isEmpty(mark.tag) ? "-" : mark.tag
　　
　　var r = puzzleId + "\t" + user + "\t" + id + "\t" + fen + "\t" + phase + "\t" + moveFor + "\t" + subject + "\t" + chessGame + "\t" + comprehensive + "\t" + tag
　　print(r);
});


function getFen(treeParts, puzzle) {
    var fen = "";
    for (var i = 0; i < treeParts.length; i++) {
        var part = treeParts[i];
        if (parseInt(puzzle.initialPly) == parseInt(part.ply)) {
            fen = part.fen;
            break;
        }
    }
    return fen;
};

function getValues(m, arr) {
    var v = "";
    if(isEmpty(arr)) {
        return "-";
    }

    arr.map(function(key, i) {
        if(v == "") {
            v = v + m[key]
        } else {
            v = v + "、" + m[key];
        }
    });
    return v;
};

function isEmpty(v) {
    return v == undefined || v == "";
}