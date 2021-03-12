let crawl = db.adm_puzzle_crawl;

let batch_data = [];
let index = 0;
crawl.find().forEach(function (obj) {
    let data = obj.puzzle_json.data;
    data._id = obj._id;

    let mark = {};
    mark.id = obj._id;
    mark.source = "Lichess";
    mark.markStatus = "Unmarked";
    mark.user = "";
    mark.rating = data.puzzle.rating;
    data.mark = mark;

    //printjson(data);
    //print("-------------------------------------------");
    batch_data.push(data);
    if (batch_data.length == 2000 || index == 89990) {
        db.adm_puzzle_mark_all_new.insert(batch_data);
        batch_data = [];
        print("saved：" + (index + 1));
    }
    index++;
});