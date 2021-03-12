import { VNode } from 'snabbdom/vnode'

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[]

export interface History {
  id: number;
  fen: Fen;
  color: Color;
  rating: number;
  lastMove: Uci;
  win: boolean;
  timeout: boolean;
}

export interface Controller {
  vm: Vm;
  playUci(uci: string): void;
  getOrientation(): Color;
  getNode(): Tree.Node;
  [key: string]: any;
}

export interface Vm {
  rushId: string;
  condition: any;
  page: 'home' | 'playing' | 'finish' | string | null | undefined;
  mode: 'threeMinutes' | 'fiveMinutes' | 'survival'  | 'custom' | string | null | undefined;
  reloading: boolean; // 页面重新加载
  maxLoss: number;

  // 做题
  cgConfig: any;
  path: Tree.Path;
  nodeList: Tree.Node[];
  node: Tree.Node;
  mainline: Tree.Node[];
  lastFeedback: 'init' | 'fail' | 'win' | 'good' | 'retry';
  initialPath: Tree.Path;
  initialNode: Tree.Node;
  justPlayed?: Key;
  resultSent: boolean;
  loading: boolean; // 加载puzzle

  // 计时
  countdown: boolean;
  puzzleSeconds: number; // 对应puzzle
  puzzleTimerInterval: any; // 对应puzzle
  seconds: number; // 对应Mode
  timerInterval: any; // 对应Mode
  finishResult: any;

  // tools-result
  history: History[];
  nb: number;
  win: number;
  loss: number;

  // side
  rankScope: 'country' | 'personal' | string | null | undefined;
  rankRange: 'today' | 'season' | 'history' | string | null | undefined;
  rankLoading: boolean;
  rankData: any;
}
