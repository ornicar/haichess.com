function loadShepherd(f) {
  var theme = 'shepherd-theme-' + ($('body').hasClass('dark') ? 'default' : 'dark');
  lichess.loadCss('vendor/shepherd/dist/css/' + theme + '.css');
  lichess.loadCss('stylesheets/shepherd.css');
  lichess.loadScript('vendor/shepherd/dist/js/tether.js', {noVersion:true}).done(function() {
    lichess.loadScript('vendor/shepherd/dist/js/shepherd.min.js', {noVersion:true}).done(function() {
      f(theme);
    });
  });
};
lichess.studyTour = function(study) {
  loadShepherd(function(theme) {
    var onTab = function(tab) {
      return {
        'before-show': function() {
          study.setTab(tab);
        }
      };
    };
    var tour = new Shepherd.Tour({
      defaults: {
        classes: theme,
        scrollTo: false,
        showCancelLink: true
      }
    });
    [{
      title: "欢迎来到Haichess研习!",
      text: "这是一个共享的带分析功能的棋盘。<br><br>" +
      "可以分析和标注对局，<br>" +
      "与好友讨论局面，<br>" +
      "也可以学习国际象棋课程！<br><br>" +
      "这是一个强大的工具，我们来看看如果使用。",
      attachTo: "main.analyse .study__buttons .help top"
    }, {
      title: "共享和",
      text: "其他成员可以实时看到您的走棋！<br>" +
      "并且，所有东西都是永久保存的。",
      attachTo: "main.analyse .areplay left"
    }, {
      title: "研习成员",
      text: "<i data-icon='v'></i> 关注可以查看研习并参与讨论。<br>" +
      "<br><i data-icon='r'></i> 贡献者可以走棋、更新研习。",
      attachTo: ".study__members right",
      when: onTab('members')
    },
      study.isOwner ? {
        title: "邀请成员",
        text: "通过点击 <i data-icon='O'></i> 按钮。<br>" +
        "然后确定谁可以进行编辑。",
        attachTo: ".study__members .add right",
        when: onTab('members')
      } : null, {
        title: "研习章节",
        text: "一个研习可以由多个章节组成。<br>" +
        "每个章节可以有一个独立的初始局面和行棋记录。",
        attachTo: ".study__chapters right",
        when: onTab('chapters')
      },
      study.isContrib ? {
        title: "新建一个章节",
        text: "通过点击 <i data-icon='O'></i> 按钮。",
        attachTo: ".study__chapters .add right",
        when: onTab('chapters')
      } : null, study.isContrib ? {
        title: "评论一个局面",
        text: "通过 <i data-icon='c'></i> 按钮，或者右键单击屏幕右侧的行棋记录。<br>" +
        "评论被共享和持久化。",
        attachTo: ".study__buttons .left-buttons .comments top"
      } : null, study.isContrib ? {
        title: "标注一个局面",
        text: "使用 !? 按钮，或者右键单击屏幕右侧的行棋记录。<br>" +
        "标注的图标可以共享并被永久保存。",
        attachTo: ".study__buttons .left-buttons .glyphs top"
      } : null, {
        title: "谢谢观看手册",
        text: "您可以在 <a href='/study/mine/hot'>我的研习</a> 找到您创建的研习。<br>" +
        "高级用户可点击 \"?\" 了解下如何用键盘快捷方式。。<br>" +
        "祝使用愉快!",
        buttons: [{
          text: 'Done',
          action: tour.next
        }],
        attachTo: "main.analyse .study__buttons .help top"
      }
    ].filter(function(v) {
      return v;
    }).forEach(function(s) {
      tour.addStep(s.title, s);
    });
    tour.start();
  });
};
