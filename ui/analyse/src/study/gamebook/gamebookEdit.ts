import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'
import { VNode } from 'snabbdom/vnode'
import AnalyseCtrl from '../../ctrl';
import { bind, iconTag } from '../../util';
import { MaybeVNodes } from '../../interfaces';
import throttle from 'common/throttle';
import * as control from '../../control';

export function running(ctrl: AnalyseCtrl): boolean {
  return !!ctrl.study && ctrl.study.data.chapter.gamebook &&
  !ctrl.gamebookPlay() && ctrl.study.vm.gamebookOverride !== 'analyse';
}

export function render(ctrl: AnalyseCtrl): VNode {

  const study = ctrl.study!,
  isMyMove = ctrl.turnColor() === ctrl.data.orientation,
  isCommented = !!(ctrl.node.comments || []).find(c => c.text.length > 2),
  hasVariation = ctrl.tree.parentNode(ctrl.path).children.length > 1;

  let content: MaybeVNodes;

  const commentHook: Hooks = bind('click', () => {
    study.commentForm.start(study.vm.chapterId, ctrl.path, ctrl.node);
    study.vm.toolTab('comments');
    window.lichess.requestIdleCallback(() => $('#comment-text').focus());
  }, ctrl.redraw);

  if (!ctrl.path) {
    if (isMyMove) content = [
      h('div.legend.todo.clickable', {
        hook: commentHook,
        class: { done: isCommented }
      }, [
        iconTag('c'),
        h('p', '在屏幕下方的说明中，输入局面的介绍，帮助学员找到初始走法。')
      ]),
      renderHint(ctrl)
    ];
    else content = [
      h('div.legend.clickable', {
        hook: commentHook
      }, [
        iconTag('c'),
        h('p', '在屏幕下方的说明中，介绍下本研习')
      ]),
      h('div.legend.todo', { class: { done: !!ctrl.node.children[0] }}, [
        iconTag('G'),
        h('p', '在棋盘上走出对手的第一步棋。')
      ])
    ];
  }
  else if (ctrl.onMainline) {
    if (isMyMove) content = [
      h('div.legend.todo.clickable', {
        hook: commentHook,
        class: { done: isCommented }
      }, [
        iconTag('c'),
        h('p', '解释对手的着法，帮助学员找到下一步棋。')
      ]),
      renderHint(ctrl)
    ];
    else content = [
      h('div.legend.clickable', {
        hook: commentHook,
      }, [
        iconTag('c'),
        h('p', '您可以给学员的正确着法一些反馈，或者留空直接进入后续的环节。')
      ]),
      hasVariation ? null : h('div.legend.clickable', {
        hook: bind('click', () => control.prev(ctrl), ctrl.redraw)
      }, [
        iconTag('G'),
        h('p', '增加变体走法，并解释为何这个走法是错误的。')
      ]),
      renderDeviation(ctrl)
    ];
  }
  else content = [
    h('div.legend.todo.clickable', {
      hook: commentHook,
      class: { done: isCommented }
    }, [
      iconTag('c'),
      h('p', '在屏幕下方的说明中，解释为何这个走法是错误的。')
    ]),
    h('div.legend', [
      h('p', '如果走法是正确的，将它提升为主线。')
    ])
  ];

  return h('div.gamebook-edit', {
    hook: { insert: _ => window.lichess.loadCssPath('analyse.gamebook.edit') }
  }, content);
}

function renderDeviation(ctrl: AnalyseCtrl): VNode {
  const field = 'deviation';
  return h('div.deviation', [
    h('div.legend.todo', { class: { done: nodeGamebookValue(ctrl.node, field).length > 2 } }, [
      iconTag('c'),
      h('p', '当走了其他的错误棋时：')
    ]),
    h('textarea', {
      attrs: { placeholder: '解释为何其他走法都是错的' },
      hook: textareaHook(ctrl, field)
    })
  ]);
}

function renderHint(ctrl: AnalyseCtrl): VNode {
  const field = 'hint';
  return h('div.hint', [
    h('div.legend', [
      iconTag(''),
      h('p', '可选的，学员点击才显示的 提示：')
    ]),
    h('textarea', {
      attrs: { placeholder: '给学员一些提示，帮助找到正确的走法' },
      hook: textareaHook(ctrl, field)
    })
  ]);
}

const saveNode = throttle(500, (ctrl: AnalyseCtrl, gamebook: Tree.Gamebook) => {
  ctrl.socket.send('setGamebook', {
    path: ctrl.path,
    ch: ctrl.study!.vm.chapterId,
    gamebook: gamebook
  });
  ctrl.redraw();
});

function nodeGamebookValue(node: Tree.Node, field: string): string {
  return (node.gamebook && node.gamebook[field]) || '';
}

function textareaHook(ctrl: AnalyseCtrl, field: string): Hooks {
  const value = nodeGamebookValue(ctrl.node, field);
  return {
    insert(vnode: VNode) {
      const el = vnode.elm as HTMLInputElement;
      el.value = value;
      el.onkeyup = el.onpaste = () => {
        const node = ctrl.node;
        node.gamebook = node.gamebook || {};
        node.gamebook[field] = el.value.trim();
        saveNode(ctrl, node.gamebook, 50);
      };
      vnode.data!.path = ctrl.path;
    },
    postpatch(old: VNode, vnode: VNode) {
      if (old.data!.path !== ctrl.path) (vnode.elm as HTMLInputElement).value = value;
      vnode.data!.path = ctrl.path;
    }
  }
}
