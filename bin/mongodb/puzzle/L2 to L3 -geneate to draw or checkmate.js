let shuffle = ([...arr]) => {
    let m = arr.length;
    while (m) {
        const i = Math.floor(Math.random() * m--);
        [arr[m], arr[i]] = [arr[i], arr[m]];
    }
    return arr;
};

let checkmate = [];
db.L2_puzzle_geneate.find({
    "idHistory.source": mb.regex.startsWith("checkmate")
}).forEach(function(obj) {
    checkmate.push(obj);
});
checkmate = shuffle(checkmate);

let part1 = checkmate.slice(0, 5000);
let part2 = checkmate.slice(5000, 10000);
let part3 = checkmate.slice(10000);

db.L3_puzzle_geneate_checkmate1.save(part1);
db.L3_puzzle_geneate_checkmate2.save(part2);
db.L3_puzzle_geneate_checkmate3.save(part3);

/* ------------------------------------------ */

let draw = [];
db.L2_puzzle_geneate.find({
    "idHistory.source": mb.regex.startsWith("draw")
}).forEach(function(obj) {
    draw.push(obj);
});

draw = shuffle(draw);
db.L3_puzzle_geneate_draw.save(draw);

