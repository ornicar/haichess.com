let arr_marked = [];
db.L2_puzzle_mark.find({
    "mark.markStatus": "Marked",
    "mark.validStatus": "Save"
}).forEach(function(obj) {
    arr_marked.push(obj);
});
db.L3_puzzle_mark_marked.save(arr_marked);


/* ---------------------------------- */

let arr_unmarked = [];
db.L2_puzzle_mark.find({
    $or: [{
        "mark.markStatus": "Unmarked"
    }, {
        "mark.validStatus": "Problem"
    }]
}).forEach(function(obj) {
    arr_unmarked.push(obj);
});
db.L3_puzzle_mark_unmarked.save(arr_unmarked);