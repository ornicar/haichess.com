
let part2 = [];
db.L3_puzzle_geneate_checkmate2.find({}).forEach(function(obj) {
    part2.push(obj);
});
db.L3_puzzle_mark_unmarked.find({}).forEach(function(obj) {
    part2.push(obj);
});


let shuffle = ([...arr]) => {
    let m = arr.length;
    while (m) {
        const i = Math.floor(Math.random() * m--);
        [arr[m], arr[i]] = [arr[i], arr[m]];
    }
    return arr;
};
part2 = shuffle(part2);


let id = 121852;
let index = 0;
let batch_data = [];
part2.forEach(function(obj) {
    obj._id = NumberInt(id);
    batch_data.push(obj);
    index ++;
    if (batch_data.length != 0 && (batch_data.length % 1000 == 0 || index == part2.length)) {
        db.L4_puzzle_part2.insert(batch_data);
        batch_data = [];
        print("saved " + index)
    }
    id = id + 2;
});