lichess = lichess || {};
lichess.startInsightTour = function() {
  lichess.hopscotch(function() {
    var tour = {
      id: "insights",
      showPrevButton: true,
      steps: [{
        title: "欢迎来到国象数据洞察！",
        content: "分析您的强项和弱项！<br>" +
          "数据洞察使用适合的度量指标和维度， " +
          "帮助您分析您的行棋风格。<br><br>" +
          "这是一个强大的工具，我们来看看如何使用吧！",
        target: "#insight header h2",
        placement: "bottom"
      }, {
        title: "数据洞察可以回答您的问题",
        content: "下面是一些预置的问题。点下试试！",
        target: "#insight .panel-tabs a.preset",
        placement: "top",
        yOffset: 10,
        onShow: function() {
          lichess.insight.setPanel('preset');
        }
      }, {
        title: "答案在右侧图表中",
        content: "彩色的条柱表示问题的答案。<br>" +
          "灰色的条柱表示数据的多少，比如走棋的步数。",
        target: "#insight .chart",
        placement: "left",
        xOffset: 50
      }, {
        title: "同样的数据，展示在表格中",
        content: "表格以另一种方式展示答案。<br>" +
          "往下浏览，是数据对应的一些对局。",
        target: "#insight table.slist",
        placement: "top"
      }, {
        title: "提问：度量指标",
        content: "如果要进行提问，第一步是选择一个度量指标。<br>" +
          "例如，我们问一个关于走棋时间的问题。",
        target: "#insight div.ms.metric",
        placement: "left",
        onShow: function() {
          lichess.insight.clearFilters();
          lichess.insight.setPanel('filter');
        }
      }, {
        title: "提问：维度",
        content: "现在选择一个维度，来对比下不同情况下的走棋时间。<br>" +
          "例如，按不同的比赛类型看走棋时间，也可以按棋子种类来看。",
        target: "#insight div.ms.dimension",
        placement: "left"
      }, {
        title: "提问：过滤器",
        content: "通过过滤对局，可以让问题更加精准。<br>" +
          "例如，您可以选择只有在您走黑棋，并且短易位情况下的对局",
        target: "#insight .panel-tabs a.filter",
        placement: "top",
        yOffset: 10,
        onShow: function() {
          lichess.insight.clearFilters();
          lichess.insight.setPanel('filter');
        }
      }, {
        title: "谢谢观看手册！",
        content: "现在您可以创造性的提出自己的问题，并找到答案！<br>" +
          "任何时候，您可以复制URL并进行分享。<br><br>" +
          "还有最后一点...",
        target: "#insight header h2",
        placement: "bottom"
      }, {
        title: "分享您的洞察数据。",
        content: "默认情况下，您的数据对您的好友是可见的。<br>" +
          "您可以设置为所有人可预见，或仅自己可见。 <a href='/account/preferences/privacy'>隐私设置</a>.<br><br>" +
          "祝使用愉快！",
        target: "#insight .info .share",
        placement: "right",
        yOffset: -20
      }]
    };
    hopscotch.startTour(tour);
  })
};
$(function() {
  if (lichess.once('insight-tour')) setTimeout(lichess.startInsightTour, 1000);
});
