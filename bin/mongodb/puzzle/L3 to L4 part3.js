
let part3 = [];
db.L3_puzzle_geneate_checkmate3.find({}).forEach(function(obj) {
    part3.push(obj);
});
db.L1_puzzle_unmark.find({}).forEach(function(obj) {
    part3.push(obj);
});


let shuffle = ([...arr]) => {
    let m = arr.length;
    while (m) {
        const i = Math.floor(Math.random() * m--);
        [arr[m], arr[i]] = [arr[i], arr[m]];
    }
    return arr;
};
part3 = shuffle(part3);


let id = 143018;
let index = 0;
let batch_data = [];
part3.forEach(function(obj) {
    obj._id = NumberInt(id);
    batch_data.push(obj);
    index ++;
    if (batch_data.length != 0 && (batch_data.length % 1000 == 0 || index == part3.length)) {
        db.L4_puzzle_part3.insert(batch_data);
        batch_data = [];
        print("saved " + index)
    }
    id = id + 2;
});