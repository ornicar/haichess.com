db.puzzle_mark.find({"mark.validStatus": "Save"}).forEach((it) => {
  let obj = it;
  let id = NumberInt(parseInt(obj._id));
  let _mark = it.mark;
  _mark.publishStatus = "Published",
      _mark.id = id

  db.puzzle.update({
    "mark.id": id
  }, {
    $set: {
      mark: _mark
    }
  })
});


db.puzzle.update(
    {"idHistory.source": {$in: ["checkmate1", "checkmate2", "checkmate3", "draw_changjiang", "draw_qizi"]}},
    { $set: { "perf.gl.d": 100 } },
    { multi: true }
)