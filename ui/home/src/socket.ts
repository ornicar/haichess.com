import HomeController from './ctrl';
import throttle from 'common/throttle';

interface Handlers {
  [key: string]: (data: any) => void;
}
const li = window.lichess;
export default class HomeSocket {

  send: SocketSend;
  handlers: Handlers;

  constructor(send: SocketSend, ctrl: HomeController) {
    this.send = send;
    this.handlers = {
      featured(o) {
        $('.home__tv').find('.feat').html(o.html);
        li.pubsub.emit('content_loaded');
      },
      redirect(e) {
        ctrl.setRedirecting();
        li.redirect(e);
      },
      fen(e) {
        $('.mini-board-' + e.id).each(function() {
          li.parseFen($(this).data('fen', e.fen).data('lastmove', e.lm));
        });
        ctrl.gameActivity(e.id);
      },
      reload_appt() {
        ctrl.loadAppts()
      },
      reload_calendar() {
        ctrl.isWeek() ? ctrl.loadWeekCalendars() : ctrl.loadDayCalendars()
      }
    };

    li.idleTimer(
      3 * 60 * 1000,
      () => send('idle', true),
      () => {
        send('idle', false);
      });
  }

  receive = (type: string, data: any): boolean => {
    if (this.handlers[type]) {
      this.handlers[type](data);
      return true;
    }
    return false;
  }
};
