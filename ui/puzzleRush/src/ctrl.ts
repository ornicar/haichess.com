import { build as treeBuild, ops as treeOps, path as treePath } from 'tree';
import { readDests, decomposeUci, sanToRole } from 'chess';
import { opposite } from 'chessground/util';
import keyboard from './keyboard';
import socketBuild from './socket';
import moveTestBuild from './moveTest';
import makePromotion from './promotion';
import { prop } from 'common';
import throttle from 'common/throttle';
import * as xhr from './xhr';
import * as speech from './speech';
import { sound } from './sound';
import { Api as CgApi } from 'chessground/api';
import * as cg from 'chessground/types';
import { Vm, Controller } from './interfaces';
import {formatTime} from "./util";

export default function(opts, redraw: () => void): Controller {

  let vm: Vm = {} as Vm;
  let data, tree, moveTest;
  const ground = prop<CgApi | undefined>(undefined);
  const modes = [
      {
        'k': 'threeMinutes',
        'v': '3 分钟',
        't': 180,
        'desc': '三分钟限时，开始时很容易，但随着时间的推移会越来越难。三振出局！'
      },
      {
        'k': 'fiveMinutes',
        'v': '5 分钟',
        't': 300,
        'desc': '五分钟限时，开始时很容易，但随着时间的推移会越来越难。三振出局！'
      },
      {
        'k': 'survival',
        'v': '生存',
        'desc': '不限时间，开始时很容易，但随着时间的推移会越来越难。三振出局！'
      },
      {
        'k': 'custom',
        'v': '自定义',
        'desc': '根据条件筛选题目，达到练习时间或错题上限结束！'
      }
    ];
  const defaultRankData = {
    "userHisRank":{"no":-1,"score":-1},
    "userTdyRank":{"no":-1,"score":-1},
    "rankList":[]
  };
  const defaultCondition = { minutes: 3, limit: 3 };

  function setup() {
    let rush = opts.rush ? opts.rush : {};
    let rounds = opts.rounds ? opts.rounds : [];
    let result = rush.result ? rush.result : {};
    vm.rushId = rush.id;
    vm.condition = rush.condition ? rush.condition : defaultCondition;
    vm.page = rush.page ? rush.page : 'home';
    vm.mode = rush.mode ? rush.mode : (opts.mode ? opts.mode : 'threeMinutes');
    vm.maxLoss = ((isCustom() && rush.condition && rush.condition.limit) ? rush.condition.limit : 3) as number;
    vm.countdown = rush.status === 10;
    vm.seconds = (rush.seconds || rush.seconds == 0) ? rush.seconds : modeByKey(vm.mode).t as number;
    vm.finishResult = rush.result;
    vm.nb = result.nb | 0;
    vm.win = result.win | 0;
    vm.loss = result.loss | 0;
    vm.history = rounds as any | [];
    vm.reloading = rush.status === 20;
    vm.rankScope = 'country';
    vm.rankRange = 'today';
    vm.rankData = {};

    if(opts.notAccept) {
      window.lichess.memberIntro();
    }
  }

  function isCustom() {
    return vm.mode === 'custom';
  }

  function conditionJson() {
    let obj = {};
    let form = $('.custom').serializeArray();
    $.each(form, function (_, field) {
      obj[field.name] = field.value;
    });
    return obj;
  }

  function start() {

    if(isCustom()) {
      vm.condition = vm.page === 'finish' ? vm.condition : conditionJson();
      if (!vm.condition.minutes) {
        alert('请设置“练习时间”');
        return;
      }
      if (!vm.condition.limit) {
        alert('请设置“错题上限”');
        return;
      }
      if (vm.condition.ratingMin && vm.condition.ratingMax &&
          parseInt(vm.condition.ratingMin) > parseInt(vm.condition.ratingMax)) {
        alert('请正确设置“难度范围”');
        return;
      }
      if (vm.condition.stepsMin && vm.condition.stepsMax &&
          parseInt(vm.condition.stepsMin) > parseInt(vm.condition.stepsMax)) {
        alert('请正确设置“答案步数”');
        return;
      }
    } else {
      vm.condition = defaultCondition;
    }

    xhr.start(vm.mode, vm.condition).then(function(res) {
      vm.rushId = res.rushId;
      vm.page = 'playing';
      vm.countdown = true;
      vm.nb = 0;
      vm.win = 0;
      vm.loss = 0;
      vm.history = [];
      vm.seconds = isCustom() ? (vm.condition.minutes as number * 60) : modeByKey(vm.mode).t as number;
      vm.maxLoss = (isCustom() ? (vm.condition.limit) : 3) as number;
      vm.finishResult = undefined;
      redraw();

      history.replaceState(null, '', '/training/rush/' + vm.rushId);
    });
  }

  function begin() {
    xhr.begin(vm.rushId).then(function() {
      vm.countdown = false;
      startClock();
      nextPuzzle(false);
    });
  }

  function finish(status, notSendResult = false) {
    clearInterval(vm.timerInterval);
    if(!notSendResult) {
      sendResult(false, true);
    }
    xhr.finish(vm.rushId, status).then(function(res) {
      vm.page = 'finish';
      vm.finishResult = res;
      //withGround(showGround);
      redraw();
      sound.rushOver();
      loadRank();
    });
  }

  function loadRank() {
    vm.rankLoading = true;
    redraw();
    if (isCustom() && vm.rankScope != 'personal') {
      vm.rankLoading = false;
      vm.rankData = defaultRankData;
      redraw();
    } else {
      xhr.loadRank(vm.mode, vm.rankScope, vm.rankRange).then(function(res) {
        vm.rankLoading = false;
        vm.rankData = res;
        redraw();
      });
    }
  }

  function modeByKey(key) {
    return modes.filter(item => item.k === key)[0];
  }

  function setPath(path) {
    vm.path = path;
    vm.nodeList = tree.getNodeList(path);
    vm.node = treeOps.last(vm.nodeList)!;
    vm.mainline = treeOps.mainlineNodeList(tree.root);
  }

  function withGround<A>(f: (cg: CgApi) => A): A | undefined {
    const g = ground();
    if (g) return f(g);
  }

  function initiate(d) {
    data = d;
    tree = treeBuild(treeOps.reconstruct(data.game.treeParts));
    let initialPath = treePath.fromNodeList(treeOps.mainlineNodeList(tree.root));
    let puzzle = data.puzzle;

    vm.loading = false;
    vm.justPlayed = undefined;
    vm.resultSent = false;
    vm.lastFeedback = 'init';
    vm.initialPath = initialPath;
    vm.initialNode = tree.nodeAtPath(initialPath);
    vm.puzzleSeconds = 0;
    vm.puzzleTimerInterval = setInterval(function () {
      vm.puzzleSeconds = vm.puzzleSeconds + 1;
    }, 1000);

    setPath(treePath.init(initialPath));

    setTimeout(function() {
      jump(initialPath);
      redraw();
    }, 500);

    moveTest = moveTestBuild(vm, puzzle);

    withGround(function(g) {
      g.setAutoShapes([]);
      g.setShapes([]);
      showGround(g);
    });
  }

  function startClock() {
    if(vm.mode != 'survival') {
      vm.timerInterval = setInterval(function () {
        let allSeconds = vm.seconds - 1;
        let outoftime = allSeconds > 0 && allSeconds < 60;
        if (outoftime) {
          $('.clock').addClass('outoftime');
          $('.clock').html(formatTime(allSeconds));
        } else {
          $('.clock').html(formatTime(allSeconds));
        }
        if (allSeconds === 59) {
          sound.lowtime();
        }

        vm.seconds = allSeconds;
        if (vm.seconds <= 0) {
          finish(30);
        }
      }, 1000);
    }
  }

  let makeCgOpts = function() {
    const viewOnly = (vm.page === 'home') || (vm.page === 'playing' && vm.countdown) || (vm.page === 'finish') || vm.reloading;
    if(viewOnly) {
      const config = {
        fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
        orientation: 'white',
        turnColor: 'white',
        viewOnly: true,
        movable: {
          color: null,
          dests: {}
        },
        premovable: {
          enabled: false
        },
        check: false,
        lastMove: []
      };
      vm.cgConfig = config;
      return config;
    } else {
      const node = vm.node;
      const color: Color = node.ply % 2 === 0 ? 'white' : 'black';
      const dests = readDests(node.dests);
      const movable = color === data.puzzle.color ? {
        color: (dests && Object.keys(dests).length > 0) ? color : null,
        dests: dests || {}
      } : {
        color: null,
        dests: {}
      };
      const config = {
        viewOnly: false,
        fen: node.fen,
        orientation: data.puzzle.color,
        turnColor: color,
        movable: movable,
        premovable: {
          enabled: false
        },
        check: !!node.check,
        lastMove: uciToLastMove(node.uci)
      };
      if (node.ply >= vm.initialNode.ply) {
        if (!dests && !node.check) {
          // premove while dests are loading from server
          // can't use when in check because it highlights the wrong king
          config.turnColor = opposite(color);
          config.movable.color = color;
          config.premovable.enabled = true;
        } else if (color !== data.puzzle.color) {
          config.movable.color = data.puzzle.color;
          config.premovable.enabled = true;
        }
      }
      vm.cgConfig = config;
      return config;
    }
  };

  function showGround(g) {
    g.set(makeCgOpts());
    if (!vm.node.dests) getDests();
  }

  function userMove(orig, dest) {
    vm.justPlayed = orig;
    if (!promotion.start(orig, dest, sendMove)) sendMove(orig, dest);
  }

  function sendMove(orig: Key, dest: Key, prom?: cg.Role) {
    const move: any = {
      orig: orig,
      dest: dest,
      fen: vm.node.fen,
      path: vm.path
    };
    if (prom) move.promotion = prom;
    socket.sendAnaMove(move);
  }

  let getDests = throttle(800, function() {
    if (!vm.node.dests && treePath.contains(vm.path, vm.initialPath))
      socket.sendAnaDests({
        fen: vm.node.fen,
        path: vm.path
      });
  });

  let uciToLastMove = function(uci) {
    return uci && [uci.substr(0, 2), uci.substr(2, 2)]; // assuming standard chess
  };

  let addNode = function(node, path) {
    let newPath = tree.addNode(node, path);
    jump(newPath);
    reorderChildren(path);
    redraw();
    withGround(function(g) { g.playPremove(); });

    let progress = moveTest();
    if (progress) applyProgress(progress);
    redraw();
    speech.node(node, false);
  };

  function reorderChildren(path: Tree.Path, recursive?: boolean) {
    let node = tree.nodeAtPath(path);
    node.children.sort(function(c1, _) {
      if (c1.puzzle === 'fail') return 1;
      if (c1.puzzle === 'retry') return 1;
      if (c1.puzzle === 'good') return -1;
      return 0;
    });
    if (recursive) node.children.forEach(function(child) {
      reorderChildren(path + child.id, true);
    });
  }

  let revertUserMove = function() {
    setTimeout(function() {
      withGround(function(g) { g.cancelPremove(); });
      userJump(treePath.init(vm.path));
      redraw();
    }, 500);
  };

  let applyProgress = function(progress) {
    if (progress === 'fail') {
      vm.lastFeedback = 'fail';
      sound.loss();
      sendResult(false, false);
    } else if (progress === 'retry') {
      vm.lastFeedback = 'retry';
      revertUserMove();
    } else if (progress === 'win') {
      vm.lastFeedback = 'win';
      withGround(showGround); // to disable premoves
      redraw();
      sound.win();
      sendResult(true, false);
    } else if (progress && progress.orig) {
      vm.lastFeedback = 'good';
      setTimeout(function() {
        socket.sendAnaMove(progress);
      }, 500);
    }
  };

  function sendResult(win, timeout) {
    if (vm.page === 'playing') {
      clearInterval(vm.puzzleTimerInterval);
      if (vm.resultSent) return;
      vm.resultSent = true;
      let lines = resultLines();
      xhr.sendResult(vm.rushId, data.puzzle.id, win, vm.puzzleSeconds, lines, timeout).then(function() {
        vm.nb = vm.nb + 1;
        vm.win = vm.win + (win ? 1 : 0);
        vm.loss = vm.loss + (win ? 0 : 1);
        vm.history.push({
          id: data.puzzle.id,
          fen: data.puzzle.fenAfterLastMove,
          color: data.puzzle.color,
          rating: data.puzzle.rating,
          lastMove: data.puzzle.initialMove,
          win: win,
          timeout: timeout
        });
        redraw();
        if (win) speech.success();

        if(vm.loss == vm.maxLoss && !timeout) {
          finish(40);
        } else {
          // 自动下一题
          if(!timeout) {
            nextPuzzle(false);
          }
        }
      });
    }
  }

  function resultLines() {
    let mainline = vm.mainline;
    if(!mainline) return [];
    let puzzleLineBeginIndex = -1;
    mainline.forEach(function (n, i) {
      if(n.puzzle != undefined && puzzleLineBeginIndex == -1) {
        puzzleLineBeginIndex = i;
      }
    });
    return mainline.filter((_, i) => (i >= puzzleLineBeginIndex)).map(function (n) {
      return { "san": n.san, "uci": n.uci, "fen": n.fen }
    });
  }

  function nextPuzzle(last) {
    vm.loading = true;
    redraw();

    xhr.nextPuzzle(vm.rushId, last, vm.condition).done(function(d) {
      vm.loading = false;
      vm.reloading = false;
      initiate(d);
      redraw();
    }).fail(function() {
      vm.loading = false;
      vm.reloading = false;
      finish(50, true);
    });
  }

  function addDests(dests, path, opening) {
    tree.addDests(dests, path, opening);
    if (path === vm.path) {
      withGround(showGround);
    }
    withGround(function(g) { g.playPremove(); });
  }

  function playUci(uci) {
    let move = decomposeUci(uci);
    if (!move[2]) sendMove(move[0], move[1]);
    else sendMove(move[0], move[1], sanToRole[move[2].toUpperCase()]);
  }

  function jump(path) {
    const pathChanged = path !== vm.path,
        isForwardStep = pathChanged && path.length === vm.path.length + 2;
    setPath(path);
    withGround(showGround);
    if (pathChanged) {
      if (isForwardStep) {
        if (!vm.node.uci) sound.move(); // initial position
        else if (!vm.justPlayed || vm.node.uci.includes(vm.justPlayed)) {
          if (vm.node.san!.includes('x')) sound.capture();
          else sound.move();
        }
        if (/\+|\#/.test(vm.node.san!)) sound.check();
      }
    }
    promotion.cancel();
    vm.justPlayed = undefined;
    window.lichess.pubsub.emit('ply', vm.node.ply);
  }

  function userJump(path) {
    withGround(function(g) {
      g.selectSquare(null);
    });
    jump(path);
    speech.node(vm.node, true);
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
      if(vm.page === 'playing') {
        jump(vm.path);
      }
    });
  });

  speech.setup();
  setup();

  return {
    vm,
    getData() {
      return data;
    },
    getTree() {
      return tree;
    },
    ground,
    makeCgOpts,
    userJump,
    nextPuzzle,
    user: opts.user,
    pref: opts.pref,
    started: {
      threeMinutes: opts.threeMinutesMode,
      fiveMinutes: opts.fiveMinutesMode,
      survival: opts.survivalMode,
    },
    socketReceive: socket.receive,
    userMove,
    playUci,
    getOrientation() {
      return withGround(function(g) { return g.state.orientation })!;
    },
    getNode() {
      return vm.node;
    },
    start,
    begin,
    finish,
    loadRank,
    startClock,
    isCustom,
    promotion,
    modes,
    modeByKey,
    redraw
  };
}