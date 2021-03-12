lichess.movetimeChart = function(data, trans) {
  lichess.loadScript('vendor/echarts/echarts.min.js').done(function () {
    $.when(
        lichess.loadScript('vendor/echarts/theme/dark.js'),
        lichess.loadScript('vendor/echarts/theme/light.js')
    ).done(function(){

      let tree = data.treeParts;
      let moveCentis = data.game.moveCentis || data.game.moveTimes.map(function(i) { return i * 10; });

      let ply = 0;
      let max = 0;
      let logC = Math.pow(Math.log(3), 2);

      let makeSeriesData = function() {
        let whiteSeriesData = [];
        let blackSeriesData = [];

        moveCentis.forEach(function(time, i) {
          let node = tree[i + 1];
          ply = node ? node.ply : ply + 1;
          let san = node ? node.san : '-';

          let turn = (ply + 1) >> 1;
          let color = ply & 1;

          let y = Math.pow(Math.log(.005 * Math.min(time, 12e4) + 3), 2) - logC;
          max = Math.max(y, max);

          let seconds = moveCentis[i] / 100;
          if (seconds) seconds = seconds.toFixed(seconds >= 2 ? 1 : 2);
          let point = {
            value: [i, color ? y : -y],
            turns: turn + (color ? '. ' : '... ') + san,
            seconds: seconds
          };

          if(color) {
            whiteSeriesData.push(point);
          } else {
            blackSeriesData.push(point);
          }
        });

        return [whiteSeriesData, blackSeriesData];
      };

      let makeDivisionLines = function(division) {
        let divisionLines = [];
        if (division.middle) {
          divisionLines.push({
            xAxis: 0,
            lineStyle: {
              color: '#639b24'
            },
            label: {
              formatter: '开局'
            }
          });

          divisionLines.push({
            xAxis: division.middle,
            lineStyle: {
              color: '#3093cc'
            },
            label: {
              formatter: '中局'
            }
          })
        }

        if (division.end) {
          divisionLines.push({
            xAxis: division.end,
            lineStyle: {
              color: '#cc9730'
            },
            label: {
              formatter: '残局'
            }
          })
        }
        return divisionLines;
      };

      let seriesData = makeSeriesData();
      let divisionLines = makeDivisionLines(data.game.division);

      let isLight = $('body').hasClass('light');
      let theme = isLight ? 'light' : 'dark';
      let symbolSize = 2;

      $('#movetimes-chart:not(.rendered)').each(function() {
        let $this = $(this).addClass('rendered');
        let chart = echarts.init($this[0], theme);
        let option = {
          animation: false,
          grid: {
            top: 20,
            left: 10,
            right:10,
            bottom: 0
          },
          tooltip: {
            trigger: 'axis',
            formatter: function(params) {
              let v = params[0].data;
              return v.turns + '<br/>' + v.seconds + '秒'
            }
          },
          xAxis: {
            axisLabel: {
              show: false
            },
            splitLine: {
              show: false
            },
            axisLine: {
              show: false
            },
            axisTick: {
              show: false
            }
          },
          yAxis: {
            min: -max,
            max: max,
            boundaryGap: true,
            axisLabel: {
              show: false
            },
            splitLine: {
              show: false
            },
            axisLine: {
              show: false
            },
            axisTick: {
              show: false
            }
          },
          series: [
            {
              name: '白方',
              type: 'line',
              areaStyle: {
                color: isLight ? 'rgba(255,255,255,0.7)' : 'rgba(255,255,255,0.3)'
              },
              symbolSize: symbolSize,
              animation: true,
              lineStyle: {
                width: 1,
                color: '#3893e8'
              },
              itemStyle: {
                borderColor: '#3893e8',
                color: '#fff'
              },
              data: seriesData[0]
            },
            {
              name: '黑方',
              type: 'line',
              areaStyle: {
                color: isLight ? 'rgba(0,0,0,0.2)' : 'rgba(0,0,0,1)'
              },
              symbolSize: symbolSize,
              animation: true,
              lineStyle: {
                width: 1,
                color: '#3893e8'
              },
              itemStyle: {
                borderColor: '#3893e8',
                color: '#333'
              },
              data: seriesData[1],
              markLine: {
                silent: true,
                animation: false,
                symbol: ['none', 'none'],
                lineStyle: {
                  type: 'solid',
                  width: 1
                },
                data: divisionLines
              }
            }
          ]
        };
        chart.setOption(option);


        chart.on('click', function (params) {
          select(params.seriesIndex, params.dataIndex);
          let x = params.data.value[0];
          lichess.pubsub.emit('analysis.chart.click', x);
        });

        function select(seriesIndex, dataIndex) {
          let d = option.series[seriesIndex].data[dataIndex];
          if(!d) {
            unselect();
          } else {
            if(!isSelected(d)) {
              option.series.forEach(function(series, i) {
                series.data.forEach(function (item, j) {
                  if(i === seriesIndex && j == dataIndex) {
                    item.symbolSize = 10;
                  } else {
                    item.symbolSize = symbolSize;
                  }
                });
              });
              chart.setOption(option);
            }
          }
        }

        function isSelected(item) {
          return item && item.symbolSize === 10;
        }

        function unselect() {
          option.series.forEach(function(series) {
            series.data.forEach(function (item) {
              item.symbolSize = symbolSize;
            });
          });
          chart.setOption(option);
        }

        lichess.pubsub.on('analysis.chart.select', (chartName, seriesIndex, dataIndex) => {
          if(chartName === 'timeChart') {
            select(seriesIndex, dataIndex);
          }
        });

        lichess.pubsub.on('analysis.chart.unselect', (chartName) => {
          if(chartName === 'timeChart') {
            unselect();
          }
        });

        window.addEventListener("resize", () => {
          chart.resize();
        });
      });
    })
  })
};
