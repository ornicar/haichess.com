var m = require('mithril');
var util = require('./util');

function streak(s, title, display) {
  return m('div.streak', [
    m('h3', [
      title + ': ',
      display(s.v)
    ]),
    util.fromTo(s)
  ]);
}

function streaks(s, display) {
  return m('div.split', [
    m('div', streak(s.max, '最长记录', display)),
    m('div', streak(s.cur, '当前记录', display))
  ]);
}

var lessThan = '棋局间隔一小时以内.';

module.exports = {
  nb: function(d) {
    return util.fMap(d.stat.playStreak.nb, function(s) {
      return [
        m('h2', m('span', {
          title: lessThan
        }, '连续对局')),
        streaks(s, function(v) {
          return v ? [
            m('strong', v),
            ' game' + (v > 1 ? 's' : '')
          ] : '无';
        })
      ];
    });
  },
  time: function(d) {
    return util.fMap(d.stat.playStreak.time, function(s) {
      return [
        m('h2', m('span', {
          title: lessThan
        }, '最长连续对局时间')),
        streaks(s, util.formatSeconds)
      ];
    });
  }
};
