import { VNode } from 'snabbdom/vnode'

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[]

export interface Controller {
  vm: Vm;
  ongoing: boolean;
  getOrientation(): Color;
  getNode(): Tree.Node;
  [key: string]: any;
}

export interface Vm {
  initialPath: Tree.Path;
  initialNode: Tree.Node;

  currentPath: Tree.Path;
  currentNode: Tree.Node;
  nodeList: Tree.Node[];
  mainline: Tree.Node[];

  ended: boolean;
  autoScrollRequested: boolean;
  autoScrollNow: boolean;
  stage: string; // pending, running, finished
  color: string; // all, white, black
  turns: number;
  whiteTurns: number;
  blackTurns: number;
  maxTurns: number;
  currTurns: number;
  currTurnsWithoutHit: number;
  hinting: Hinting;
  hinted: boolean;
  mistake: boolean;
  currMistake: boolean;
  cgConfig: any;
  historys: any;
  readonly: boolean;
  homeworkId: string;
}

interface Hinting {
  mode: 'move' | 'piece';
  uci: Uci;
}