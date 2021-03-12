import { h, thunk } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import {/* plural,*/ bind, spinner, richHTML, option } from '../../util';
import { StudyCtrl } from '../interfaces';
import { MaybeVNodes } from '../../interfaces';
import { StudyPracticeData, StudyPracticeCtrl } from './interfaces';
import { boolSetting } from '../../boolSetting';
import { view as descView } from '../description';

function selector(data: StudyPracticeData) {
  return h('select.selector', {
    hook: bind('change', e => {
      location.href = '/practice/' + (e.target as HTMLInputElement).value;
    })
  }, [
    h('option', {
      attrs: { disabled: true, selected: true }
    }, '练习列表'),
    ...data.structure.map(function(section) {
      return h('optgroup', {
        attrs: { label: section.name }
      }, section.studies.map(function(study) {
        return option(
          section.id + '/' + study.slug + '/' + study.id,
          '',
          study.name);
      }));
    })
  ]);
}

function renderGoal(practice: StudyPracticeCtrl, inMoves: number) {
  const goal = practice.goal();
  switch (goal.result) {
    case 'mate':
      return '将杀对手';
    case 'mateIn':
      return inMoves + '步之内将杀对手';
    case 'drawIn':
      return inMoves + '步之内和棋';
    case 'equalIn':
      return inMoves + '步之内均势';
    case 'evalIn':
      if (practice.isWhite() === (goal.cp! >= 0))
        return inMoves + '步之内获得胜势'
      return '防守' + inMoves + '步';
    case 'promotion':
      return '安全的进行兵生变';
  }
}

export function underboard(ctrl: StudyCtrl): MaybeVNodes {
  if (ctrl.vm.loading) return [h('div.feedback', spinner())];
  const p = ctrl.practice!,
    gb = ctrl.gamebookPlay(),
    pinned = ctrl.data.chapter.description;
  if (gb) return pinned ? [h('div.feedback.ongoing', [
    h('div.comment', { hook: richHTML(pinned) })
  ])] : [];
  else if (!ctrl.data.chapter.practice) return [descView(ctrl, true)];
  switch (p.success()) {
    case true:
      const next = ctrl.nextChapter();
      return [
        h('a.feedback.win', next ? {
          hook: bind('click', p.goToNext)
        } : {
          attrs: { href: '/practice' }
        }, [
          h('span', '成功！'),
          ctrl.nextChapter() ? '下一节' : '返回练习目录'
        ])
      ];
    case false:
      return [
        h('a.feedback.fail', {
          hook: bind('click', p.reset, ctrl.redraw)
        }, [
          h('span', [renderGoal(p, p.goal().moves!)]),
          h('strong', '重试')
        ])
      ];
    default:
      return [
        h('div.feedback.ongoing', [
          h('div.goal', [renderGoal(p, p.goal().moves! - p.nbMoves())]),
          pinned ? h('div.comment', { hook: richHTML(pinned) }) : null
        ]),
        boolSetting({
          name: '自动加载下一节',
          id: 'autoNext',
          checked: p.autoNext(),
          change: p.autoNext
        }, ctrl.trans, ctrl.redraw)
      ];
  }
}

export function side(ctrl: StudyCtrl): VNode {

  const current = ctrl.currentChapter(),
    data = ctrl.practice!.data;

  return h('div.practice__side', [
    h('div.practice__side__title', [
      h('i.' + data.study.id),
      h('div.text', [
        h('h1', data.study.name),
        h('em', data.study.desc)
      ])
    ]),
    h('div.practice__side__chapters', {
      hook: bind('click', e => {
        e.preventDefault();
        const target = e.target as HTMLElement,
          id = (target.parentNode as HTMLElement).getAttribute('data-id') || target.getAttribute('data-id');
        if (id) ctrl.setChapter(id, true);
        return false;
      })
    }, ctrl.chapters.list().map(function(chapter) {
      const loading = ctrl.vm.loading && chapter.id === ctrl.vm.nextChapterId,
        active = !ctrl.vm.loading && current && current.id === chapter.id,
        completion = data.completion[chapter.id] >= 0 ? 'done' : 'ongoing';
      return [
        h('a.ps__chapter', {
          key: chapter.id,
          attrs: {
            href: data.url + '/' + chapter.id,
            'data-id': chapter.id
          },
          class: { active, loading }
        }, [
          h('span.status.' + completion, {
            attrs: {
              'data-icon': ((loading || active) && completion === 'ongoing') ? 'G' : 'E'
            }
          }),
          h('h3', chapter.name)
        ])
      ];
    }).reduce((a, b) => a.concat(b), [])),
    h('div.finally', [
      h('a.back', {
        attrs: {
          'data-icon': 'I',
          href: '/practice',
          title: '更多练习'
        }
      }),
      thunk('select.selector', selector, [data])
    ])
  ]);
}
