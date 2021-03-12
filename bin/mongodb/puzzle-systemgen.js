let id = 244212;

db.puzzle_draw.find().forEach(function(obj) {
    var puzzle_ = obj;
    var mark_ = puzzle_.mark;

    var data = {};
    data._id = NumberInt(id);
    data.idHistory = {};
    data.idHistory.system = puzzle_._id;
    data.gameId = puzzle_.gameId;
    data.history = puzzle_.history;
    data.fen = puzzle_.fen;
    data.lines = puzzle_.lines;
    data.depth = NumberInt(puzzle_.depth);
    data.white = puzzle_.white;
    data.date = new Date();
    data.perf = {};
    data.perf.gl = {};
    data.perf.gl.r = 1300.0;
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
    data.mate = puzzle_.mate;
    data.mark = mark_;
    data.mark.id = NumberInt(1);
    data.mark.rating = NumberInt(mark_.rating);


    //printjson(data);
    db.puzzle.save(data);
    id = id + 3;
});
