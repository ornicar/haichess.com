import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import chessground from './chessground';
import { render as treeView } from './tree';
import * as control from '../control';
import * as side from './side';
import * as gridHacks from './gridHacks';
import { Controller } from '../interfaces';
import practice from './practice';
import { onInsert, bind, bindMobileMousedown, hasTouchEvents } from '../util';

function renderOpeningBox(ctrl: Controller) {
  var opening = ctrl.getTree().getOpening(ctrl.vm.nodeList);
  if (opening) return h('div.opening_box', {
    attrs: { title: opening.eco + ' ' + opening.name }
  }, [
    h('strong', opening.eco),
    ' ' + opening.name
  ]);
}

function renderAnalyse(ctrl: Controller) {
  return h('div.recall__moves.areplay', [
    renderOpeningBox(ctrl),
    treeView(ctrl)
  ]);
}

function wheel(ctrl: Controller, e: WheelEvent) {
  const target = e.target as HTMLElement;
  if (target.tagName !== 'PIECE' && target.tagName !== 'SQUARE' && target.tagName !== 'CG-BOARD') return;
  e.preventDefault();
  if (e.deltaY > 0) control.next(ctrl);
  else if (e.deltaY < 0) control.prev(ctrl);
  ctrl.redraw();
  return false;
}

function dataAct(e) {
  return e.target.getAttribute('data-act') || e.target.parentNode.getAttribute('data-act');
}

function jumpButton(icon, effect) {
  return h('button.fbt', {
    attrs: {
      'data-act': effect,
      'data-icon': icon
    }
  });
}

function controls(ctrl: Controller) {
  return h('div.recall__controls.analyse-controls', {
    hook: onInsert(el => {
      bindMobileMousedown(el, e => {
        const action = dataAct(e);
        if (action === 'prev') control.prev(ctrl);
        else if (action === 'next') control.next(ctrl);
        else if (action === 'first') control.first(ctrl);
        else if (action === 'last') control.last(ctrl);
      }, ctrl.redraw);
    })
  }, [
    h('div.jumps', [
      jumpButton('W', 'first'),
      jumpButton('Y', 'prev'),
      jumpButton('X', 'next'),
      jumpButton('V', 'last')
    ])
  ]);
}

export default function(ctrl: Controller): VNode {
  return h('main.recall', {
    hook: {
      postpatch(old, vnode) {
        gridHacks.start(vnode.elm as HTMLElement);
      }
    }
  }, [
    h('aside.recall__side', [
      side.gameMetas(ctrl),
      side.history(ctrl)
    ]),
    h('div.recall__board.main-board' + (ctrl.pref.blindfold ? '.blindfold' : ''), {
      hook: hasTouchEvents ? undefined : bind('wheel', e => wheel(ctrl, e as WheelEvent))
    }, [
      chessground(ctrl),
      ctrl.promotion.view()
    ]),
    h('div.recall__tools', [
      renderAnalyse(ctrl),
      practice(ctrl)
    ]),
    controls(ctrl)
  ]);
}
