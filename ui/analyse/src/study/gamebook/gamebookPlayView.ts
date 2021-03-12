import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import GamebookPlayCtrl from './gamebookPlayCtrl';
import { bind, dataIcon, iconTag, richHTML } from '../../util';
import { State } from './gamebookPlayCtrl';

const defaultComments = {
  play: '当前局面该走什么？',
  end: '祝贺您完成本课程！'
};

export function render(ctrl: GamebookPlayCtrl): VNode {

  const state = ctrl.state,
  comment = state.comment || defaultComments[state.feedback];

  return h('div.gamebook', {
    hook: { insert: _ => window.lichess.loadCssPath('analyse.gamebook.play') }
  }, [
    comment ? h('div.comment', {
      class: { hinted: state.showHint }
    }, [
      h('div.content', { hook: richHTML(comment) }),
      hintZone(ctrl)
    ]) : undefined,
    h('div.floor', [
      renderFeedback(ctrl, state),
      h('img.mascot', {
        attrs: {
          width: 120,
          height: 120,
          src: window.lichess.assetUrl('images/mascot/octopus.svg')
        }
      })
    ])
  ]);
}

function hintZone(ctrl: GamebookPlayCtrl) {
  const state = ctrl.state,
  clickHook = () => ({
    hook: bind('click', ctrl.hint, ctrl.redraw)
  });
  if (state.showHint) return h('div', clickHook(), [
    h('div.hint', { hook: richHTML(state.hint!) })
  ]);
  if (state.hint) return h('a.hint', clickHook(), '获得提示');
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback,
  color = ctrl.root.turnColor();
  if (fb === 'bad') return h('div.feedback.act.bad' + (state.comment ? '.com' : ''), {
    hook: bind('click', ctrl.retry)
  }, [
    iconTag('P'),
    h('span', '重试')
  ]);
  if (fb === 'good' && state.comment) return h('div.feedback.act.good.com', {
    hook: bind('click', ctrl.next)
  }, [
    h('span.text', { attrs: dataIcon('G') }, '下一步'),
    h('kbd', '<空格键>')
  ]);
  if (fb === 'end') return renderEnd(ctrl);
  return h('div.feedback.info.' + fb + (state.init ? '.init' : ''),
    h('div', fb === 'play' ? [
      h('div.no-square', h('piece.king.' + color)),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('yourTurn')),
        h('em', ctrl.trans.noarg(color === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack'))
      ])
    ] : [ '好棋！' ])
  );
}

function renderEnd(ctrl: GamebookPlayCtrl) {
  const study = ctrl.root.study!,
  nextChapter = study.nextChapter();
  return h('div.feedback.end', [
    nextChapter ? h('a.next.text', {
      attrs: dataIcon('G'),
      hook: bind('click', () => study.setChapter(nextChapter.id))
    }, '下一章') : undefined,
    h('a.retry', {
      attrs: dataIcon('P'),
      hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw)
    }, '再来一次'),
    h('a.analyse', {
      attrs: dataIcon('A'),
      hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw)
    }, '分析')
  ]);
}
