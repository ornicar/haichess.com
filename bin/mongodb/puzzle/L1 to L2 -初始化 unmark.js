db.L1_puzzle_unmark.find().forEach(function(p) {
    let r = 600;
    if(p.perf.gl.r > 700) {
        r = p.perf.gl.r - 100;
    }
    db.L1_puzzle_unmark.update({
        _id: p._id
    }, {
        $set: {
            "idHistory.source": "lichess-unmark",
            "attempts": NumberInt(99),
            "perf.nb": NumberInt(1),
            "perf.gl.r": r,
            "perf.gl.d": 150,
            "perf.gl.v": 0.06
        },
        $unset: {
            "idHistory.part": ""
        }
    });
});