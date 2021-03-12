import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import chessGround from './chessground';
import side from './side';
import tools from './tools';
import * as gridHacks from './gridHacks';
import { Controller } from '../interfaces';

export default function (ctrl: Controller): VNode {
    return h('main.puzzleRush', {
            class: {
                home: true
            },
            hook: {
                postpatch(_, vnode) {
                    gridHacks.start(vnode.elm as HTMLElement);
                }
            }
        },
        [
            ctrl.vm.page === 'home' || ctrl.vm.page === 'finish' ? side(ctrl) : null,
            chessGround(ctrl),
            tools(ctrl)
        ]
    );
}

