function getLine(oldLines) {
    var lineStr = JSON.stringify(oldLines);
    lineStr = lineStr.replace(new RegExp("\"win\"","gm"), "true");
    lineStr = lineStr.replace(new RegExp("\"retry\"","gm"), "false");
    return JSON.parse(lineStr);
}

db.puzzle.find().forEach(function(p) {
    db.puzzle.update({
        _id: p._id
    }, {
        $set: {
            "lines": getLine(p.lines)
        }
    });
});