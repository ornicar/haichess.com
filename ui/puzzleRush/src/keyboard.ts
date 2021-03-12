import * as control from './control';

const preventing = (f: () => void) => (e: MouseEvent) => {
  e.preventDefault();
  f();
}

export default function(ctrl) {
  if (!window.Mousetrap) return;
  const kbd = window.Mousetrap;
  kbd.bind(['left', 'k'], preventing(function() {
    control.prev(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['right', 'j'], preventing(function() {
    control.next(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['up', '0'], preventing(function() {
    control.first(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['down', '$'], preventing(function() {
    control.last(ctrl);
    ctrl.redraw();
  }));
}
