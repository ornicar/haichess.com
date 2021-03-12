db.adm_puzzle_mark.find({
    "mark.markStatus": "Marked",
    "mark.validStatus": "Save"
}).forEach(function(obj) {
    var game_ = obj.game;
    var puzzle_ = obj.puzzle;
    var mark_ = obj.mark;

    var data = {};
    data._id = puzzle_.id;
    data.idHistory = {};
    data.idHistory.mark = obj._id;
    data.idHistory.lichess = puzzle_.id;
    data.gameId = game_.id;
    data.history = getHistory(game_.treeParts, puzzle_);
    data.fen = puzzle_.fen;
    data.lines = getLine(puzzle_.lines);
    data.depth = getDepth(data.lines);
    data.white = (puzzle_.color === "white") ? true : false;
    data.date = new Date();
    data.perf = {};
    data.perf.gl = {};
    data.perf.gl.r = parseFloat(puzzle_.rating);
    data.perf.gl.d = 350.0;
    data.perf.gl.v = 0.06;
    data.perf.nb = NumberInt(1);
    data.vote = {};
    data.vote.up = NumberInt(1);
    data.vote.down = NumberInt(0);
    data.vote.nb = NumberInt(1);
    data.vote.ratio = NumberInt(100);
    data.attempts = NumberInt(99);
    data.wins = NumberInt(0);
    data.time = NumberInt(0);
    data.mate = getMate(data.lines);
    data.mark = mark_;
    data.mark.id = NumberInt(parseInt(mark_.id));

    //printjson(data);
    db.puzzle.save(data);
});

function getHistory(treeParts, puzzle) {
    var history = "";
    for (var i = 0; i < treeParts.length; i++) {
        var part = treeParts[i];
        if (parseInt(puzzle.initialPly) == parseInt(part.ply)) {
            history = part.uci;
            break;
        }
    }
    return history;
}

function getLine(oldLines) {
    var piotr = {
        'A1': 'a', 'B1': 'b', 'C1': 'c', 'D1': 'd', 'E1': 'e', 'F1': 'f', 'G1': 'g', 'H1': 'h',
        'A2': 'i', 'B2': 'j', 'C2': 'k', 'D2': 'l', 'E2': 'm', 'F2': 'n', 'G2': 'o', 'H2': 'p',
        'A3': 'q', 'B3': 'r', 'C3': 's', 'D3': 't', 'E3': 'u', 'F3': 'v', 'G3': 'w', 'H3': 'x',
        'A4': 'y', 'B4': 'z', 'C4': 'A', 'D4': 'B', 'E4': 'C', 'F4': 'D', 'G4': 'E', 'H4': 'F',
        'A5': 'G', 'B5': 'H', 'C5': 'I', 'D5': 'J', 'E5': 'K', 'F5': 'L', 'G5': 'M', 'H5': 'N',
        'A6': 'O', 'B6': 'P', 'C6': 'Q', 'D6': 'R', 'E6': 'S', 'F6': 'T', 'G6': 'U', 'H6': 'V',
        'A7': 'W', 'B7': 'X', 'C7': 'Y', 'D7': 'Z', 'E7': '0', 'F7': '1', 'G7': '2', 'H7': '3',
        'A8': '4', 'B8': '5', 'C8': '6', 'D8': '7', 'E8': '8', 'F8': '9', 'G8': '!', 'H8': '?'
    };

    var lineStr = JSON.stringify(oldLines);
    for (var cell in piotr) {
        lineStr = lineStr.replace(new RegExp(cell.toLowerCase(),"gm"), piotr[cell]);
    }
    lineStr = lineStr.replace( "\"win\"", "true");
    return JSON.parse(lineStr);
}

function getDepth(lines) {
    var num = getNounNum(lines);
    return NumberInt(Math.ceil(num / 2));
}

function getMate(lines) {
    var num = getNounNum(lines);
    return num % 2 == 0 ? false : true;
}

function getNounNum(lines) {
    var lineStr = JSON.stringify(lines);
    lineStr = lineStr.replace(new RegExp(":\"retry\"","gm"),"");
    return lineStr.split(":").length - 1;
}
