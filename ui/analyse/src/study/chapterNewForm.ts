import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { defined, prop, Prop } from 'common';
import { storedProp, StoredProp } from 'common/storage';
import { bind, bindSubmit, spinner, option, onInsert } from '../util';
import { variants as xhrVariants, importPgn } from './studyXhr';
import * as modal from '../modal';
import { chapter as chapterTour } from './studyTour';
import { StudyChapterMeta } from './interfaces';
import { Redraw } from '../interfaces';
import { descTitle } from './description';
import AnalyseCtrl from '../ctrl';


export const modeChoices = [
  ['normal', '正常分析'],
  ['practice', '与电脑练习'],
  /*['conceal', '隐藏下一步'],*/
  ['gamebook', '交互式课程']
];

export function fieldValue(e: Event, id: string) {
  const el = (e.target as HTMLElement).querySelector('#chapter-' + id);
  return el ? (el as HTMLInputElement).value : null;
};

export interface StudyChapterNewFormCtrl {
  root: AnalyseCtrl;
  vm: {
    variants: Variant[];
    open: boolean;
    initial: Prop<boolean>;
    tab: StoredProp<string>;
    editor: any;
    editorFen: Prop<Fen | null>;
  };
  open(): void;
  openInitial(): void;
  close(): void;
  toggle(): void;
  submit(d: any): void;
  chapters: Prop<StudyChapterMeta[]>;
  startTour(): void;
  multiPgnMax: number;
  redraw: Redraw;
}

export function ctrl(send: SocketSend, chapters: Prop<StudyChapterMeta[]>, setTab: () => void, root: AnalyseCtrl): StudyChapterNewFormCtrl {

  const multiPgnMax = 20;

  const vm = {
    variants: [],
    open: false,
    initial: prop(false),
    tab: storedProp('study.form.tab', 'init'),
    editor: null,
    editorFen: prop(null)
  };

  function loadVariants() {
    if (!vm.variants.length) xhrVariants().then(function(vs) {
      vm.variants = vs;
      root.redraw();
    });
  };

  function open() {
    vm.open = true;
    loadVariants();
    vm.initial(false);
  }
  function close() {
    vm.open = false;
  }

  return {
    vm,
    open,
    root,
    openInitial() {
      open();
      vm.initial(true);
    },
    close,
    toggle() {
      if (vm.open) close();
      else open();
    },
    submit(d) {
      d.initial = vm.initial();
      d.sticky = root.study!.vm.mode.sticky;
      if (!d.pgn) send("addChapter", d);
      else importPgn(root.study!.data.id, d);
      close();
      setTab();
    },
    chapters,
    startTour: () => chapterTour(tab => {
      vm.tab(tab);
      root.redraw();
    }),
    multiPgnMax,
    redraw: root.redraw
  }
}

export function view(ctrl: StudyChapterNewFormCtrl): VNode {

  const activeTab = ctrl.vm.tab();
  const makeTab = function(key: string, name: string, title: string) {
    return h('span.' + key, {
      class: { active: activeTab === key },
      attrs: { title },
      hook: bind('click', () => ctrl.vm.tab(key), ctrl.root.redraw)
    }, name);
  };
  const gameOrPgn = activeTab === 'game' || activeTab === 'pgn';
  const currentChapter = ctrl.root.study!.data.chapter;
  const mode = currentChapter.practice ? 'practice' : (defined(currentChapter.conceal) ? 'conceal' : (currentChapter.gamebook ? 'gamebook' : 'normal'));

  return modal.modal({
    class: 'chapter-new',
    onClose() {
      ctrl.close();
      ctrl.redraw();
    },
    content: [
      activeTab === 'edit' ? null : h('h2', [
        '新建章节',
        h('i.help', {
          attrs: { 'data-icon': '' },
          hook: bind('click', ctrl.startTour)
        })
      ]),
      h('form.form3', {
        hook: bindSubmit(e => {
          const o: any = {
            fen: fieldValue(e, 'fen') || (ctrl.vm.tab() === 'edit' ? ctrl.vm.editorFen() : null)
          };
          'name game variant pgn orientation mode'.split(' ').forEach(field => {
            o[field] = fieldValue(e, field);
          });
          ctrl.submit(o);
        }, ctrl.redraw)
      }, [
        h('div.form-group', [
          h('label.form-label', {
            attrs: {for: 'chapter-name' }
          }, '名称'),
          h('input#chapter-name.form-control', {
            attrs: {
              minlength: 2,
              maxlength: 80
            },
            hook: onInsert<HTMLInputElement>(el => {
                if (!el.value) {
                  el.value = '章节 ' + (ctrl.vm.initial() ? 1 : (ctrl.chapters().length + 1));
                  el.select();
                  el.focus();
                }
            })
          })
        ]),
        h('div.tabs-horiz', [
          makeTab('init', '初始局面', '从初始位置开始'),
          makeTab('edit', '编辑器', '从自定义位置开始'),
          makeTab('fen', 'FEN', '加载FEN'),
          makeTab('pgn', 'PGN', '加载PGN'),
          makeTab('game', 'URL', '加载对局URL')
        ]),
        activeTab === 'edit' ? h('div.board-editor-wrap', {
          hook: {
            insert: vnode => {
              $.when(
                window.lichess.loadScript('compiled/lichess.editor.min.js'),
                $.get('/editor.json', {
                  fen: ctrl.root.node.fen
                })
              ).then(function(_, b) {
                const data = b[0];
                data.embed = true;
                data.options = {
                  inlineCastling: true,
                  onChange: ctrl.vm.editorFen
                };
                ctrl.vm.editor = window['LichessEditor'](vnode.elm as HTMLElement, data);
                ctrl.vm.editorFen(ctrl.vm.editor.getFen());
              });
            },
            destroy: _ => {
              ctrl.vm.editor = null;
            }
          }
        }, [spinner()]) : null,
        activeTab === 'game' ? h('div.form-group', [
          h('label.form-label', {
            attrs: { 'for': 'chapter-game' }
          }, '从haichess.com或chessgames.com加载一个对局'),
          h('input#chapter-game.form-control', {
            attrs: { placeholder: '对局URL' }
          })
        ]) : null,
        activeTab === 'fen' ? h('div.form-group', [
          h('input#chapter-fen.form-control', {
            attrs: {
              value: ctrl.root.node.fen,
              placeholder: 'Initial FEN position'
            }
          })
        ]) : null,
        activeTab === 'pgn' ? h('div.form-groupabel', [
          h('textarea#chapter-pgn.form-control', {
            attrs: { placeholder: '粘贴PGN文本，最多 ' + ctrl.multiPgnMax + ' 局' }
          }),
          window.FileReader ? h('input#chapter-pgn-file.form-control', {
            attrs: {
              type: 'file',
              accept: '.pgn'
            },
            hook: bind('change', e => {
              const file = (e.target as HTMLInputElement).files![0];
              if (!file) return;
              const reader = new FileReader();
              reader.onload = function() {
                (document.getElementById('chapter-pgn') as HTMLTextAreaElement).value = reader.result as string;
              };
              reader.readAsText(file);
            })
          }) : null
        ]) : null,
        h('div.form-split', [
          h('div.form-group.form-half', [
            h('label.form-label', {
              attrs: { 'for': 'chapter-variant' }
            }, '变体'),
            h('select#chapter-variant.form-control', {
              attrs: { disabled: gameOrPgn }
            }, gameOrPgn ? [
              h('option', '自动识别')
            ] :
            ctrl.vm.variants.map(v => option(v.key, currentChapter.setup.variant.key, v.name)))
          ]),
          h('div.form-group.form-half', [
            h('label.form-label', {
              attrs: { 'for': 'chapter-orientation' }
            }, '棋盘方向'),
            h('select#chapter-orientation.form-control', {
              hook: bind('change', e => {
                ctrl.vm.editor && ctrl.vm.editor.setOrientation((e.target as HTMLInputElement).value);
              })
            }, [{'key':'white', 'name': '白方'}, {'key':'black', 'name': '黑方'}].map(function(color) {
              return option(color.key, currentChapter.setup.orientation, color.name);
            }))
          ])
        ]),
        h('div.form-group', [
          h('label.form-label', {
            attrs: { 'for': 'chapter-mode' }
          }, '分析模式'),
          h('select#chapter-mode.form-control', modeChoices.map(c => option(c[0], mode, c[1])))
        ]),
        modal.button('创建章节')
      ])
    ]
  });
}

export function descriptionGroup(desc?: string) {
  return h('div.form-group', [
    h('label.form-label', {
      attrs: { for: 'chapter-description' }
    }, descTitle(true)),
    h('select#chapter-description.form-control', [
      ['', '无'],
      ['1', '棋盘正下方']
    ].map(v => option(v[0], desc ? '1' : '', v[1])))
  ]);
}
