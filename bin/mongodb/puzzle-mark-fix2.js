db.adm_puzzle_mark.update({"mark.subject":"ZhouShuang"},
    {$set:{"mark.subject":"ZhuoShuang"}},
    { multi: true, upsert: false}
)


db.adm_puzzle_mark.find({"mark.subject":"GuoZai"}).forEach((it)=> {
    let subject = it.mark.subject;
    let chessGame = it.mark.chessGame;

    it.mark.chessGame = addElement(chessGame, "GuoZai");
    it.mark.subject = deleteElement(subject, "GuoZai");
    db.adm_puzzle_mark.save(it);
});

db.adm_puzzle_mark.find({"mark.chessGame":"MenSha"}).forEach((it)=> {
    let subject = it.mark.subject;
    let chessGame = it.mark.chessGame;

    it.mark.subject = addElement(subject, "MenSha");
    it.mark.chessGame = deleteElement(chessGame, "MenSha");
    db.adm_puzzle_mark.save(it);
});

function addElement(a, n) {
    a = (a == null || a == undefined) ? [] : a;
    a.push(n);
    return a;
}


function deleteElement(a, n) {
    return a.filter(function(item) {
        return item != n
    });
}