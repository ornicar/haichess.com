import { h, thunk } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import {bind, dataIcon} from '../util';
import { Controller } from '../interfaces';

export function puzzleBox(ctrl: Controller) {
  var data = ctrl.getData();
  return h('div.puzzle__side__metas', [
    capsuleInfos(data.capsule),
    homeworkInfos(data.homework),
    puzzleInfos(ctrl, data.puzzle),
    gameInfos(ctrl, data.game, data.puzzle)
  ]);
}

function puzzleInfos(ctrl: Controller, puzzle): VNode {
  return h('div.infos.puzzle', {
    attrs: dataIcon('-')
  }, [h('div', {attrs: {class: 'wrapper'}},[
    h('div', [
      h('a.title', {
        attrs: { href: '/training/' + puzzle.id }
      }, ctrl.trans('puzzleId', puzzle.id)),
      h('a', {
        attrs: {
          'data-icon': (ctrl.vm.liked) ? 't' : 's',
          title: (ctrl.vm.liked) ? '取消收藏': '收藏',
          class: 'like'
        },
        hook: bind('click', () => ctrl.like(!ctrl.vm.liked))
      })
    ]),
    h('p', ctrl.trans.vdom('ratingX', ctrl.vm.mode === 'play' ?
        h('span.hidden', ctrl.trans.noarg('hidden')) :
        (puzzle.ipt ? h('strong', 'NA'): h('strong', puzzle.rating)))),
    h('p', ctrl.trans.vdom('playedXTimes', h('strong', window.lichess.numberFormat(puzzle.attempts))))
  ])]);
}

function gameInfos(ctrl: Controller, game, puzzle): VNode {
  return h('div.infos', {
    attrs: dataIcon(game.perf.icon)
  }, [h('div', [
    h('p', ctrl.trans.vdom('fromGameLink', !ctrl.vm.ipt ? h('a', {
      attrs: { href: `/${game.id}/${puzzle.color}#${puzzle.initialPly}` }
    }, '#' + game.id) : h('span', '#' + game.id))),
    h('p', [
      game.clock, ' • ',
      game.perf.name, ' • ',
      ctrl.trans.noarg(game.rated ? 'rated' : 'casual')
    ]),
    h('div.players', game.players.map(function(p) {
      return h('div.player.color-icon.is.text.' + p.color,
        p.userId ? h('a.user-link.ulpt', {
          attrs: { href: '/@/' + p.userId }
        }, p.name) : p.name
      );
    }))
  ])]);
}

function capsuleInfos(capsule) {
  return capsule ? h('div.infos.capsule', [
    h('strong', '来自：'),
    h('a', {
      attrs: { href: `/capsule/${capsule.id}/puzzle/list`}
    }, capsule.name)
  ]) : null;
}

function homeworkInfos(homework) {
  return homework ? h('div.infos.homework', [
    h('strong', [
      h('span', homework.clazzName),
      h('span', ` `),
      h('a', {
        attrs: { href: `/homework/show?id=${homework.id}` }
      }, `第${homework.index}节`)
    ])
  ]) : null;
}

export function userBox(ctrl: Controller) {
  const data = ctrl.getData();
  if (!data.user || !data.rated) return;
  const diff = ctrl.vm.round && ctrl.vm.round.ratingDiff;
  const hash = ctrl.recentHash();
  return h('div.puzzle__side__user', [
    h('h2', ctrl.trans.vdom('yourPuzzleRatingX', h('strong', [
      data.user.rating,
      ...(diff > 0 ? [' ', h('good.rp', '+' + diff)] : []),
      ...(diff < 0 ? [' ', h('bad.rp', '−' + (-diff))] : [])
    ]))),
    h('div', thunk('div.rating_chart.' + hash, ratingChart, [ctrl, hash]))
  ]);
}

function ratingChart(ctrl: Controller, hash: string) {
  return h('div.rating_chart.' + hash, {
    hook: {
      insert(vnode) { drawRatingChart(ctrl, vnode) },
      postpatch(_, vnode) { drawRatingChart(ctrl, vnode) }
    }
  });
}

function drawRatingChart(ctrl: Controller, vnode: VNode) {
  const $el = $(vnode.elm as HTMLElement);
  const dark = document.body.classList.contains('dark');
  const points = ctrl.getData().user.recent.map(function(r) {
    return r[2] + r[1];
  });
  const redraw = () => $el['sparkline'](points, {
    type: 'line',
    width: Math.round($el.outerWidth()) + 'px',
    height: '80px',
    lineColor: dark ? '#4444ff' : '#0000ff',
    fillColor: dark ? '#222255' : '#ccccff',
    numberFormatter: (x: number) => { return x; }
  });
  window.lichess.raf(redraw);
  window.addEventListener('resize', redraw);
}

export function tagBox(ctrl: Controller) {
  const data = ctrl.getData();
  if (!data.user) return;
  if (ctrl.vm.mode === 'play') return;

  return h('div.puzzle__side__tag', [
    h('input', {
      attrs: {
        id: 'taginput',
        type: 'text',
        value: ctrl.vm.tagger,
        placeholder: '添加标签'
      },
      hook: {
        insert(vnode) { applyTags( vnode, ctrl) },
        postpatch(_, vnode) { applyTags(vnode, ctrl) }
      }
    })
  ]);
}

function applyTags(vnode: VNode, ctrl: Controller) {
  const $el = $(vnode.elm as HTMLElement);
  (<any>$el).tagsInput({
    "width": "100%",
    "height": "70px",
    "interactive": true,
    "defaultText": "添加标签",
    "removeWithBackspace": true,
    "minChars": 0,
    "maxChars": 10,
    "placeholderColor": "#666666",
    "onAddTag": function () {
      ctrl.setTag($("#taginput").val());
    },
    "onRemoveTag": function () {
      ctrl.setTag($("#taginput").val());
    }
  })
}

export function markBox(ctrl: Controller) {
  const mark = ctrl.getData().puzzle.mark;
  if (!mark || ctrl.vm.mode === 'play') return;

  let phase = mark.phase == undefined ? [] : mark.phase;
  let moveFor = mark.moveFor == undefined ? [] : mark.moveFor;
  let subject = mark.subject == undefined ? [] : mark.subject;
  let chessGame = mark.chessGame == undefined ? [] : mark.chessGame;
  let strength = mark.strength == undefined ? [] : mark.strength;
  let comprehensive = mark.comprehensive == undefined ? [] : mark.comprehensive;
  let tag = mark.tag == undefined ? [] : mark.tag;
  let tags = phase.concat(moveFor).concat(subject).concat(strength).concat(chessGame).concat(comprehensive).concat(tag);
  return h('div.puzzle__side__mark', tags.map(function(tag) {
    return h('span', tag);
  }));
  
}
