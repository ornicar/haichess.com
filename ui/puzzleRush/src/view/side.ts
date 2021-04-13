import { h } from 'snabbdom';
import { Controller } from '../interfaces';
import { bind, spinner, assetsUrl } from "../util";

export default function side(ctrl: Controller) {
  return h('div.puzzleRush__side', [
    userInfo(ctrl),
    rankingHeader(ctrl),
    rankingContent(ctrl)
  ]);
}

function userInfo(ctrl: Controller) {
  let rankData = ctrl.vm.rankData;
  let userTdyRank = rankData.userTdyRank ? rankData.userTdyRank : {};
  let userHisRank = rankData.userHisRank ? rankData.userHisRank : {};
  let head = ctrl.user.head ? '/image/' + ctrl.user.head : assetsUrl+ '/images/head-default-64.png';
  return h('div.user', [
    h('table.user-header', [
      h('tr', [
        h('td', {attrs: { rowspan: 2 }}, [
          h('img.header', {attrs: {src: head}})
        ]),
        h('td', [
          h('a.user-link.ulpt',{ attrs: { href: '/@/' + ctrl.user.username }}, ctrl.user.username)
        ]),
        h('td', [
          h('strong.today', '今日最佳')
        ]),
        h('td', [
          h('strong.history', '历史最佳')
        ])
      ]),
      h('tr', [
        h('td'),
        h('td', [
          h('span.best.today', userTdyRank.score == -1 ? '--' : userTdyRank.score)
        ]),
        h('td', [
          h('span.best.history', userHisRank.score == -1 ? '--' : userHisRank.score)
        ])
      ]),
      h('tr', [
        h('td'),
        h('td'),
        h('td', [
          h('span.no.today', userTdyRank.no == -1 ? '' : userTdyRank.no + 'th')
        ]),
        h('td', [
          h('span.no.history', userHisRank.no == -1 ? '' : userHisRank.no + 'th')
        ])
      ])
    ])
  ])
}

function rankingHeader(ctrl: Controller) {
  const scopes = [{k: 'country', n: '全国'}, {k: 'level', n: '同级'}, {k: 'friend', n: '好友'}, {k: 'personal', n: '个人'}];
  return h('div.ranking-header.number-menu.number-menu--tabs',
      scopes.map(function (data) {
        return h('a.nm-item', {
          class: {
            active: ctrl.vm.rankScope === data.k
          },
          attrs: {
            'data-tab': data.k
          },
          hook: bind('click', e => {
            e.stopPropagation();
            ctrl.vm.rankScope = data.k;
            ctrl.loadRank();
          })
        }, data.n)
      })
  )
}

function rankingContent(ctrl: Controller) {
  return h('div.ranking-content', [
    h('table.range', [
      h('tr', [
        h('td', [
          h('span.group', [
            h('input', {
              attrs: {
                type: 'radio', name: 'date', id: 'rk-rd-today', value: 'today'
              },
              hook: bind('click', e => {
                e.stopPropagation();
                ctrl.vm.rankRange = 'today';
                ctrl.loadRank();
              })
            }),
            h('label', { attrs: { for: 'rk-rd-today' }}, '今日')
          ]),
          h('span.group', [
            h('input', {
              attrs: {
                type: 'radio', name: 'date', id: 'rk-rd-season', checked: true, value: 'season'
              },
              hook: bind('click', e => {
                e.stopPropagation();
                ctrl.vm.rankRange = 'season';
                ctrl.loadRank();
              })
            }),
            h('label', { attrs: { for: 'rk-rd-season' }}, '本月')
          ]),
          h('span.group', [
            h('input', {
              attrs: {
                type: 'radio', name: 'date', id: 'rk-rd-history', value: 'history'
              },
              hook: bind('click', e => {
                e.stopPropagation();
                ctrl.vm.rankRange = 'history';
                ctrl.loadRank();
              })
            }),
            h('label', { attrs: { for: 'rk-rd-history' }}, '历史')
          ])
        ]),
        h('td', [
          ctrl.vm.rankScope === 'personal' ? h('a.whole', { attrs: { href: '/training/rush/mine?page=1&mode=' + ctrl.vm.mode + '&order=endTime'} }, '全部记录') : null
        ])
      ])
    ]),
    h('div.list', [
      ctrl.vm.rankLoading ? spinner() : h('div', [
        (ctrl.vm.rankScope === 'personal' || !ctrl.vm.rankData.userRank || ctrl.vm.rankData.userRank.no <= 10) ? null : h('table', renderUserTable(ctrl)),
        h('table', renderTable(ctrl))
      ])
    ])
  ])
}

function renderUserTable(ctrl: Controller) {
  let rankData = ctrl.vm.rankData;
  let rankList = rankData.userRank ? [rankData.userRank] : [];
  return rankList.map(function (r) {
    return renderTr(ctrl, r);
  });
}

function renderTable(ctrl: Controller) {
  let rankData = ctrl.vm.rankData;
  let rankList = rankData.rankList ? rankData.rankList : [];
  return rankList.map(function (r) {
    return renderTr(ctrl, r);
  });
}

function renderTr(ctrl: Controller, rank) {
  return h('tr', {
    class: {
      mine: rank.user && rank.user.name === ctrl.user.username
    }
  }, [
    h('td', [
      h('span.no', '#' + rank.no)
    ]),
    h('td', [
      ctrl.vm.rankScope === 'personal' ? h('a.time', { attrs: { href: '/training/rush/' + rank.id} }, rank.time) : renderUser(rank.user)
    ]),
    h('td', [
      h('span.score', rank.score)
    ])
  ])
}

/*
function renderUser(u) {
  return h('a.ulpt.user-link', {
    attrs: { href: `/@/${u.name}`},
    class: { online: !!u.online }
  }, [
    h('i.line' + (u.patron ? '.patron' : '')),
    h('name', [
      u.title && h('span.title', u.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, u.title + ' '),
      u.name
    ])
  ]);
}
*/

function renderUser(u) {
  let head = u.head ? '/image/' + u.head : $('body').data('asset-url') + '/assets/images/head-default-64.png';
  return h('a.user-link.ulpt', {
    attrs: { href: `/@/${u.name}`},
    class: { online: !!u.online, offline: !u.online }
  }, [
    h('div.head-line', [
      h('img.head', { attrs: { src: head }}),
      h('i.line')
    ]),
    h('i.line' + (u.patron ? '.patron' : '')),
    u.title ? h('span.title', u.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, u.title) : null,
    u.name,
    h('span.badges', [
      u.patron ? h('i.patron') : null
    ])
  ]);
}