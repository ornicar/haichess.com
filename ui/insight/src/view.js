var m = require('mithril');
var axis = require('./axis');
var filters = require('./filters');
var presets = require('./presets');
var chart = require('./chart');
var table = require('./table');
var help = require('./help');
var info = require('./info');
var boards = require('./boards');

function cache(view, dataToKey) {
  var prev = null;
  return function(data) {
    var key = dataToKey(data);
    if (prev === key) return {
      subtree: "retain"
    };
    prev = key;
    return view(data);
  };
}

var renderMeat = cache(function(ctrl) {
  let assetUrl = $('body').data('asset-url') + '/assets/';
  if (ctrl.vm.broken) return m('div.broken', [
    m('img', {
      src: assetUrl + 'images/insight_broken.jpg'
    })
  ]);
  if (!ctrl.vm.answer) return;
  return m('div', [
    chart(ctrl),
    table.vert(ctrl),
    boards(ctrl)
  ]);
}, function(ctrl) {
  var q = ctrl.vm.answer ? ctrl.vm.answer.question : null;
  return q ? ctrl.makeUrl(q.dimension, q.metric, q.filters) : ctrl.vm.broken;
});

module.exports = function(ctrl) {
  return m('div', {
    class: ctrl.vm.loading ? 'loading' : 'ready'
  }, [
    m('div.left-side', [
      info(ctrl),
      m('div.panel-tabs', [
        m('a[data-panel="preset"]', {
          class: 'tab preset' + (ctrl.vm.panel === 'preset' ? ' active' : ''),
          onclick: function() {
            ctrl.setPanel('preset');
          }
        }, '预置'),
        m('a[data-panel="filter"]', {
          class: 'tab filter' + (ctrl.vm.panel === 'filter' ? ' active' : ''),
          onclick: function() {
            ctrl.setPanel('filter');
          }
        }, '过滤器'), Object.keys(ctrl.vm.filters).length ? m('a.clear', {
          title: '清除所有条件',
          'data-icon': 'L',
          onclick: ctrl.clearFilters
        }, '清除') : null,
      ]),
      ctrl.vm.panel === 'filter' ? filters(ctrl) : null,
      ctrl.vm.panel === 'preset' ? presets(ctrl) : null,
      help(ctrl)
    ]),
    m('header', [
      axis(ctrl),
      m('h2', {
        class: 'text',
        'data-icon': '7'
      }, '数据洞察')
    ]),
    m('div.meat.box', renderMeat(ctrl))
  ]);
};
