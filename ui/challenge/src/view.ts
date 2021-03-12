import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Chessground } from 'chessground';
import { Ctrl, Challenge, ChallengeData, ChallengeDirection, ChallengeUser, TimeControl } from './interfaces'

export function loaded(ctrl: Ctrl): VNode {
  return ctrl.redirecting() ?
  h('div#challenge-app.dropdown', h('div.initiating', spinner())) :
  h('div#challenge-app.links.dropdown.rendered', renderContent(ctrl));
}

export function loading(): VNode {
  return h('div#challenge-app.links.dropdown.rendered', [
    h('div.empty.loading', '-'),
    create()
  ]);
}

function renderContent(ctrl: Ctrl): VNode[] {
  let d = ctrl.data();
  const nb = d.in.length + d.out.length;
  return nb ? [allChallenges(ctrl, d, nb)] : [
    empty(),
    create()
  ];
}

function userPowertips(vnode: VNode) {
  window.lichess.powertip.manualUserIn(vnode.elm);
}

function allChallenges(ctrl: Ctrl, d: ChallengeData, nb: number): VNode {
  return h('div.challenges', {
    class: { many: nb > 3 },
    hook: {
      insert: userPowertips,
      postpatch: userPowertips
    }
  }, d.in.map(challenge(ctrl, 'in')).concat(d.out.map(challenge(ctrl, 'out'))));
}

function challenge(ctrl: Ctrl, dir: ChallengeDirection) {
  return (c: Challenge) => {
    return h('div.challenge.' + dir + '.c-' + c.id, {
      class: {
        declined: !!c.declined,
        hasBoard: c.variant.key === 'fromPosition',
        hasAppt: c.appt
      }
    }, [
      h('div.content', [
        h('span.head', renderUser(dir === 'in' ? c.challenger : c.destUser)),
        h('span.desc', [
          ctrl.trans()(c.rated ? 'rated' : 'casual'),
          timeControl(c.timeControl),
          c.variant.name
        ].join(' • ')),
        c.appt ? h('div.appt', [
          h('strong', '预约：'),
          c.apptStartsAt
        ]) : null
      ]),
      h('i', {
        attrs: {'data-icon': c.perf.icon}
      }),
      c.variant.key === 'fromPosition' ? h('div.mini-board.cg-wrap.is2d', {
        hook: {
          insert: vnode => Chessground((vnode.elm as HTMLElement), {
            coordinates: false,
            drawable: { enabled: false, visible: false },
            orientation: dir === 'out' ? c.color : (c.color === 'white' ? 'black': 'white'),
            resizable: false,
            viewOnly: true,
            fen: c.initialFen
          })
        }
      }) : null,

      h('div.buttons', (dir === 'in' ? inButtons : outButtons)(ctrl, c))
    ]);
  };
}

function inButtons(ctrl: Ctrl, c: Challenge): VNode[] {
  const trans = ctrl.trans();
  return [
    h('form', {
      attrs: {
        method: 'post',
        action: c.appt ? `/appt/${c.id}/accept` : `/challenge/${c.id}/accept`
      }
    }, [
      c.appt && c.apptExpired ? h('a.button.expired', {
        attrs: {
          'data-icon': 'm',
          href: '/appt/' + c.id + '/form',
          title: '改时间'
        }
      }): h('button.button.accept', {
        attrs: {
          'type': 'submit',
          'data-icon': 'E',
          title: trans('accept')
        },
        hook: onClick(ctrl.onRedirect)
      })]),
    h('button.button.decline', {
      attrs: {
        'type': 'submit',
        'data-icon': 'L',
        title: trans('decline')
      },
      hook: onClick(() => ctrl.decline(c.id))
    })
  ];
}

function outButtons(ctrl: Ctrl, c: Challenge) {
  const trans = ctrl.trans();
  return [
    h('div.owner', [
      h('span.waiting', ctrl.trans()('waiting')),
      h('a.view', {
        attrs: {
          'data-icon': 'v',
          href: '/' + c.id,
          title: trans('viewInFullSize')
        }
      })
    ]),
    h('button.button.decline', {
      attrs: {
        'data-icon': 'L',
        title: trans('cancel')
      },
      hook: onClick(() => ctrl.cancel(c.id))
    })
  ];
}

function timeControl(c: TimeControl): string {
  switch (c.type) {
    case 'unlimited':
      return '无限时间';
    case 'correspondence':
      return c.daysPerTurn + '天';
    case 'clock':
      return c.show || '-';
  }
}

/*function renderUser(u?: ChallengeUser): VNode {
  if (!u) return h('span', 'Open challenge');
  const rating = u.rating + (u.provisional ? '?' : '');
  return h('a.ulpt.user-link', {
    attrs: { href: `/@/${u.name}`},
    class: { online: !!u.online }
  }, [
    h('i.line' + (u.patron ? '.patron' : '')),
    h('name', [
      u.title && h('span.title', u.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, u.title + ' '),
      u.name + ' (' + rating + ') '
    ]),
      h('signal', u.lag === undefined ? [] : [1, 2, 3, 4].map((i) => h('i', {
        class: { off: u.lag! < i}
      })))
  ]);
}*/

function renderUser(u?: ChallengeUser): VNode {
  if (!u) return h('span', '打开挑战');
  const rating = u.rating + (u.provisional ? '?' : '');
  const head = u.head ? '/image/' + u.head : $('body').data('asset-url') + '/assets/images/head-default-64.png';
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
    u.name + ' (' + rating + ') ',
    h('span.badges', [
      u.patron ? h('i.patron') : null
    ]),
    h('signal', u.lag === undefined ? [] : [1, 2, 3, 4].map((i) => h('i', {
      class: { off: u.lag! < i}
    })))
  ]);
}

function create(): VNode {
  return h('a.create', {
    attrs: {
      href: '/lobby?any#friend',
      'data-icon': 'O'
    },
    title: 'Challenge someone'
  });
}

function empty(): VNode {
  return h('div.empty.text', {
    attrs: {
      'data-icon': '',
    }
  }, '没有挑战.');
}

function onClick(f: (e: Event) => void) {
  return {
    insert: (vnode: VNode) => {
      (vnode.elm as HTMLElement).addEventListener('click', f);
    }
  };
}

function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
