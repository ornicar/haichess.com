import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { prop } from 'common';
import { bind, bindSubmit, spinner, option, onInsert, emptyRedButton } from '../util';
import * as modal from '../modal';
import * as chapterForm from './chapterNewForm';
import { StudyChapterMeta } from './interfaces';

export function ctrl(send: SocketSend, chapterConfig, redraw: () => void) {

  const current = prop<StudyChapterMeta | null>(null);

  function open(data) {
    current({
      id: data.id,
      name: data.name
    });
    chapterConfig(data.id).then(d => {
      current(d);
      redraw();
    });
  };

  function isEditing(id) {
    const c = current();
    return c ? c.id === id : false;
  };

  return {
    open,
    toggle(data) {
      if (isEditing(data.id)) current(null);
      else open(data);
    },
    current,
    submit(data) {
      const c = current();
      if (c) {
        data.id = c.id;
        send("editChapter", data)
        current(null);
      }
      redraw();
    },
    delete(id) {
      send("deleteChapter", id);
      current(null);
    },
    clearAnnotations(id) {
      send("clearAnnotations", id);
      current(null);
    },
    isEditing,
    redraw
  }
}

export function view(ctrl): VNode | undefined {

  const data = ctrl.current();
  if (!data) return;

  const isLoaded = !!data.orientation;
  const mode = data.practice ? 'practice' : (!isNaN(data.conceal) ? 'conceal' : (data.gamebook ? 'gamebook' : 'normal'));

  return modal.modal({
    class: 'edit-' + data.id, // full redraw when changing chapter
    onClose() {
      ctrl.current(null);
      ctrl.redraw();
    },
    content: [
      h('h2', '编辑章节'),
      h('form.form3', {
        hook: bindSubmit(e => {
          const o: any = {};
          'name mode orientation description'.split(' ').forEach(field => {
            o[field] = chapterForm.fieldValue(e, field);
          });
          ctrl.submit(o);
        })
      }, [
        h('div.form-group', [
          h('label.form-label', {
            attrs: { for: 'chapter-name' }
          }, '名称'),
          h('input#chapter-name.form-control', {
            attrs: {
              minlength: 2,
              maxlength: 80
            },
            hook: onInsert<HTMLInputElement>(el => {
              if (!el.value) {
                el.value = data.name;
                el.select();
                el.focus();
              }
            })
          })
        ])
      ].concat(
        isLoaded ? [
          h('div.form-split', [
            h('div.form-group.form-half', [
              h('label.form-label', {
                attrs: { for: 'chapter-orientation' }
              }, '棋盘方向'),
              h('select#chapter-orientation.form-control', [{'key':'white', 'name': '白方'}, {'key':'black', 'name': '黑方'}].map(function(color) {
                return option(color.key, data.orientation, color.name);
              }))
            ]),
            h('div.form-group.form-half', [
              h('label.form-label', {
                attrs: { for: 'chapter-mode' }
              }, '分析模式'),
              h('select#chapter-mode.form-control', chapterForm.modeChoices.map(c => {
                return option(c[0], mode, c[1]);
              }))
            ])
          ]),
          chapterForm.descriptionGroup(data.description),
          modal.button('保存章节')
        ] : [spinner()]
      )),
      h('div.destructive', [
        h(emptyRedButton, {
          hook: bind('click', _ => {
            if (confirm('是否清空章节内所有说明和形状?'))
              ctrl.clearAnnotations(data.id);
          })
        }, '清空注释'),
        h(emptyRedButton, {
          hook: bind('click', _ => {
            if (confirm('是否删除章节? 删除后无法恢复!'))
              ctrl.delete(data.id);
          })
        }, '删除章节')
      ])
    ]
  });
}
