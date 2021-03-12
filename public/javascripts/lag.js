$(function() {

  $.when(
      lichess.loadScript('vendor/echarts/theme/dark.js'),
      lichess.loadScript('vendor/echarts/theme/light.js')
  ).done(function(){

    let bulidChartOption = function(v, n) {
      return {
        series: [
          {
            type: 'gauge',
            min: 0,
            max: 750,
            splitNumber: 15,
            pointer: {
              width: 4
            },
            axisLine: {
              lineStyle: {
                width: 10,
                color: [
                  [0.667, '#55bf3b'],
                  [0.867, '#dddf0d'],
                  [1, '#df5353']
                ]
              }
            },
            axisTick: {
              length: 15,
              lineStyle: {
                color: 'auto'
              }
            },
            splitLine: {
              length: 20,
              lineStyle: {
                color: 'auto'
              }
            },
            data: [{value: v, name: n}]
          }
        ]
      };
    };

    let serverChartOption = bulidChartOption(-1,'服务器延迟(ms)');
    let networkChartOption = bulidChartOption(-1,'网络延迟(ms)');
    let serverChart;
    let networkChart;

    let isLight = $('body').hasClass('light');
    let theme = isLight ? 'light' : 'dark';

    $('.server .meter').each(function() {
      serverChart = echarts.init(this, theme);
    });

    $('.network .meter').each(function() {
      networkChart = echarts.init(this, theme);
    });

    let updateServerChart = function(v) {
      serverChartOption.series[0].data[0].value = v;
      serverChart.setOption(serverChartOption);
    };

    let updateNetworkChart = function(v) {
      networkChartOption.series[0].data[0].value = v;
      networkChart.setOption(networkChartOption);
    };

    let values = {
      server: -1,
      network: -1
    };
    let updateAnswer = function() {
      if (values.server === -1 || values.network === -1) return;
      let c;
      if (values.server <= 100 && values.network <= 500) {
        c = 'nope-nope';
      } else if (values.server <= 100) {
        c = 'nope-yep';
      } else {
        c = 'yep';
      }
      $('.lag .answer span').hide().parent().find('.' + c).show();
    };

    lichess.socket = new lichess.StrongSocket('/socket', false, {
      options: {
        name: "analyse",
        onFirstConnect: function() {
          lichess.socket.send('moveLat', true);
        }
      },
      receive: function(t, d) {
        if (t === 'mlat') {
          let v = parseInt(d);
          updateServerChart(v);
          values.server = v;
          updateAnswer();
        } else if (t === 'n') setTimeout(function() {
          let v = Math.round(lichess.socket.averageLag());
          updateNetworkChart(v);
          values.network = v;
          updateAnswer();
        }, 100);
      }
    });
  });

});
