
let part1 = [];
db.L3_puzzle_geneate_draw.find({}).forEach(function(obj) {
    part1.push(obj);
});
db.L3_puzzle_geneate_checkmate1.find({}).forEach(function(obj) {
    part1.push(obj);
});
db.L3_puzzle_mark_marked.find({}).forEach(function(obj) {
    part1.push(obj);
});


let shuffle = ([...arr]) => {
    let m = arr.length;
    while (m) {
        const i = Math.floor(Math.random() * m--);
        [arr[m], arr[i]] = [arr[i], arr[m]];
    }
    return arr;
};
part1 = shuffle(part1);


let id = 100000;
let index = 0;
let batch_data = [];
part1.forEach(function(obj) {
    obj._id = NumberInt(id);
    batch_data.push(obj);
    index ++;
    if (batch_data.length != 0 && (batch_data.length % 1000 == 0 || index == part1.length)) {
        db.L4_puzzle_part1.insert(batch_data);
        batch_data = [];
        print("saved " + index)
    }
    id = id + 2;
});