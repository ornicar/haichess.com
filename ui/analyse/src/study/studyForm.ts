import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import * as modal from '../modal';
import { prop, Prop } from 'common';
import { bind, bindSubmit, emptyRedButton } from '../util';
import { StudyData } from './interfaces';
import { MaybeVNodes } from '../interfaces';
import RelayCtrl from './relay/relayCtrl';

export interface StudyFormCtrl {
  open: Prop<boolean>;
  openIfNew(): void;
  save(data: FormData, isNew: boolean): void;
  getData(): StudyData;
  isNew(): boolean;
  redraw(): void;
  relay?: RelayCtrl;
  isCoach?: boolean;
  isTeam?: boolean;
  canCreatePrivate?: boolean;
}

interface FormData {
  [key: string]: string;
}

interface Select {
  key: string;
  name: string;
  choices: Choice[];
  selected: string;
}
type Choice = [string, string];

function visibilityChoices(isCoach?: boolean, isTeam?: boolean, canCreatePrivate?: boolean): Choice[] {
  let visibility: Choice[] = [];
  if (isCoach || isTeam || canCreatePrivate) {
    visibility.push(['private', '私有']);
  }
  if (isCoach) {
    visibility.push(['student', '我的学员']);
  }
  if (isTeam) {
    visibility.push(['team', '我的俱乐部']);
  }
  visibility.push(['public', '公共']);
  return visibility;
}

const userSelectionChoices: Choice[] = [
  ['nobody', '不允许'],
  ['owner', '仅自己'],
  ['contributor', '贡献者'],
  ['member', '成员'],
  ['everyone', '所有人']
];

function select(s: Select): MaybeVNodes {
  return [
    h('label.form-label', {
      attrs: { for: 'study-' + s.key }
    }, s.name),
    h(`select#study-${s.key}.form-control`, s.choices.map(function(o) {
      return h('option', {
        attrs: {
          value: o[0],
          selected: s.selected === o[0]
        }
      }, o[1]);
    }))
  ];
};

export function ctrl(save: (data: FormData, isNew: boolean) => void, getData: () => StudyData, redraw: () => void, relay?: RelayCtrl, isCoach?: boolean, isTeam?: boolean, canCreatePrivate?: boolean): StudyFormCtrl {

  const initAt = Date.now();

  function isNew(): boolean {
    const d = getData();
    return d.from === 'scratch' && !!d.isNew && Date.now() - initAt < 9000;
  }

  const open = prop(false);

  return {
    open,
    openIfNew() {
      if (isNew()) open(true);
    },
    save(data: FormData, isNew: boolean) {
      save(data, isNew);
      open(false);
    },
    getData,
    isNew,
    redraw,
    relay,
    isCoach,
    isTeam,
    canCreatePrivate
  };
}

export function view(ctrl: StudyFormCtrl): VNode {
  const data = ctrl.getData();
  const isNew = ctrl.isNew();
  const vc = visibilityChoices(ctrl.isCoach, ctrl.isTeam, ctrl.canCreatePrivate);
  const updateName = function(vnode, isUpdate) {
    const el = vnode.elm as HTMLInputElement;
    if (!isUpdate && !el.value) {
      el.value = data.name;
      if (isNew) el.select();
      el.focus();
    }
  };
  return modal.modal({
    class: 'study-edit',
    onClose: function() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', ctrl.relay ? '配置直播' : (isNew ? '创建' : '编辑') + '研习'),
      h('form.form3', {
        hook: bindSubmit(e => {
          const obj: FormData = {};
          'name visibility computer explorer cloneable chat sticky description'.split(' ').forEach(n => {
            const el = ((e.target as HTMLElement).querySelector('#study-' + n) as HTMLInputElement);
            if (el) obj[n] = el.value;
          });
          ctrl.save(obj, isNew);
        }, ctrl.redraw)
      }, [
        h('div.form-group' + (ctrl.relay ? '.none' : ''), [
          h('label.form-label', { attrs: { 'for': 'study-name' } }, '名称'),
          h('input#study-name.form-control', {
            attrs: {
              minlength: 3,
              maxlength: 100
            },
            hook: {
              insert: vnode => updateName(vnode, false),
              postpatch: (_, vnode) => updateName(vnode, true)
            }
          })
        ]),
        h('div.form-split', [
          h('div.form-group.form-half', select({
            key: 'visibility',
            name: '可见范围',
            choices: vc,
            selected: data.visibility
          })),
          h('div.form-group.form-half', select({
            key: 'cloneable',
            name: '允许复制',
            choices: userSelectionChoices,
            selected: data.settings.cloneable
          }))
        ]),
        h('div.form-split', [
          h('div.form-group.form-half', select({
            key: 'computer',
            name: '电脑分析',
            choices: userSelectionChoices,
            selected: data.settings.computer
          })),
          h('div.form-group.form-half', select({
            key: 'explorer',
            name: '开局浏览器',
            choices: userSelectionChoices,
            selected: data.settings.explorer
          }))
        ]),
        h('div.form-split', [
          h('div.form-group.form-half', select({
            key: 'chat',
            name: '聊天',
            choices: userSelectionChoices,
            selected: data.settings.chat
          })),
          h('div.form-group.form-half', select({
            key: 'sticky',
            name: '同步浏览',
            choices: [
              ['true', '是：保持每个人在相同的局面'],
              ['false', '否：自由浏览']
            ],
            selected: '' + data.settings.sticky
          }))
        ]),
        h('div.form-group.form-half', select({
          key: 'description',
          name: '固定说明',
          choices: [
            ['false', '无'],
            ['true', '棋盘正下方']
          ],
          selected: '' + data.settings.description
        })),
        modal.button(isNew ? '开始' : '保存')
      ]),
      h('div.destructive', [
        isNew ? null : h('form', {
          attrs: {
            action: '/study/' + data.id + '/clear-chat',
            method: 'post'
          },
          hook: bind('submit', _ => {
            return confirm('是否删除聊天历史? 删除后无法恢复!');
          })
        }, [
          h(emptyRedButton, '清空聊天')
        ]),
        h('form', {
          attrs: {
            action: '/study/' + data.id + '/delete',
            method: 'post'
          },
          hook: bind('submit', _ => {
            return isNew || confirm('是否删除研习? 删除后无法恢复!');
          })
        }, [
          h(emptyRedButton, isNew ? '取消' : '删除')
        ])
      ])
    ]
  });
}
