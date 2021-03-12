var m = require('mithril');

function fMap(v, f, orDefault) {
  return v ? f(v) : (orDefault || null);
}

function gameLink(id, content) {
  return m('a', {
    class: 'glpt',
    href: '/' + id
  }, content);
}

function absDate(d) {
  return m('time', window.lichess.timeago.absolute(d));
}

function fromTo(s) {
  return fMap(s.from, function(r) {
    return [
      '自 ',
      gameLink(r.gameId, absDate(r.at)),
      ' 至 ',
      fMap(s.to, function(r) {
        return gameLink(r.gameId, absDate(r.at));
      }, '现在')
    ];
  }, m.trust('&nbsp;'));
}

module.exports = {
  fMap: fMap,
  gameLink: gameLink,
  date: absDate,
  showUser: function(u, rating) {
    return m('a', {
      class: 'ulpt',
      href: '/@/' + u.name
    }, (u.title ? (u.title + ' ') : '') + u.name + ' (' + rating + ')');
  },
  noData: '没有足够的对局.',
  fromTo: fromTo,
  streaks: function(s, f) {
    return [
      m('div.streak', [
        m('h3', '最长: '), f(s.max)
      ]),
      m('div.streak', [
        m('h3', '当前'), f(s.cur)
      ])
    ];
  },
  green: function(v) {
    return m('green', v);
  },
  red: function(v) {
    return m('red', v);
  },
  identity: function(v) {
    return v;
  },
  formatSeconds: function(s, format) {
    var hours = Math.floor(s / 3600);
    var minutes = Math.floor((s % 3600) / 60);
    if (format === 'short') return hours + '小时, ' + minutes + '分钟';
    return hours + ' 小时, ' + minutes + ' 分钟';
  }
};
