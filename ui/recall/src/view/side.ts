import {Controller} from '../interfaces';
import {h} from 'snabbdom';
import {dataIcon, bind} from '../util';

export function gameMetas(ctrl: Controller) {
    let game = ctrl.getData().game;
    return h('div.game__meta', [
        h('section', [
            h('div.game__meta__infos', {
                attrs: dataIcon(game.perf.icon)
            }, [
                h('div', [
                    h('div', [
                        h('span', '来自对局'),
                        (game.id == 'synthetic' || game.id == 'temporary') ? ' #——' : h('a', {
                            attrs: {href: `/${game.id}`}
                        }, '#' + game.id)
                    ]),
                    h('div', [
                        game.clock ? game.clock : '无', ' • ',
                        game.perf.name, ' • ',
                        game.rated ? '无积分' : '无积分'
                    ])
                ])
            ]),
            h('div.game__meta__players', game.players.map(function (p) {
                return h('div.player.color-icon.is.text.' + p.color,
                    p.userId ? h('a.user-link.ulpt', {
                        attrs: {href: '/@/' + p.userId}
                    }, p.name) : p.name
                );
            }))
        ]),
        game.status ? h('section.status', game.status) : null
    ])
}

export function history(ctrl: Controller) {
    let historys = ctrl.vm.historys ? ctrl.vm.historys : [];
    return h('div.history', [
        h('div.action', [
            h('a.create', {
                hook: bind('click', _ =>  {
                    ctrl.openCreateModal()
                })
            }, '新建'),
            h('a.whole', { attrs:{ href: '/recall/whole?p=1' } }, '全部记录')
        ]),
        h('table.list', historys.map(function (his, i) {
            return h('tr', {
                class: {
                    'current': ctrl.getData().recall.id === his.id
                }
            }, [
                h('td', '#' + (i + 1)),
                h('td', [
                    h('a.name', { attrs: { href: '/recall/' + his.id } }, his.name)
                ]),
                h('td', [
                    h('a.edit', {
                        hook: bind('click', _ =>  {
                            $.ajax({
                                url: '/recall/'+ his.id +'/edit?&goTo='+ location.href
                            }).then(function(html) {
                                $.modal($(html));
                                $('.cancel').click(function () {
                                    $.modal.close();
                                });
                            })
                        })
                    }, '编辑'),
                    ' ',
                    h('a.del', {
                        hook: bind('click', _ =>  {
                            if(confirm('确认删除？')) {
                                $.post('/recall/'+ his.id +'/delete?goTo=/recall').done(function () {
                                    location.reload();
                                })
                            }
                        })
                    }, '删除')
                ])
            ])
        }))
    ])
}

