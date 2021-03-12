var m = require('mithril');

function provisional() {
  return m('span', {
    title: '没有进行足够的积分游戏来建立可靠的分级.'
  }, '(临时)');
}

function percentile(d) {
  return d.percentile === 0 ? '' : [
    ' 超过 ',
    m('a', {
      href: '/stat/rating/distribution/' + d.stat.perfType.key
    }, [
      m('strong', d.percentile + '%'),
      ' 的 ' + d.stat.perfType.name + ' 棋手'
    ])
  ];
}

function progress(p) {
  if (p > 0) return m('green[data-icon=N]', p);
  else if (p < 0) return m('red[data-icon=M]', -p);
}

module.exports = function(d) {
  return [
    m('h2', [
      '积分: ',
      m('strong', {
        title: '是的，积分有小数精度.'
      }, d.perf.glicko.rating),
      '. ',
      m('span.details', d.perf.glicko.provisional ? provisional() : percentile(d))
    ]),
    m('p', [
      '最新 12 局后的变化： ',
      m('span.progress', progress(d.perf.progress) || '无'),
      '. ',
      '积分偏差: ',
      m('strong', {
        title: '偏差越低，表示积分越稳定。偏差高于 110，积分将会被评为暂定。'
      }, d.perf.glicko.deviation),
      '. '
    ])
  ];
};
