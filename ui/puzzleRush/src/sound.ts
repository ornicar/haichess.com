import throttle from 'common/throttle';
import { assetsUrl } from './util'

const soundUrl = assetsUrl + '/sound/';
let make = function(file, volume) {
  var sound = new window.Howl({
    src: [
      soundUrl + file + '.ogg',
      soundUrl + file + '.mp3'
    ],
    volume: volume || 1
  });
  return function() {
    if (window.lichess.sound.set() !== 'silent') sound.play();
  };
};

const sounds = window.lichess.sound;
export const sound = {
  move: throttle(50, sounds.move),
  capture: throttle(50, sounds.capture),
  check: throttle(50, sounds.check),
  lowtime: throttle(50, sounds.lowtime),
  countdown: throttle(50, make('other/countdown', 0.1)),
  rushGo: throttle(50, make('other/countdown-go', 0.1)),
  rushOver: throttle(50, make('other/rush-over', 0.1)),
  win: throttle(50, make('other/win', 0.1)),
  loss: throttle(50, make('other/loss', 0.1))
};

