import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import TournamentController from './ctrl';
import { bind } from './view/util';

export function button(ctrl: TournamentController): VNode {
  return h('button.fbt', {
    class: { active: ctrl.searching },
    attrs: {
      'data-icon': ctrl.searching ? 'L' : 'y',
      title: '查询锦标赛棋手'
    },
    hook: bind('mousedown', ctrl.toggleSearch, ctrl.redraw)
  });
}

export function input(ctrl: TournamentController): VNode {
  return h('div.search',
    h('input', {
      hook: {
        insert(vnode) {
          window.lichess.raf(() => {
            const el = vnode.elm as HTMLInputElement;
            window.lichess.userAutocomplete($(el), {
              tag: 'span',
              tour: ctrl.data.id,
              focus: true,
              minLength: 2,
              onSelect(v) {
                ctrl.jumpToPageOf(v.id || v);
                $(el).typeahead('close');
                el.value = '';
                ctrl.redraw();
              }
            });
          });
        }
      }
    })
  );
}
