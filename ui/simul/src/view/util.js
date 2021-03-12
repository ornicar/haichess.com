var m = require('mithril');

function playerHtml(p) {
  var headSrc = p.head ? '/image/' + p.head : $('body').data('asset-url') + '/assets/images/head-default-64.png';
  var patron = p.patron ? '<i class="patron"></i>' : '';
  var title = p.title ? '<span class="title">' + p.title + '</span>' : '';
  var rating = '<em>' + p.rating + (p.provisional ? '?' : '') + '</em>';
  var uHtml =
      '<a class="user-link online ulpt" href="/@/' + p.username + '">' +
      '<div class="head-line">' +
      '<img class="head" src="' + headSrc + '">' +
      '<i class="line"></i>' +
      '</div>' + title +
      '&nbsp;' + p.username + rating +
      '<span class="badges">' + patron + '</span>' +
      '</a>';
  return uHtml;
/*
  var html = '<a class="text ulpt user-link online" href="/@/' + p.username + '">';
  html += p.patron ? '<i class="line patron"></i>' : '<i class="line"></i>';
  html += (p.title ? p.title + ' ' : '') + p.username;
  if (p.rating) html += '<em>' + p.rating + (p.provisional ? '?' : '') + '</em>';
  html += '</a>';
  return html;*/
}

module.exports = {
  title: function(ctrl) {
    return m('h1', [
      ctrl.data.fullName,
      m('span.author', m.trust(ctrl.trans('by', playerHtml(ctrl.data.host))))
    ]);
  },
  player: function(p) {
    return m.trust(playerHtml(p));
  },
  playerVariant: function(ctrl, p) {
    return ctrl.data.variants.find(function(v) {
      return v.key === p.variant;
    });
  }
};
