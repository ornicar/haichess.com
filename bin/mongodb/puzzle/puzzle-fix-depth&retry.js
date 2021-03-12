function depth(ln) {
    let toKey = function (branch) {
        let key = '';
        for (let k of branch) {
            key = key + '/' + k
        }
        return key;
    };

    let branchs = new Map();
    let depthOf = function (parent, lines) {
        for (let key in lines) {
            let newBranch = [];
            Object.assign(newBranch, parent);
            newBranch.push(key);
            branchs.set(toKey(newBranch), newBranch);
            depthOf(newBranch, lines[key])
        }
        // 叶子节点
        if(typeof(lines) == 'boolean') {
            if(!lines) {
                branchs.delete(toKey(parent));
            }
        }

        if (Object.keys(lines).length > 0) {
            branchs.delete(toKey(parent));
        }

    };

    depthOf([], ln);
    let branchsArray = [...branchs.values()];
    return (Math.ceil((branchsArray[0].length) / 2));
}

function retry(ln) {
    var lineStr = JSON.stringify(ln);
    return lineStr.indexOf('false') > 0;
}

db.puzzle.find({"idHistory.source": {$in : ["lichess-mark", "lichess-unmark"]}, "depth": {$gt: 1}}).forEach(function(p) {
    let _depth = depth(p.lines);
    let _retry = retry(p.lines);
    db.puzzle.update({
        _id: p._id
    }, {
        $set: {
            depth: NumberInt(_depth),
            retry: _retry
        }
    });
});