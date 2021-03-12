import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'

export const hasTouchEvents = 'ontouchstart' in window;

export function bindMobileMousedown(el: HTMLElement, f: (e: Event) => any, redraw?: () => void) {
  el.addEventListener(hasTouchEvents ? 'touchstart' : 'mousedown', e => {
    f(e);
    e.preventDefault();
    if (redraw) redraw();
  })
}

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return onInsert(el =>
    el.addEventListener(eventName, e => {
      const res = f(e);
      if (redraw) redraw();
      return res;
    })
  );
}

export function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks {
  return {
    insert: vnode => f(vnode.elm as A)
  };
}

export function dataIcon(icon: string) {
  return {
    'data-icon': icon
  };
}

export function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}

const piotr = {
  'a': 'A1',
  'b': 'B1',
  'c': 'C1',
  'd': 'D1',
  'e': 'E1',
  'f': 'F1',
  'g': 'G1',
  'h': 'H1',
  'i': 'A2',
  'j': 'B2',
  'k': 'C2',
  'l': 'D2',
  'm': 'E2',
  'n': 'F2',
  'o': 'G2',
  'p': 'H2',
  'q': 'A3',
  'r': 'B3',
  's': 'C3',
  't': 'D3',
  'u': 'E3',
  'v': 'F3',
  'w': 'G3',
  'x': 'H3',
  'y': 'A4',
  'z': 'B4',
  'A': 'C4',
  'B': 'D4',
  'C': 'E4',
  'D': 'F4',
  'E': 'G4',
  'F': 'H4',
  'G': 'A5',
  'H': 'B5',
  'I': 'C5',
  'J': 'D5',
  'K': 'E5',
  'L': 'F5',
  'M': 'G5',
  'N': 'H5',
  'O': 'A6',
  'P': 'B6',
  'Q': 'C6',
  'R': 'D6',
  'S': 'E6',
  'T': 'F6',
  'U': 'G6',
  'V': 'H6',
  'W': 'A7',
  'X': 'B7',
  'Y': 'C7',
  'Z': 'D7',
  '0': 'E7',
  '1': 'F7',
  '2': 'G7',
  '3': 'H7',
  '4': 'A8',
  '5': 'B8',
  '6': 'C8',
  '7': 'D8',
  '8': 'E8',
  '9': 'F8',
  '!': 'G8',
  '?': 'H8'
};

function decodePiotr(uciPiotr) {
  let p = uciPiotr.slice(0, 2);
  return (piotr[p[0]] + piotr[p[1]] + uciPiotr.slice(2)).toLowerCase();
}

export function decodeLines(lines) {
  let dl = {};
  for(let key in lines) {
    let val;
    if (lines[key] === true) val = 'win';
    else if(lines[key] === false) val = 'retry';
    else val = decodeLines(lines[key]);
    dl[decodePiotr(key)] = val;
  }
  return dl;
}