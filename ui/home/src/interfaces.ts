import { VNode } from 'snabbdom/vnode'

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[]

interface Untyped {
  [key: string]: any;
}

export interface HomeOpts extends Untyped {
  element: HTMLElement;
  socketSend: SocketSend;
  userId: string;
}

export interface Vm extends Untyped {
  mode: string;
  appts: Appt[];
  calendars: Calendars;
  week: number;
  day: number;
}

export interface Calendars {
  list: Calendar[];
  days: string[];
  weeks: string[];
}

export interface Clock {
  limit: number;
  increment: number;
}

export interface ApptContest {
  id: string;
  name: string;
  logo: string;
  roundNo: number;
  boardNo: number;
}

export interface ApptRecord {
  id: string;
  time: string;
  message: string;
  current: boolean;
  whiteStatus: string;
  blackStatus: string;
  applyBy: string;
}

export interface Appt {
  id: string;
  game: string;
  position: string;
  minDateTime: string;
  maxDateTime: string;
  whitePlayerUid: string;
  blackPlayerUid: string;
  confirmed: number;
  finalTime: string;
  contest: ApptContest;
  record: ApptRecord;
  source: string;
  createBy: string;
}

export interface Calendar {
  id: string;
  typ: string;
  week: number;
  date: string;
  st: string;
  et: string;
  period: string;
  content: string;
  tag: string; // 忽略
  onlySdt: boolean; // 忽略
  link: string;
  icon: string;
  bg: string; // 识别为图标颜色
  finished: boolean;
}


