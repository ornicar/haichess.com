import { h } from 'snabbdom';
import { spinner, bind } from './util';
import HomeController from '../ctrl';
import {Appt, Calendar} from '../interfaces';

const assetsUrl = $('body').data('asset-url') + '/assets';

export default function(ctrl: HomeController) {
  if (ctrl.redirecting) return spinner();

  return h('div.home-header-content', [
      renderAppt(ctrl),
      renderCalendar(ctrl)
  ]);
};

function renderAppt(ctrl: HomeController) {
    let appts = ctrl.vm.appts;
    return h('div.head-box.home__appts', [
        h('div.box-header', [
            h('div.title.text', [h('img', { attrs: { src: assetsUrl + '/images/rectangle.png'}}), h('span', '待处理预约')]),
            h('a.more', {attrs: {href: `/appt/page`}}, '更多 »')
        ]),
        h('div.box-body', [
            h('div.list', [
                (appts === undefined || appts.length == 0) ? h('span', '- 暂无 -') :
                    h('ul', appts.map(function (appt: Appt) {
                        return h('li', [
                            h('div.title', [
                                h('div', appt.game),
                                appt.contest == null ? null : h('div', [
                                    h('a', {
                                        attrs: {'data-icon': '赛', href: `/contest/${appt.contest.id}`}
                                    }, appt.contest.name),
                                    h('span', ` 第${appt.contest.roundNo}轮 `),
                                    h('span', ` #${appt.contest.boardNo}`)
                                ])
                            ]),
                            h('div.players', [
                                h('a.user-link.ulpt', {
                                    attrs: {href: '/@/' + appt.whitePlayerUid}
                                }, appt.whitePlayerUid),
                                h('strong', ' Vs. '),
                                h('a.user-link.ulpt', {
                                    attrs: {href: '/@/' + appt.blackPlayerUid}
                                }, appt.blackPlayerUid)
                            ]),
                            h('div.time', appt.record.time),
                            h('div.actions',  ctrl.isAccept(appt) ? ['已接受'] : [
                                ctrl.isExpired(appt) ? null : h('button.green', {
                                    hook: bind('click', e => {
                                        if (confirm('确认接受？')) {
                                            ctrl.accept(appt.id)
                                        }
                                    })
                                }, '接受'),
                                ctrl.isMaxTimeExpired(appt) ? '已过期' : h('button', {
                                    hook: bind('click', e => {
                                        ctrl.setRedirecting();
                                        location.href = `/appt/${appt.id}/form`
                                    })
                                }, '改时间'),
                                appt.source == 'challenge' && ctrl.isChallenger(appt) ? h('button.red', {
                                    hook: bind('click', e => {
                                        if (confirm('确认取消？')) {
                                            ctrl.cancel(appt.id)
                                        }
                                    })
                                }, '取消') : null,
                                appt.source == 'challenge' && ctrl.isDest(appt) ? h('button.red', {
                                    hook: bind('click', e => {
                                        if (confirm('确认拒绝？')) {
                                            ctrl.decline(appt.id)
                                        }
                                    })
                                }, '拒绝') : null
                            ])
                        ])
                    }))
            ])
        ])
    ])
}

function renderCalendar(ctrl: HomeController) {
    let c = ctrl.vm.calendars;
    let periods = ['上午', '下午', '晚上'];
    let isWeek = ctrl.isWeek();

    return h('div.head-box.home__calendar', {
        class: {
            'calendar-week': isWeek,
            'calendar-day': !isWeek
        }
    },[
        h('div.box-header', [
            h('div.title.text', [h('img', { attrs: { src: assetsUrl + '/images/rectangle.png'}}), h('span', '日程')]),
            h('div.actions', [
                h('div.btn-group.week', [
                    h('button.prev', {
                        hook: bind('click', e => {
                            ctrl.prevCalendars();
                        })
                    }, isWeek ? '< 上一周': '< 前一天'),
                    h('button.today', {
                        hook: bind('click', e => {
                            ctrl.todayCalendars();
                        })
                    },isWeek ? '本周': '今天'),
                    h('button.next',{
                        hook: bind('click', e => {
                            ctrl.nextCalendars();
                        })
                    }, isWeek ? '下一周 >': '后一天 >')
                ]),
                h('div.btn-group.view', [
                    h('button', {
                        class: {
                            'active': isWeek
                        },
                        hook: bind('click', e => {
                            ctrl.setWeekMode();
                        })
                    },'周视图'),
                    h('button', {
                        class: {
                            'active': !isWeek
                        },
                        hook: bind('click', e => {
                            ctrl.setDayMode();
                        })
                    },'日视图')
                ])
            ])
        ]),
        h('div.box-body', [
            h('table.list', [
                h('thead', [
                    h('tr', [
                        h('th', { attrs: {style: 'width: 3em'}}),
                        ...c.weeks.map(function (week, i) {
                            return h('th', [
                                h('div', week),
                                h('div', c.days[i])
                            ])
                        })
                    ])
                ]),
                h('tbody', periods.map(function (period) {
                    return h('tr', [
                        h('td', period),
                        ...c.days.map(function (day) {
                            let cs = c.list.filter(c => (c.period == period && c.date == day));
                            return renderEvents(cs)
                        })
                    ])
                }))
            ])
        ])
    ])
}

function renderEvents(calendars: Calendar[]) {
    return h('td', [
        h('div.events', calendars.map(function (calendar: Calendar) {
            return  h('a.event', {
                class: {
                    'overdue': calendar.finished,
                },
                attrs: {
                    title: calendar.content,
                    href: calendar.link ? calendar.link : 'javascript:void();'
                }
            }, [
                h('div.event-head', [
                    calendar.icon ? h('i.icon', {attrs: {'data-icon': calendar.icon , style: 'color: ' + (calendar.bg ? calendar.bg : '#000000')}}) : null,
                    h('div.time', calendar.st)
                ]),
                h('div.event-content', calendar.content)
            ])
        }))
    ])
}
