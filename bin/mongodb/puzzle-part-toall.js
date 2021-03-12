let id = 208870;
let batch_data = [];
let index = 0
db.adm_puzzle_part3.find({}).forEach((it)=> {
    it._id = NumberInt(it._id);
    it.lines = getLine(it.lines);
    if(it.mark) {
        it.mark.rating = NumberInt(it.mark.rating);
    }
    it.idHistory.part = 3;
    batch_data.push(it);
    index ++;
    if (batch_data.length != 0 && (batch_data.length % 1000 == 0 || index == 78887)) {
        db.adm_puzzle_part.insert(batch_data);
        batch_data = [];
        print("saved " + index)
    }
    id = id + 1;
});

//------------------------------------------------------
let batch_data = [];
let index = 0;
db.puzzle.find({}).forEach((it) => {
    it._id = NumberInt(it._id);
    it.lines = getLine(it.lines);
    if(it.mark) {
        it.mark.rating = NumberInt(it.mark.rating);
    }

    batch_data.push(it);
    index++;
    if (batch_data.length != 0 && (batch_data.length % 1000 == 0 || index == 100661)) {
        db.puzzle.insert(batch_data);
        batch_data = [];
        print("saved " + index)
    }
});


function getLine(oldLines) {
    var lineStr = JSON.stringify(oldLines);
    lineStr = lineStr.replace(new RegExp("\"win\"","gm"), "true");
    lineStr = lineStr.replace(new RegExp("\"retry\"","gm"), "false");
    return JSON.parse(lineStr);
}

//------------------------------------------------------
let gameIds = [];
let gameIds_ = [];
let all = []
let i = 0
db.puzzle.find({}).forEach((it)=> {
    gameIds.push(it.gameId);
    i++;
    if (gameIds.length != 0 && (gameIds.length % 1000 == 0 || i == 100661)) {
        db.game5.find({
            _id: {
                $in: gameIds
            }
        }).forEach((g)=> {
            gameIds_.push(g._id);
        });
        all.push(getArrDifference(gameIds, gameIds_));
        gameIds = [];
        gameIds_ = [];
        print(i + "->" + all)
    };

});

function getArrDifference(arr1, arr2) {
    return arr1.concat(arr2).filter(function(v, i, arr) {
        return arr.indexOf(v) === arr.lastIndexOf(v);
    });
}

//------------------------------------------------------
let id = 1;
db.adm_puzzle_checkmate.find({}).forEach(function(obj) {
    var data = {};
    data._id = NumberInt(id);
    data.idHistory = {};
    data.idHistory.system = obj._id;
    data.gameId = obj.gameId;
    data.history = obj.history;
    data.fen = obj.fen;
    data.lines = obj.lines;
    data.depth = obj.depth;
    data.white = obj.white;
    data.date = new Date();
    data.perf = {};
    data.perf.gl = {};
    data.perf.gl.r = getRating(obj);
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
    data.mate = obj.mate;
    data.mark = obj.mark;
    data.mark.id = NumberInt(id);
    data.mark.source = "haichess";
    data.mark.rating = getRating(obj);

    //printjson(data);
    db.adm_puzzle_part1.save(data);
    id ++;
});

function getRating(obj) {
    if(obj.strategy == 'checkmate1') {
        return NumberInt(700);
    } else if(obj.strategy == 'checkmate2') {
        return NumberInt(700);
    }
}

let shuffle = ([...arr]) => {
    let m = arr.length;
    while (m) {
        const i = Math.floor(Math.random() * m--);
        [arr[m], arr[i]] = [arr[i], arr[m]];
    }
    return arr;
};
