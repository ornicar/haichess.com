lichess.advantageChart = function(data, trans, el) {
  lichess.loadScript('vendor/echarts/echarts.min.js').done(function () {
    $.when(
        lichess.loadScript('vendor/echarts/theme/dark.js'),
        lichess.loadScript('vendor/echarts/theme/light.js')
    ).done(function(){

      let makeXAxisData = function() {
        let xAxisData = [];
        for(let i = 0; i < Math.max(data.treeParts.length - 1, 0); i++) {
          xAxisData.push(i);
        }
        return xAxisData;
      };

      let makeSeriesData = function(d) {
        return d.treeParts.slice(1).map(function(node, i) {
          let color = node.ply & 1, cp;
          if (node.eval && node.eval.mate) {
            cp = node.eval.mate > 0 ? Infinity : -Infinity;
          } else if (node.san.includes('#')) {
            cp = color === 1 ? Infinity : -Infinity;
            if (d.game.variant.key === 'antichess') cp = -cp;
          } else if (node.eval && typeof node.eval.cp !== 'undefined') {
            cp = node.eval.cp;
          } else return {
            value: null
          };

          let turn = Math.floor((node.ply - 1) / 2) + 1;
          let dots = color === 1 ? '.' : '...';
          let y = 2 / (1 + Math.exp(-0.004 * cp)) - 1;
          let point = {
            value: y,
            turns: turn + dots + ' ' + node.san
          };
          return point;
        });
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

      let xAxisData = makeXAxisData();
      let seriesData = makeSeriesData(data);
      let divisionLines = makeDivisionLines(data.game.division);

      let isLight = $('body').hasClass('light');
      let theme = isLight ? 'light' : 'dark';
      let symbolSize = 2;

      let chart = echarts.init(el, theme);
      let option = {
        grid: {
          top: 20,
          left: 10,
          right: 10,
          bottom: 5
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let param = params[0];
            let paramData = param.data;
            let eval = data.treeParts[(param.dataIndex + 1)].eval;
            if (!eval) {
              return '';
            } else if (eval.mate) {
              return paramData.turns + '<br/>' + params[0].marker + ' 优势：#' + eval.mate;
            } else if (typeof eval.cp !== 'undefined') {
              let e = Math.max(Math.min(Math.round(eval.cp / 10) / 10, 99), -99);
              if (e > 0) {
                e = '+' + e;
              }
              return paramData.turns + '<br/>' + params[0].marker + '优势：' + e;
            }
          }
        },
        xAxis: {
          animation: false,
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
          },
          data: xAxisData
        },
        yAxis: {
          min: -1.1,
          max: 1.1,
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
            type: 'line',
            areaStyle: {
              color: isLight ? 'rgba(0,0,0,0.2)' : 'rgba(0,0,0,1)'
            },
            symbolSize: symbolSize,
            animation: true,
            lineStyle: {
              width: 1,
              color: '#d85000'
            },
            data: seriesData,
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
        lichess.pubsub.emit('analysis.chart.click', params.dataIndex);
      });

      function select(seriesIndex, dataIndex) {
        let series = option.series[seriesIndex];
        if(series) {
          let data = series.data[dataIndex];
          if(!data) {
            unselect();
          } else {
            if(!isSelected(data)) {
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
        if(chartName === 'acplChart') {
          select(seriesIndex, dataIndex);
        }
      });

      lichess.pubsub.on('analysis.chart.unselect', (chartName) => {
        if(chartName === 'acplChart') {
          unselect();
        }
      });

      lichess.pubsub.on('analysis.chart.change', (chartName, data) => {
        if(chartName === 'acplChart') {
          let seriesData = makeSeriesData(data);
          option.series[0].data = seriesData;
          chart.setOption(option);
        }
      });

      window.addEventListener("resize", () => {
        chart.resize();
      });
    });
  });
};
