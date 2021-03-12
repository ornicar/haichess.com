import { h } from 'snabbdom'
import { Chessground } from 'chessground';
import { Config as CgConfig } from 'chessground/config';
import resizeHandle from 'common/resize';
import {sound} from "../sound";

export default function(ctrl) {
  return h('div.puzzleRush__board.main-board' + (ctrl.pref.blindfold ? '.blindfold' : ''),  [
    ctrl.vm.page === 'playing' && ctrl.vm.countdown ? h('div.countdown', {
      hook: {
        insert: function (vnode) {
          let el = vnode.elm as HTMLElement;
          let i = 0;
          let countdown = ['3', '2', '1', 'Go'];
          showNumber(el, countdown[i]);
          let countdownInterval = setInterval(function () {
            i++;
            if (i === 4) {
              clearInterval(countdownInterval);
              ctrl.begin();
              return;
            }
            showNumber(el, countdown[i]);
          }, 1000);
        }
      }
    }/*, [h('span.number', '3')]*/) : null,
    h('div.cg-wrap', {
      hook: {
        insert: vnode => ctrl.ground(Chessground((vnode.elm as HTMLElement), makeConfig(ctrl))),
        destroy: _ => ctrl.ground().destroy()
      }
    }),
    ctrl.promotion.view()
  ]);
}

function showNumber(el, c) {
  el.innerHTML = '<span class="number">' + c + '</span>';
  if (c === 'Go') {
    sound.rushGo();
  } else {
    sound.countdown();
  }
}

function makeConfig(ctrl): CgConfig {
  const opts = ctrl.makeCgOpts();
  return {
    fen: opts.fen,
    orientation: opts.orientation,
    turnColor: opts.turnColor,
    check: opts.check,
    lastMove: opts.lastMove,
    coordinates: ctrl.pref.coords !== 0,
    addPieceZIndex: ctrl.pref.is3d,
    movable: {
      free: false,
      color: opts.movable.color,
      dests: opts.movable.dests,
      showDests: ctrl.pref.destination,
      rookCastle: ctrl.pref.rookCastle
    },
    draggable: {
      enabled: ctrl.pref.moveEvent > 0,
      showGhost: ctrl.pref.highlight
    },
    selectable: {
      enabled: ctrl.pref.moveEvent !== 1
    },
    events: {
      move: ctrl.userMove,
      insert(elements) {
        resizeHandle(
            elements,
            ctrl.pref.resizeHandle,
            opts.viewOnly ? 0 : ctrl.vm.node.ply,
            (_) => true
        )
      }
    },
    premovable: {
      enabled: opts.premovable.enabled
    },
    drawable: {
      enabled: true
    },
    highlight: {
      lastMove: ctrl.pref.highlight,
      check: ctrl.pref.highlight
    },
    animation: {
      enabled: false,
      duration: ctrl.pref.animation.duration
    },
    disableContextMenu: true
  };
}

