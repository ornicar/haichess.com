var m = require('mithril');
var util = require('./util');

function ratingAt(title, opt, color) {
  return util.fMap(opt, function(r) {
    return [
      m('h2', [title + ': ', m('strong', color(r.int))]),
      util.gameLink(r.gameId, util.date(r.at))
    ];
  }, [
    m('h2', title + ': '),
    m('span', util.noData)
  ]);
}

module.exports = function(d) {
  return [
    m('div', ratingAt('最高积分', d.stat.highest, util.green)),
    m('div', ratingAt('最低积分', d.stat.lowest, util.red))
  ];
};
