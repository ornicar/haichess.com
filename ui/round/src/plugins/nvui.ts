import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { renderClock } from '../clock/clockView';
import { renderTableWatch, renderTablePlay, renderTableEnd } from '../view/table';
import { makeConfig as makeCgConfig } from '../ground';
import { Chessground } from 'chessground';
import renderCorresClock from '../corresClock/corresClockView';
import { renderResult } from '../view/replay';
import { plyStep } from '../round';
import { onInsert } from '../util';
import { Step, DecodedDests, Position, Redraw } from '../interfaces';
import * as game from 'game';
import { renderSan, renderPieces, renderBoard, styleSetting } from 'nvui/chess';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { castlingFlavours, supportedVariant, Style } from 'nvui/chess';
import { commands } from 'nvui/command';

type Sans = {
  [key: string]: Uci;
}

window.lichess.RoundNVUI = function(redraw: Redraw) {

  const notify = new Notify(redraw),
    moveStyle = styleSetting();

  window.lichess.pubsub.on('socket.in.message', line => {
    if (line.u === 'lichess') notify.set(line.t);
  });
  window.lichess.pubsub.on('round.suggestion', notify.set);

  return {
    render(ctrl: RoundController): VNode {
      const d = ctrl.data, step = plyStep(d, ctrl.ply), style = moveStyle.get(),
        variantNope = !supportedVariant(d.game.variant.key) && 'Sorry, this variant is not supported in blind mode.';
      if (!ctrl.chessground) {
        ctrl.setChessground(Chessground(document.createElement("div"), {
          ...makeCgConfig(ctrl),
          animation: { enabled: false },
          drawable: { enabled: false },
          coordinates: false
        }));
        if (variantNope) setTimeout(() => notify.set(variantNope), 3000);
      }
      return h('div.nvui', [
        h('h1', 'Textual representation'),
        h('h2', 'Game info'),
        ...(['white', 'black'].map((color: Color) => h('p', [
          color + ' player: ',
          renderPlayer(ctrl, ctrl.playerByColor(color))
        ]))),
        h('p', `${d.game.rated ? 'Rated' : 'Casual'} ${d.game.perf}`),
        d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
        h('h2', 'Moves'),
        h('p.moves', {
          attrs: {
            role : 'log',
            'aria-live': 'off'
          }
        }, renderMoves(d.steps.slice(1), style)),
        h('h2', 'Pieces'),
        h('div.pieces', renderPieces(ctrl.chessground.state.pieces, style)),
        h('h2', 'Game status'),
        h('div.status', {
          attrs: {
            role : 'status',
            'aria-live' : 'assertive',
            'aria-atomic' : true
          }
        }, [ctrl.data.game.status.name === 'started' ? 'Playing' : renderResult(ctrl)]),
        h('h2', 'Last move'),
        h('p.lastMove', {
          attrs: {
            'aria-live' : 'assertive',
            'aria-atomic' : true
          }
        }, renderSan(step.san, step.uci, style)),
        ...(ctrl.isPlaying() ? [
          h('h2', 'Move form'),
          h('form', {
            hook: onInsert(el => {
              const $form = $(el as HTMLFormElement),
                $input = $form.find('.move').val('').focus();
              $form.submit(onSubmit(ctrl, notify.set, moveStyle.get, $input));
            })
          }, [
            h('label', [
              d.player.color === d.game.player ? 'Your move' : 'Waiting',
              h('input.move.mousetrap', {
                attrs: {
                  name: 'move',
                  'type': 'text',
                  autocomplete: 'off',
                  autofocus: true,
                  disabled: !!variantNope,
                  title: variantNope
                }
              })
            ])
          ])
        ]: []),
        h('h2', 'Your clock'),
        h('div.botc', anyClock(ctrl, 'bottom')),
        h('h2', 'Opponent clock'),
        h('div.topc', anyClock(ctrl, 'top')),
        notify.render(),
        h('h2', 'Actions'),
        ...(ctrl.data.player.spectator ? renderTableWatch(ctrl) : (
          game.playable(ctrl.data) ? renderTablePlay(ctrl) : renderTableEnd(ctrl)
        )),
        h('h2', 'Board'),
        h('pre.board', renderBoard(ctrl.chessground.state.pieces, ctrl.data.player.color)),
        h('h2', 'Settings'),
        h('label', [
          'Move notation',
          renderSetting(moveStyle, ctrl.redraw)
        ]),
        h('h2', 'Commands'),
        h('p', [
          'Type these commands in the move input.', h('br'),
          '/c: Read clocks.', h('br'),
          '/l: Read last move.', h('br'),
          commands.piece.help, h('br'),
          commands.scan.help, h('br'),
          '/abort: Abort game.', h('br'),
          '/resign: Resign game.', h('br'),
          '/draw: Offer or accept draw.', h('br'),
          '/takeback: Offer or accept take back.', h('br')
        ]),
        h('h2', 'Promotion'),
        h('p', [
          'Standard PGN notation selects the piece to promote to. Example: a8=n promotes to a knight.',
          h('br'),
          'Omission results in promotion to queen'
        ])
      ]);
    }
  };
}

const promotionRegex = /^([a-h]x?)?[a-h](1|8)=\w$/;

function onSubmit(ctrl: RoundController, notify: (txt: string) => void, style: () => Style, $input: JQuery) {
  return function() {
    const input = castlingFlavours($input.val());
    if (input[0] === '/') onCommand(ctrl, notify, input.slice(1), style());
    else {
      const d = ctrl.data,
        legalUcis = destsToUcis(ctrl.chessground.state.movable.dests!),
        sans: Sans = sanWriter(plyStep(d, ctrl.ply).fen, legalUcis) as Sans;
      let uci = sanToUci(input, sans) || input,
        promotion = '';

      if (input.match(promotionRegex)) {
        uci = sanToUci(input.slice(0, -2), sans) || input;
        promotion = input.slice(-1).toLowerCase();
      }

      if (legalUcis.includes(uci.toLowerCase())) ctrl.socket.send("move", {
        u: uci + promotion
      }, { ackable: true });
      else notify(d.player.color === d.game.player ? `Invalid move: ${input}` : 'Not your turn');
    }
    $input.val('');
    return false;
  };
}

function onCommand(ctrl: RoundController, notify: (txt: string) => void, c: string, style: Style) {
  if (c == 'c' || c == 'clock') notify($('.nvui .botc').text() + ', ' + $('.nvui .topc').text());
  else if (c == 'l' || c == 'last') notify($('.lastMove').text());
  else if (c == 'abort') $('.nvui button.abort').click();
  else if (c == 'resign') $('.nvui button.resign-confirm').click();
  else if (c == 'draw') $('.nvui button.draw-yes').click();
  else if (c == 'takeback') $('.nvui button.takeback-yes').click();
  else {
    const pieces = ctrl.chessground.state.pieces;
    notify(
      commands.piece.apply(c, pieces, style) ||
      commands.scan.apply(c, pieces) ||
      `Invalid command: ${c}`
    );
  }
}

function anyClock(ctrl: RoundController, position: Position) {
  const d = ctrl.data, player = ctrl.playerAt(position);
  return (ctrl.clock && renderClock(ctrl, player, position)) || (
    d.correspondence && renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, position, d.game.player)
  ) || undefined;
}

function destsToUcis(dests: DecodedDests) {
  const ucis: string[] = [];
  Object.keys(dests).forEach(function(orig) {
    dests[orig].forEach(function(dest) {
      ucis.push(orig + dest);
    });
  });
  return ucis;
}

function sanToUci(san: string, sans: Sans): Uci | undefined {
  if (san in sans) return sans[san];
  const lowered = san.toLowerCase();
  for (let i in sans)
    if (i.toLowerCase() === lowered) return sans[i];
  return;
}

function renderMoves(steps: Step[], style: Style) {
  const res: Array<string | VNode> = [];
  steps.forEach(s => {
    if (s.ply & 1) res.push((Math.ceil(s.ply / 2)) + ' ');
    res.push(renderSan(s.san, s.uci, style) + ', ');
    if (s.ply % 2 === 0) res.push(h('br'));
  });
  return res;
}

function renderPlayer(ctrl: RoundController, player: game.Player) {
  return player.ai ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai) : userHtml(ctrl, player);
}

function userHtml(ctrl: RoundController, player: game.Player) {
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : (perf && perf.rating),
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : ( rd < 0 ? '???' + (-rd) : '')) : '';
  return user ? h('span', [
    h('a', {
      attrs: { href: '/@/' + user.username }
    }, user.title ? `${user.title} ${user.username}` : user.username),
    rating ? ` ${rating}` : ``,
    ' ' + ratingDiff,
  ]) : 'Anonymous';
}
