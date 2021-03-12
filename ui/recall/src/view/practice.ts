import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import {Controller} from "../interfaces";
import {bind} from "../util";

export default function(ctrl: Controller): VNode | undefined {
  let data = ctrl.getData();
  return h('div.practice-box', [
    h('div.title', data.recall.name),
    ctrl.vm.stage === 'pending' ? pending(ctrl) : null,
    ctrl.vm.stage === 'running' ? running(ctrl) : null,
    ctrl.vm.stage === 'finished' ? finished(ctrl) : null
  ]);
}

const color = [{'k': 'all', 'v': '双方'}, {'k': 'white', 'v': '白方'}, {'k': 'black', 'v': '黑方'}];

function pending(ctrl: Controller) {
  return h('div.pending', [
    h('div.conf', [
      h('div.color', color.map(function (d) {
        return h('div.rd', [
          h('input', {
            attrs: {
              type: 'radio', name: 'date', id: 'rd-' + d.k, checked: ctrl.vm.color === d.k, value: d.k, disabled: ctrl.vm.readonly
            },
            hook: bind('click', e => {
              ctrl.vm.color = d.k;
              let $turns = $('#input-turns');
              let turns = $turns.val();
              turns = ctrl.turns(turns);
              $turns.val(turns);
              ctrl.vm.turns = turns;
            })
          }),
          h('label', { attrs: { for: 'rd-' + d.k }}, d.v)
        ])
      })),
      h('div.turns', [
        h('label', '回合数：'),
        h('input', {
          attrs: {
            id: 'input-turns',
            type: 'number',
            min: '1',
            max: '500',
            disabled: ctrl.vm.readonly,
            value: ctrl.vm.turns
          },
          hook: bind('change', e => {
            let $this = $(e.target as HTMLElement);
            let turns = $this.val() as number;
            turns = ctrl.turns(turns);
            $this.val(turns);
            ctrl.vm.turns = turns;
          })
        })
      ])
    ]),
    h('div.action', h('button.button', {
      class: {
        'disabled': ctrl.getData().recall.id === 'synthetic'
      },
      attrs: {
        'disabled': ctrl.getData().recall.id === 'synthetic'
      },
      hook: bind('click', e => {
        ctrl.start();
      })
    }, '开始记谱'))
  ])
}

function running(ctrl: Controller) {
  let color = ctrl.currPlyColor();
  let hint = ctrl.vm.hinting;
  return h('div.feedback', [
    h('div.player', [
      ctrl.vm.ended ? null : h('div.no-square', h('piece.king.'+ color)),
      h('div.instruction', [
        h('strong', ctrl.vm.ended ? '已结束' : (ctrl.isMyTurn() ? ctrl.vm.currMistake ? '出现错误，请重试！' : '该您走' : ctrl.vm.currMistake ? '出现错误，请重试！': '对手走')),
        !ctrl.vm.ended && ctrl.isMyTurn() ? h('div.choices', [
          h('a', {
            hook: bind('click', e => {
              ctrl.hint();
            })
          }, hint ? (hint.mode === 'piece' ? '查看答案' : '隐藏答案') : '提示')
        ]) : null
      ]),
      h('div.action', h('button.button.button-red', {
        hook: bind('click', e => {
          ctrl.finish()
        })
      }, '结束'))
    ]),
    h('div.progress', [
      h('div.text', [
        '进度：', ctrl.vm.currTurnsWithoutHit, '/', ctrl.vm.turns
      ]),
      h('div.bar', {
        attrs: {
          style:  'width: ' + (ctrl.vm.currTurnsWithoutHit / ctrl.vm.turns * 100) + '%'
        }
      })
    ])
  ])
}


function finished(ctrl: Controller) {
  let cl = color.filter(c => c.k === ctrl.vm.color)[0].v;
  return h('div.finished', [
    h('div.infos', [
      h('span.color', [
        '棋色：', cl
      ]),
      h('span.turns', [
        '回合数：', ctrl.vm.turns
      ]),
      h('span.progress', [
        '进度：', ctrl.vm.currTurnsWithoutHit, '/', ctrl.vm.turns
      ])
    ]),
    h('div.action', h('button.button', {
      hook: bind('click', e => {
        ctrl.startAgain();
      })
    }, '再来一次'))
  ])
}
