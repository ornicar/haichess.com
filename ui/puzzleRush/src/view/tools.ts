import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import { uci2move, bind, formatNotBlinkTime, dataIcon, assetsUrl} from '../util';
import {Controller, History} from '../interfaces';
import feedbackView from './feedback';
import { Chessground } from 'chessground';

export default function(ctrl: Controller) {
    if (ctrl.vm.page  === 'home') return home(ctrl);
    if (ctrl.vm.page  === 'playing') return playing(ctrl);
    if (ctrl.vm.page  === 'finish') return finish(ctrl);
}

const colors = [{ v: '', n: '全部'}, { v: 'White', n: '白棋'}, { v: 'Black', n: '黑棋'}];
const phases = [{ v: '', n: '全部'}, { v: 'Opening', n: '开局'}, { v: 'MiddleGame', n: '中局'}, { v: 'EndingGame', n: '残局'}];
const selector = [{ v: 'round', n: '循环'}, { v: 'random', n: '随机'}];

function home(ctrl: Controller) {
    return h('div.puzzleRush__tools.home',[
        h('div.main', [
            h('div.desc', ctrl.modeByKey(ctrl.vm.mode).desc),
            !ctrl.isCustom() ? h('div.octopus', h('img', { attrs: { src: assetsUrl + '/images/mascot/octopus.svg'}})) : null,
            ctrl.isCustom() ? h('form.custom', [
                h('table', [
                    h('tr',[
                        h('td', '难度范围：'),
                        h('td', [
                            h('input', {
                                attrs: {
                                    type: 'number',
                                    name: 'ratingMin',
                                    value: 600,
                                    step: 100,
                                    min: 600,
                                    max: 2800
                                }
                            }),
                            h('span', ' 到 '),
                            h('input', {
                                attrs: {
                                    type: 'number',
                                    name: 'ratingMax',
                                    value: 1500,
                                    step: 100,
                                    min: 600,
                                    max: 2800
                                }
                            })
                        ])
                    ]),
                    h('tr',[
                        h('td', '答案步数：'),
                        h('td', [
                            h('input', {
                                attrs: {
                                    type: 'number',
                                    name: 'stepsMin',
                                    value: 1,
                                    min: 1,
                                    max: 10
                                }
                            }),
                            h('span', ' 到 '),
                            h('input', {
                                attrs: {
                                    type: 'number',
                                    name: 'stepsMax',
                                    value: 3,
                                    min: 1,
                                    max: 10
                                }
                            })
                        ])
                    ]),
                    h('tr',[
                        h('td', '棋色：'),
                        h('td', [
                            h('select', { attrs: { name: 'color'} }, colors.map(function (item) {
                                return h('option', { attrs: { value: item.v } }, item.n)
                            }))
                        ])
                    ]),
                    h('tr',[
                        h('td', '阶段：'),
                        h('td', [
                            h('select', { attrs: { name: 'phase'} }, phases.map(function (item) {
                                return h('option', { attrs: { value: item.v} }, item.n)
                            }))
                        ])
                    ]),
                    h('tr',[
                        h('td', '出题模式：'),
                        h('td', [
                            h('select', { attrs: { name: 'selector'} }, selector.map(function (item) {
                                return h('option', { attrs: { value: item.v} }, item.n)
                            }))
                        ])
                    ]),
                    h('tr',[
                        h('td', '练习时间：'),
                        h('td', [
                            h('input', {
                                attrs: {
                                    type: 'number',
                                    name: 'minutes',
                                    value: 3,
                                    min: 1,
                                    max: 30
                                }
                            }),
                            h('span', '（分钟）')
                        ])
                    ]),
                    h('tr',[
                        h('td', '错题上限：'),
                        h('td', [
                            h('input', {
                                attrs: {
                                    type: 'number',
                                    name: 'limit',
                                    value: 3,
                                    min: 1,
                                    max: 20
                                }
                            }),
                            h('span', '（个）')
                        ])
                    ])
                ])
            ]) : null
        ]),
        h('div.actions', [
            h('div.pick', ctrl.modes.map(function(m) {
                return h('a', {
                    class: {
                        active: ctrl.vm.mode == m.k
                    },
                    attrs: {
                        mode: m.k
                    },
                    hook: bind('click', e => {
                        pick(e, ctrl);
                    })
                }, [
                    h('span', m.v)
                ])
            })),
            h('div.start', [
                h('button.button', {
                    hook: bind('click', _ => {
                        if (ctrl.started[ctrl.vm.mode as string]) {
                            location.href = '/training/rush/' + ctrl.started[ctrl.vm.mode as string];
                        } else {
                            ctrl.start();
                        }
                    })
                }, startButtonText(ctrl))
            ])
        ])
    ]);
}

function playing(ctrl: Controller) {
    return h('div.puzzleRush__tools.playing',[
        playingHeader(ctrl),
        feedbackView(ctrl)
    ]);
}

function finish(ctrl: Controller) {
    return h('div.puzzleRush__tools.finish',[
        playingHeader(ctrl),
        afterView(ctrl)
    ]);
}

function playingHeader(ctrl: Controller) {
    return h('div.main', [
        h('table.finish-header', [
            h('tr', [
                h('td', {attrs: { rowspan: 2 }}, [
                    h('div.record-warp', [
                        h('span.record', ctrl.vm.win.toString())
                    ])
                ]),
                h('td', [
                    h('span.mode', ctrl.isCustom() ? ctrl.vm.condition.minutes + ' 分钟' : ctrl.modeByKey(ctrl.vm.mode).v)
                ])
            ]),
            h('tr', [
                h('td', [
                    h('div.clock', startClock(ctrl))
                ])
            ])
        ]),
        h('div.finish-streaks', streaks(ctrl)),
        ctrl.isCustom() ? condition(ctrl) : null
    ])
}

function streaks(ctrl: Controller): VNode[] {
    return ctrl.vm.history.map(function (p) {
        return h('a.streaks', {
                attrs:{
                    href: '/training/' + p.id,
                    target: '_blank'
                },
                class: { win: p.win, loss: !p.win, timeout: p.timeout},
                hook: {
                    insert: vnode => {
                        $(vnode.elm as HTMLElement).powerTip({
                            intentPollInterval: 100,
                            placement: 'se',
                            mouseOnToPopup: true,
                            closeDelay: 200
                        }).data('powertipjq', miniPuzzle(p))
                    },
                    destroy(vnode) {
                        $(vnode.elm as HTMLElement).data('powertipjq', null);
                        $.powerTip.destroy(vnode.elm as HTMLElement);
                    }
                }
            }, [
            h('span.icon'),
            h('span.rating', p.rating.toString())
        ])
    });
}

function condition(ctrl: Controller) {
    let condition = ctrl.vm.condition;
    return h('div.condition', [
        h('h2', '筛选条件'),
        h('table', [
            h('tr',[
                h('td', '难度范围：'),
                h('td', (!condition.ratingMin && !condition.ratingMax) ? ['不限'] : [condition.ratingMin ? condition.ratingMin : '不限', ' ~ ', condition.ratingMax ? condition.ratingMax : '不限'])
            ]),
            h('tr',[
                h('td', '答案步数：'),
                h('td', (!condition.stepsMin && !condition.stepsMax) ? ['不限'] : [condition.stepsMin ? condition.stepsMin : '不限', ' ~ ', condition.stepsMax ? condition.stepsMax : '不限'])
            ]),
            h('tr',[
                h('td', '棋色：'),
                h('td',  displayColor(condition))
            ]),
            h('tr',[
                h('td', '阶段：'),
                h('td', displayPhase(condition))
            ]),
            h('tr',[
                h('td', '出题模式：'),
                h('td', displaySelector(condition))
            ]),
            h('tr',[
                h('td', '练习时间：'),
                h('td', [condition.minutes, '分钟'])
            ]),
            h('tr',[
                h('td', '错题上限：'),
                h('td', [condition.limit, '个'])
            ])
        ])
    ])
}

function displayColor(condition) {
    let c = colors.filter(item => item.v == condition.color);
    return c.length === 0 ? '全部' : c[0].n
}

function displayPhase(condition) {
    let c = phases.filter(item => item.v == condition.phase);
    return c.length === 0 ? '全部' : c[0].n
}

function displaySelector(condition) {
    let c = selector.filter(item => item.v == condition.selector);
    return c.length === 0 ? '循环' : c[0].n
}

function miniPuzzle(p: History) {
    let $board = $('<div class="mini-board cg-wrap is2d" id="rush-puzzle-' + p.id + '"></div>').appendTo('.min-puzzle');
    Chessground($board[0] as HTMLElement, {
        coordinates: false,
        drawable: {enabled: false, visible: false},
        orientation: p.color,
        resizable: false,
        viewOnly: true,
        fen: p.fen,
        lastMove: uci2move(p.lastMove)
    });
    return $board;
}

function startClock(ctrl: Controller) {
    if (ctrl.vm.mode === 'survival') {
        return '不限时间';
    } else {
        return formatNotBlinkTime(ctrl.vm.seconds);
    }
}

function pick(e: Event, ctrl: Controller) {
    e.stopPropagation();
    let el = (e.currentTarget as HTMLElement);
    ctrl.vm.mode = el.getAttribute('mode');
    ctrl.redraw();
    ctrl.loadRank();
}

function startButtonText(ctrl: Controller) {
    return ctrl.started[ctrl.vm.mode as string] ? '继 续' : '开 始';
}

function afterView(ctrl: Controller) {
    let r = ctrl.vm.finishResult;
    return h('div.actions', [
        h('table.after', [
            h('tr', [
                h('td', [
                    h('i', { attrs: dataIcon('Q') }),
                    h('span', ' 最长连胜')
                ]),
                h('td', r.winStreaks)
            ]),
            h('tr', [
                h('td', [
                    h('i', { attrs: dataIcon('E') }),
                    h('span', ' 最难解决的谜题')
                ]),
                h('td', r.maxRating)
            ]),
            h('tr', [
                h('td', [
                    h('i', { attrs: dataIcon('p') }),
                    h('span', ' 每个谜题的平均时间')
                ]),
                h('td', formatNotBlinkTime(r.avgTime))
            ]),
            h('tr', [
                h('td', [
                    h('a.button', {attrs: {href : '/training/rush'}},'战术冲刺')
                ]),
                h('td', [
                    h('a.button.button-green', {
                        hook: bind('click', _ => {
                            ctrl.start();
                        })
                    }, '新的 ' + ctrl.modeByKey(ctrl.vm.mode).v)
                ])
            ])
        ])
    ])
}
