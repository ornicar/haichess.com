import { h } from 'snabbdom';
import { bind, dataIcon } from '../util';

function renderVote(ctrl) {
  var data = ctrl.getData();
  if (!data.puzzle.enabled) return;
  return h('div.vote', [
    h('a', {
      attrs: {
        'data-icon': 'S',
        title: ctrl.trans.noarg('thisPuzzleIsCorrect')
      },
      class: { active: ctrl.vm.voted === true },
      hook: bind('click', () => ctrl.vote(true))
    }),
    h('span.count', {
      attrs: {
        title: 'Popularity'
      }
    }, '' + Math.max(0, data.puzzle.vote)),
    h('a', {
      attrs: {
        'data-icon': 'R',
        title: ctrl.trans.noarg('thisPuzzleIsWrong')
      },
      class: { active: ctrl.vm.voted === false },
      hook: bind('click', () => ctrl.vote(false))
    })
  ]);
}

export default function(ctrl) {
  const data = ctrl.getData();
  const voteCall = !!data.user && ctrl.callToVote() && data.puzzle.enabled && data.voted === undefined;
  const showNextPuzzle = data.showNextPuzzle;
  return h('div.puzzle__feedback.after' + (voteCall && showNextPuzzle ? '.call' : ''), [
    voteCall ? h('div.vote_call', [
      h('strong', ctrl.trans('wasThisPuzzleAnyGood')),
      h('br'),
      h('span', ctrl.trans('pleaseVotePuzzle'))
    ]) : (ctrl.thanks() ? h('div.vote_call',
      h('strong', ctrl.trans('thankYou'))
    ) : null),
    h('div.half.half-top', [
      ctrl.vm.lastFeedback === 'win' ? h('div.complete.feedback.win', h('div.player', [
        h('div.icon', '✓'),
        h('div.instruction', ctrl.trans.noarg('success'))
      ])) : h('div.complete', '完成训练！'),
      data.user ? renderVote(ctrl) : null
    ]),
    (showNextPuzzle && !ctrl.vm.resourceNotFound) ?
      h('a.half.continue', {
        hook: bind('click', () => ctrl.nextPuzzle())
      }, [
        !ctrl.vm.puzzleErrors ? h('i', { attrs: dataIcon('G') }) : null,
        ctrl.trans.noarg('continueTraining')
      ]) : null,
    (showNextPuzzle && !ctrl.vm.resourceNotFound && ctrl.vm.puzzleErrors) ?
      h('a.half.deleteAndContinue', {
        hook: bind('click', () => ctrl.nextPuzzle(true))
      }, '删除并继续') : null,
    (ctrl.vm.theme || ctrl.vm.capsule || ctrl.vm.homework) && ctrl.vm.resourceNotFound ?
        h('div.half.rnf', [
            h('span', '已经是最后一题！'),
            ctrl.vm.capsule ? h('a', { attrs:{ href: '/capsule/list' } }, '返回列表') : null,
            ctrl.vm.homework ? h('a', { attrs:{ href: `/homework/show?id=${ctrl.vm.homework.id}` } }, '返回课后练') : null
        ]) : null
  ]);
}
