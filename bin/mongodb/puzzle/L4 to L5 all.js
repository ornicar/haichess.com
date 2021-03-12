let all = [];
db.L4_puzzle_part1.find({}).forEach(function(obj) {
    all.push(obj);
});
db.L4_puzzle_part2.find({}).forEach(function(obj) {
    all.push(obj);
});
db.L4_puzzle_part3.find({}).forEach(function(obj) {
    all.push(obj);
});

let index = 0;
let batch_data = [];
all.forEach(function(obj) {
    batch_data.push(obj);
    index ++;
    if (batch_data.length != 0 && (batch_data.length % 2000 == 0 || index == all.length)) {
        db.L5_puzzle_all.insert(batch_data);
        batch_data = [];
        print("saved " + index)
    }
});