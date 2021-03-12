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
lichess.studyTourChapter = function(study) {
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
      title: "我们来创建一个新章节",
      text: "一个研习可以由多个章节组成。<br>" +
        "每个章节由独立的行棋记录么，<br>" +
        "行棋记录中可以创建多个变化路径。",
      attachTo: '.study__modal label[for=chapter-name] left'
    }, {
      title: "起始位置",
      text: "为新的章节创建一个起始位置。<br>" +
        "适合探索研究各种开局。",
      attachTo: '.study__modal .tabs-horiz .init top',
      when: onTab('init')
    }, {
      title: "棋盘编辑器",
      text: "按自己期望的方式摆棋。<br>" +
        "适合研究残局。",
      attachTo: '.study__modal .tabs-horiz .edit bottom',
      when: onTab('edit')
    }, {
      title: "加载一个已有的对局",
      text: "粘贴 Haichess 对局URL<br>" +
        "(如：https://haichess.com/hsg7iKv5)<br>" +
        "来加载对局的行棋记录。",
      attachTo: '.study__modal .tabs-horiz .game top',
      when: onTab('game')
    }, {
      title: "从FEN字",
      text: "粘贴FEN格式的局面<br>" +
        "如：<i>4k3/4rb2/8/7p/8/5Q2/1PP5/1K6 w<br>" +
        "从这个开始章节。",
      attachTo: '.study__modal .tabs-horiz .fen top',
      when: onTab('fen')
    }, {
      title: "从PGN文件",
      text: "粘贴PGN文件。<br>" +
        "加载行棋记录，评论和变体到章节中。",
      attachTo: '.study__modal .tabs-horiz .pgn top',
      when: onTab('pgn')
    }, {
      title: "谢谢观看手册",
      text: "章节会永久保存。<br>" +
        "组织自己的国际象棋心得，祝使用愉快！",
      buttons: [{
        text: 'Done',
        action: tour.next
      }],
      attachTo: '.study__modal .help bottom'
    }].forEach(function(s) {
      tour.addStep(s.title, s);
    });
    tour.start();
  });
};
