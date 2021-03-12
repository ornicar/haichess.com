import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { spinner } from '../util';
import { Controller } from '../interfaces';
import { bind } from '../util';

function initial(ctrl: Controller): VNode {
  var puzzleColor = ctrl.getData().puzzle.color;
  return h('div.actions', [
    h('div.feedback.play', [
      h('div.player', [
        h('div.no-square', h('piece.king.' + puzzleColor)),
        h('div.instruction', [
          h('strong', '该您走'),
          h('em', puzzleColor === 'white' ? '找出白方的最佳着' : '找出黑方的最佳着')
        ])
      ])
    ]),
    (ctrl.vm.page === 'playing') ? h('button.button.button-red', {
      hook: bind('click', _ => {
        ctrl.finish(50);
      })
    }, '结束本局') : null
  ]);
}


function good(ctrl: Controller): VNode {
  return h('div.actions', [
    h('div.feedback.good', [
      h('div.player', [
        h('div.icon', '✓'),
        h('div.instruction', [
          h('strong', '最佳着！'),
          h('em', '请继续...')
        ])
      ])
    ]),
    (ctrl.vm.page === 'playing') ? h('button.button.button-red', {
      hook: bind('click', _ => {
        ctrl.finish(50);
      })
    }, '结束本局') : null
  ]);
}

function retry(ctrl: Controller): VNode {
  return h('div.actions', [
    h('div.feedback.retry', [
      h('div.player', [
        h('div.icon', '!'),
        h('div.instruction', [
          h('strong', '好棋！'),
          h('em', '您还可以做得更好。')
        ])
      ])
    ]),
    (ctrl.vm.page === 'playing') ? h('button.button.button-red', {
      hook: bind('click', _ => {
        ctrl.finish(50);
      })
    }, '结束本局') : null
  ]);
}

function fail(ctrl: Controller): VNode {
  return h('div.actions', [
    h('div.feedback.fail', [
      h('div.player', [
        h('div.icon', '✗'),
        h('div.instruction', [
          h('strong', '解题失败'),
          h('em', '继续努力吧！')
        ])
      ])
    ]),
    (ctrl.vm.page === 'playing') ? h('button.button.button-red', {
      hook: bind('click', _ => {
        ctrl.finish(50);
      })
    }, '结束本局') : null
  ]);
}

function win(ctrl: Controller): VNode {
  return h('div.actions', [
    h('div.feedback.win', [
      h('div.player', [
        h('div.icon', '✓'),
        h('div.instruction', [
          h('strong', '解题成功'),
          h('em', '加油！')
        ])
      ])
    ]),
    (ctrl.vm.page === 'playing') ? h('button.button.button-red', {
      hook: bind('click', _ => {
        ctrl.finish(50);
      })
    }, '结束本局') : null
  ]);
}

function loading(): VNode {
  return h('div.actions', [
    h('div.feedback.loading', spinner())
  ]);
}
export default function(ctrl: Controller) {
  if (ctrl.vm.countdown || ctrl.vm.loading) return loading();
  if (ctrl.vm.lastFeedback === 'init') return initial(ctrl);
  if (ctrl.vm.lastFeedback === 'good') return good(ctrl);
  if (ctrl.vm.lastFeedback === 'retry') return retry(ctrl);
  if (ctrl.vm.lastFeedback === 'fail') return fail(ctrl);
  if (ctrl.vm.lastFeedback === 'win') return win(ctrl);
}
