import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import { Chessground } from 'chessground';
import { HomeOpts } from './interfaces';
import HomeController from './ctrl';

export const patch = init([klass, attributes]);

import makeCtrl from './ctrl';
import view from './view/main';

export function start(opts: HomeOpts) {

  let vnode: VNode, ctrl: HomeController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  if(opts.element) {
    opts.element.innerHTML = '';
    vnode = patch(opts.element, blueprint);
  }

  $(function() {
    (<any> $('.banner')).unslider({
      speed: 5000,
      delay: 5000,
      keys: false,
      dots: true,
      arrows: false,
      fluid: false
    });

    let $rush = $('.home__rush');
    $rush.find('.header > div').click(function() {
      let oldActiveTab = $rush.find('.header > .active').data('tab');
      $rush.find('.header > div').removeClass('active');
      $rush.find('.panels div').removeClass('active');

      $(this).addClass('active');
      let activeTab = $(this).data('tab');
      $rush.find('.panels').find('.' + activeTab).addClass('active');

      let href = $('.btn-rush').attr('href');
      $('.btn-rush').attr('href', href.replace(oldActiveTab, activeTab));
    });

    $(window).resize(function () {
      $('.banner').css('width', '100%');

      let rh1 = $('.home__daily .mini-board').height();
      if(rh1 > 0 && $(window).width() >= 500) {
        $('.home__rush .tabs').height(rh1);
      } else $('.home__rush .tabs').css('height', '');

      let rh2 = $('.home__tv .mini-board').height();
      if(rh2 > 0 && $(window).width() >= 500) {
        $('.home__leaderboard .content-wrap').height(rh2);
      } else $('.home__leaderboard .content-wrap').css('height', '');
    });
    $(window).trigger('resize');
  });

  return {
    socketReceive: ctrl.socket.receive,
    redraw: ctrl.redraw
  };
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
