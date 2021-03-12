let puzzles = db.puzzle;

puzzles.find({mark: {$exists:true}}).forEach(function(p) {
    puzzles.update({
        _id: p._id
    }, {
        $set: {
            "mark.id": NumberInt(parseInt(p.mark.id))
        }
    });
});