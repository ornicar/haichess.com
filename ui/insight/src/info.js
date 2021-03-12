var m = require('mithril');

var shareStates = ["不分享", "和朋友分享", "和每个人分享"];

module.exports = function(ctrl) {
  var shareText = shareStates[ctrl.user.shareId] + '我的洞察数据';
  return m('div.info.box', [
    m('div.top', [
      m('a.help', {
        title: '如何使用？',
        onclick: lichess.startInsightTour
      }, '?'),
      m('a.username.user-link.insight-ulpt', {
        href: '/@/' + ctrl.user.name
      }, ctrl.user.name)
    ]),
    m('div.content', [
      m('p', [
        '数据基于 ',
        m('strong', ctrl.user.nbGames),
        ' 盘有积分的对局'
      ]),
      m('p.share', ctrl.own ? m('a', {
        href: '/account/preferences/privacy',
        target: '_blank'
      }, shareText) : shareText)
    ]),
    m('div.refresh', {
      config: function(e, isUpdate) {
        if (isUpdate) return;
        var $ref = $('.insight-stale');
        if ($ref.length) {
          $(e).html($ref.show());
          lichess.refreshInsightForm();
        }
      }
    })
  ]);
};
