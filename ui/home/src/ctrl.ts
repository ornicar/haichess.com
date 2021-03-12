import {Appt, HomeOpts, Vm} from './interfaces';
import HomeSocket from './socket';
import * as xhr from './xhr';

export default class HomeController {

    vm: Vm = {} as Vm;
    opts: HomeOpts;
    socket: HomeSocket;
    redraw: () => void;
    redirecting: boolean = false;
    private alreadyWatching: string[] = [];

    constructor(opts: HomeOpts, redraw: () => void) {
        this.vm = {
            nowPlaying: opts.data.nowPlaying,
            appts: [],
            calendars: {
                list: [],
                days: [],
                weeks: []
            },
            week: 0,
            day: 0,
            mode: this.getMode(),// week, day
        };
        this.opts = opts;
        this.redraw = redraw;
        this.socket = new HomeSocket(opts.socketSend, this);
        if(this.opts.userId) {
            this.loadAppts();
            this.isWeek() ? this.loadWeekCalendars() : this.loadDayCalendars();
        }
        this.startWatching();
    }

    setRedirecting = () => {
        this.redirecting = true;
        setTimeout(() => {
            this.redirecting = false;
            this.redraw();
        }, 4000);
        this.redraw();
    };

    gameActivity = gameId => {
        if (this.vm.nowPlaying.find(p => p.gameId === gameId))
            xhr.nowPlaying().then(povs => {
                this.vm.nowPlaying = povs;
                this.startWatching();
                this.redraw();
            });
    };

    private startWatching() {
        const newIds = this.vm.nowPlaying
            .map(p => p.gameId)
            .filter(id => !this.alreadyWatching.includes(id));
        if (newIds.length) {
            setTimeout(() => this.socket.send("startWatching", newIds.join(' ')), 2000);
            newIds.forEach(id => this.alreadyWatching.push(id));
        }
    };

    loadAppts = () => {
        xhr.loadAppts().then(appts => {
            this.vm.appts = appts;
            this.redraw();
        });
    };

    prevCalendars = () => {
        if (this.isWeek()) {
            this.vm.week = this.vm.week - 1;
            this.loadWeekCalendars();
        } else {
            this.vm.day = this.vm.day - 1;
            this.loadDayCalendars();
        }
    };

    nextCalendars = () => {
        if (this.isWeek()) {
            this.vm.week = this.vm.week + 1;
            this.loadWeekCalendars();
        } else {
            this.vm.day = this.vm.day + 1;
            this.loadDayCalendars();
        }
    };

    todayCalendars = () => {
        if (this.isWeek()) {
            this.vm.week = 0;
            this.loadWeekCalendars();
        } else {
            this.vm.day = 0;
            this.loadDayCalendars();
        }
    };


    getMode = () => {
        let w = $(window).width();
        if (w <= 650) {
            return "day";
        } else {
            return "week";
        }
    };

    isWeek = () => {
        return this.vm.mode === 'week';
    };

    setWeekMode = () => {
        this.vm.mode = 'week';
        this.resetCalendars();
        this.loadWeekCalendars();
    };

    setDayMode = () => {
        this.vm.mode = 'day';
        this.resetCalendars();
        this.loadDayCalendars();
    };

    resetCalendars = () => {
        this.vm.week = 0;
        this.vm.day = 0;
        this.vm.calendars.list = [];
        this.vm.calendars.days = [];
        this.vm.calendars.weeks = [];
    };

    loadWeekCalendars = () => {
        xhr.loadWeekCalendars(this.vm.week).then(data => {
            this.vm.calendars = data;
            this.redraw();
        });
    };

    loadDayCalendars = () => {
        xhr.loadDayCalendars(this.vm.day).then(data => {
            this.vm.calendars = data;
            this.redraw();
        });
    };

    accept = (id) => {
        xhr.accept(id).then(() => {
            this.loadAppts();
            (this.vm.mode === 'week') ? this.loadWeekCalendars() : this.loadDayCalendars();
        });
    };

    cancel = (id) => {
        xhr.cancel(id).then(() => {
            this.loadAppts();
            (this.vm.mode === 'week') ? this.loadWeekCalendars() : this.loadDayCalendars();
        });
    };

    decline = (id) => {
        xhr.decline(id).then(() => {
            this.loadAppts();
            (this.vm.mode === 'week') ? this.loadWeekCalendars() : this.loadDayCalendars();
        });
    };

    isAccept = (appt: Appt) => {
        let color = this.opts.userId === appt.whitePlayerUid;
        if (color) {
            return appt.record.whiteStatus == 'confirmed'
        } else {
            return appt.record.blackStatus == 'confirmed'
        }
    };

    isExpired = (appt: Appt) => {
        return new Date(appt.record.time) < new Date();
    };

    isMaxTimeExpired = (appt: Appt) => {
        return new Date(appt.maxDateTime) < new Date();
    };

    isChallenger = (appt: Appt) => {
        return appt.createBy == this.opts.userId;
    };

    isDest = (appt: Appt) => {
        return appt.createBy != undefined && appt.createBy != this.opts.userId;
    };

}
