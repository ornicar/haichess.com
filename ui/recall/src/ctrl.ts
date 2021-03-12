import { build as treeBuild, ops as treeOps, path as treePath } from 'tree';
import { readDests, readDrops, decomposeUci, sanToRole } from 'chess';
import { opposite } from 'chessground/util';
import { Config as CgConfig } from 'chessground/config';
import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import keyboard from './keyboard';
import socketBuild from './socket';
import makePromotion from './promotion';
import { prop } from 'common';
import throttle from 'common/throttle';
import * as speech from './speech';
import { sound } from './sound';
import { Vm, Controller } from './interfaces';

export default function(opts, redraw: () => void): Controller {

  let vm: Vm = {} as Vm;
  let data, tree, resultMainline, home;
  const ground = prop<CgApi | undefined>(undefined);

  function initiate(fromData) {
    data = fromData;
    home = data.home;
    tree = treeBuild(treeOps.reconstruct(data.game.treeParts));

    let recall = data.recall;
    let mainline = treeOps.mainlineNodeList(tree.root);
    //let currentPath = treePath.fromNodeList(mainline);
    let initialPath = mainline[0].id;

    vm.hinted = false;
    vm.mistake = false;
    vm.currMistake = false;
    vm.initialPath = initialPath;
    vm.initialNode = tree.nodeAtPath(initialPath);
    vm.historys = data.history;
    vm.homeworkId = data.homeworkId;
    vm.ended = false;
    vm.stage = 'pending';
    vm.readonly = recall.readonly;
    vm.color = recall.color ? recall.color : 'all';
    vm.whiteTurns = mainline.filter((n, i) => i > 0 && n.ply % 2 === 1).length;
    vm.blackTurns = mainline.filter((n, i) => i > 0 && n.ply % 2 === 0).length;
    vm.maxTurns = plyToTurn(tree.lastPly());
    vm.turns = turns(recall.turns);
    vm.currTurns = 0;
    vm.currTurnsWithoutHit = 0;

    setPath(initialPath);

/*    setPath(currentPath);
    setTimeout(function() {
      jump(currentPath);
      redraw();
    }, 500);*/
  }

  function turns(t) {
    let turns = 500;
    if(t) {
      if(vm.color === 'white') {
        turns = Math.min(t, vm.whiteTurns)
      } else if(vm.color === 'black') {
        turns = Math.min(t, vm.blackTurns)
      } else if(vm.color === 'all') {
        turns = Math.min(t, vm.maxTurns)
      }
    } else {
      if(vm.color === 'white') {
        turns = vm.whiteTurns
      } else if(vm.color === 'black') {
        turns = vm.blackTurns
      } else if(vm.color === 'all') {
        turns = vm.maxTurns
      }
    }
    turns = Math.max(1, turns);
    return turns;
  }

  function setPath(path) {
    vm.currentPath = path;
    vm.nodeList = tree.getNodeList(path);
    vm.currentNode = treeOps.last(vm.nodeList)!;
    vm.mainline = treeOps.mainlineNodeList(tree.root);
  }

  function withGround<A>(f: (cg: CgApi) => A): A | undefined {
    const g = ground();
    if (g) return f(g);
  }

  let makeCgOpts = function() {
    const node = vm.currentNode,
        color = plyColor(node.ply),
        dests = readDests(node.dests),
        drops = readDrops(node.drops),
        movableColor = (((dests && Object.keys(dests).length > 0) || drops === null || drops.length) ? color : undefined),
        config: CgConfig = {
          fen: node.fen,
          orientation: (vm.color === 'all' ? 'white' : vm.color) as cg.Color,
          turnColor: color,
          movable: {
            color: (vm.stage === 'finished') ? undefined : (isMyTurn() ? color : undefined),
            dests: (movableColor === color ? (dests || {}) : {})
          },
          check: !!node.check,
          lastMove: uciToLastMove(node.uci)
        };
    if (!dests && !node.check) {
      // premove while dests are loading from server
      // can't use when in check because it highlights the wrong king
      config.turnColor = opposite(color);
      config.movable!.color = color;
    }
    config.premovable = {
      enabled: config.movable!.color && config.turnColor !== config.movable!.color
    };
    vm.cgConfig = config;
    return config;
  };

  function hint() {
    let prev = vm.hinting;
    let rightNode = treeOps.nodeAtPly(resultMainline, (vm.currentNode.ply + 1));
    if(rightNode) {
      if(!vm.hinted && !vm.mistake) {
        sendResult();
        vm.hinted = true;
      }
      if (prev && prev.mode === 'move') {
        vm.hinting = undefined;
      } else vm.hinting = {
        mode: prev ? 'move' : 'piece',
        uci: rightNode.uci
      };
    } else {
      vm.hinting = undefined;
    }

    withGround(function(g) {
      g.setAutoShapes(makeShapes());
    });
    redraw();
  }

  function makeShapes() {
    if(vm.hinting) {
      const move = decomposeUci(vm.hinting.uci);
      return [{
        orig: move[0],
        dest: vm.hinting.mode === 'piece' ? undefined : move[1],
        brush: 'paleBlue'
      }];
    } else return [];
  }

  function plyToTurn(ply) {
    return Math.floor((ply - 1) / 2) + 1;
  }

  function plyColor(ply: number) {
    return (ply % 2 === 0) ? 'white' : 'black';
  }

  function currPlyColor() {
    return plyColor(vm.currentNode.ply);
  }

  function isMyTurn() {
    return (vm.color === 'all') || (turnColor(vm.currentNode) === bottomColor());
  }

  function isMyTurnOfJust() {
    return (vm.color === 'all') || (plyColor(vm.currentNode.ply - 1) === bottomColor());
  }

  function turnColor(node) {
    return plyColor(node.ply);
  }

  function bottomColor() {
    return (vm.color === 'all') ? 'white' : vm.color;
  }

  function uciToLastMove(uci) {
    return uci && [uci.substr(0, 2), uci.substr(2, 2)]; // assuming standard chess
  }

  function showGround(g) {
    g.set(makeCgOpts());
    if (!vm.currentNode.dests) getDests();
  }

  function userMove(orig, dest) {
    vm.hinting = undefined;
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  }

  function sendMove(orig: Key, dest: Key, prom?: cg.Role) {
    const move: any = {
      orig: orig,
      dest: dest,
      fen: vm.currentNode.fen,
      path: vm.currentPath
    };
    if (prom) move.promotion = prom;
    socket.sendAnaMove(move);
  }

  let getDests = throttle(800, function() {
    if (!vm.currentNode.dests && treePath.contains(vm.currentPath, vm.initialPath))
      socket.sendAnaDests({
        fen: vm.currentNode.fen,
        path: vm.currentPath
      });
  });

  let addNode = function(node, path) {
    let newPath = tree.addNode(node, path);
    jump(newPath);
    redraw();
    withGround(function(g) {
      g.playPremove();
      g.setAutoShapes([]);
    });
    applyProgress(node);
    redraw();
    speech.node(node, false);
  };

  let revertUserMove = function() {
    setTimeout(function() {
      withGround(function(g) {
        g.cancelPremove();
      });
      userJump(treePath.init(vm.currentPath));
      redraw();
    }, 500);
  };

  let applyProgress = function(node) {
    let rightNode = treeOps.nodeAtPly(resultMainline, node.ply);
    let win = rightNode && node.id == rightNode.id;
    if(isMyTurnOfJust()) {
      node.recall = win ? 'win' : 'fail';
    }
    if(win) {
      if(isMyTurnOfJust()) {
        vm.currMistake = false;
        let sTurn = plyToTurn(Math.max(tree.root.ply, 1));
        let eTurn = plyToTurn(tree.lastPly());
        vm.currTurns = eTurn - sTurn + 1;
        if (!vm.hinted && !vm.mistake) {
          vm.currTurnsWithoutHit = eTurn - sTurn + 1;
          if (vm.currTurnsWithoutHit == vm.turns) {
            sendResult();
          }
        }
      }
      playOpponentMove(node);
    } else {
      vm.currMistake = true;
      if (!vm.hinted && !vm.mistake) {
        sendResult();
        vm.mistake = true;
      }

      setTimeout(function() {
        revertUserMove();
        tree.deleteNodeAt(vm.currentPath);
      }, 500);
    }
  };
  
  function playOpponentMove(node) {
    let nextRightNode = treeOps.nodeAtPly(resultMainline, (node.ply + 1));
    if(nextRightNode) {
      if(!isMyTurn()) {
        const uci = decomposeUci(nextRightNode.uci);
        let opponentMove: any = {
          orig: uci[0],
          dest: uci[1],
          fen: node.fen,
          path: vm.currentPath
        };
        setTimeout(function() {
          socket.sendAnaMove(opponentMove);
        }, 500);
      }
    } else {
      vm.ended = true;
    }
  }

  function addDests(dests, path, opening) {
    tree.addDests(dests, path, opening);
    if (path === vm.currentPath) {
      withGround(showGround);
    }
    withGround(function(g) { g.playPremove(); });
  }

  function jump(path) {
    const pathChanged = path !== vm.currentPath,
      isForwardStep = pathChanged && path.length === vm.currentPath.length + 2;
    setPath(path);
    withGround(showGround);
    if (pathChanged) {
      if (isForwardStep) {
        if (!vm.currentNode.uci) sound.move(); // initial position
        else if (vm.currentNode.san.indexOf('x') != -1) {
          sound.capture();
        } else {
          sound.move()
        }
        if (/\+|\#/.test(vm.currentNode.san!)) sound.check();
      }
    }
    promotion.cancel();
    vm.autoScrollRequested = true;
    window.lichess.pubsub.emit('ply', vm.currentNode.ply);
  }

  function userJump(path) {
    withGround(function(g) {
      g.selectSquare(null);
    });
    jump(path);
    speech.node(vm.currentNode, true);
  }

  function openCreateModal() {
    $.ajax({
      url: '/recall/createForm'
    }).then(function(html) {
      $.modal($(html));
      let $md = $('.recall-create');
      $md.find('.tabs-horiz span').click(function () {
        let $this = $(this);
        $md.find('.tabs-horiz span').removeClass("active");
        $md.find('.tabs-content div').removeClass("active");

        let cls = $this.attr('class');
        $this.addClass('active');
        $md.find('.tabs-content div.' + cls).addClass('active');
      });

      $md.find('input[type=file]').on('change', function() {
        let file = this.files[0];
        if (!file) return;
        let reader = new FileReader();
        reader.onload = function(e1) {
          let r = <string> (<FileReader> e1.target).result;
          $md.find('textarea').val(r);
        };
        reader.readAsText(file);
      });

      $('.cancel').click(function () {
        $.modal.close();
      });
      create($md);
    });
  }

  function create($md) {
    $md.find('form').submit(function(e) {
      e.preventDefault();

      let $form = $md.find('.form3');
      if(!$form.find('#form3-game').val() && !$form.find('#form3-pgn').val() && !$form.find('#form3-chapter').val()) {
        alert('输入一种PGN获取方式');
        return false;
      }

      $.ajax({
        method: 'POST',
        url: '/recall/create',
        data: $form.serialize()
      }).then(function(res) {
        location.href = `/recall/${res.id}`
      }, function (err) {
        handleError(err)
      });
      return false;
    });
  }

  function handleError(res) {
    let json = res.responseJSON;
    if (json) {
      if (json.error) {
        if(typeof json.error === 'string') {
          alert(json.error);
        } else alert(JSON.stringify(json.error));
      } else alert(res.responseText);
    } else alert('发生错误');
  }

  function start() {
    vm.stage = 'running';
    let resultTree = treeBuild(treeOps.reconstruct(data.game.treeParts));
    resultMainline = treeOps.mainlineNodeList(resultTree.root);
    initTree();
    userJump(vm.initialPath);
    playOpponentMove(vm.currentNode);
    redraw();
  }

  function finish() {
    sendResult().done(function () {
      vm.hinted = false;
      vm.mistake = false;
      vm.stage = 'finished';
      withGround(showGround);
      redraw();
    });
  }

  function sendResult() {
    return $.ajax({
      method: 'POST',
      data: {
        'win' : vm.currTurnsWithoutHit >= vm.turns,
        'turns': vm.currTurnsWithoutHit
      },
      url: '/recall/finish?id=' + data.recall.id + (vm.homeworkId ? '&homeworkId=' + vm.homeworkId : '')
    })
  }

  function startAgain() {
    vm.currTurns = 0;
    vm.currTurnsWithoutHit = 0;
    vm.hinting = undefined;
    vm.ended = false;
    start();
  }

  function initTree() {
    let treePart = data.game.treeParts[0];
    treePart.children = [];
    let tps = [treePart];
    tree = treeBuild(treeOps.reconstruct(tps));
  }

  const socket = socketBuild({
    send: opts.socketSend,
    addNode: addNode,
    addDests: addDests,
    reset: function() {
      withGround(showGround);
      redraw();
    }
  });

  const promotion = makePromotion(vm, ground, redraw);

  keyboard({
    vm,
    userJump,
    redraw
  });

  // If the page loads while being hidden (like when changing settings),
  // chessground is not displayed, and the first move is not fully applied.
  // Make sure chessground is fully shown when the page goes back to being visible.
  document.addEventListener('visibilitychange', function() {
    window.lichess.requestIdleCallback(function() {
      jump(vm.currentPath);
    });
  });

  initiate(opts.data);
  speech.setup();

  return {
    vm,
    getData() {
      return data;
    },
    getTree() {
      return tree;
    },
    isHome() {
      return home;
    },
    ground,
    makeCgOpts,
    userJump,
    pref: opts.pref,
    socketReceive: socket.receive,
    turns,
    userMove,
    openCreateModal,
    start,
    finish,
    startAgain,
    currPlyColor,
    hint,
    isMyTurn,
    getOrientation() {
      return withGround(function(g) { return g.state.orientation })!;
    },
    getNode() {
      return vm.currentNode;
    },
    promotion,
    redraw,
    ongoing: false
  };
}
