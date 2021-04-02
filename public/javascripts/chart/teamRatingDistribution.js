lichess.teamRatingDistributionChart = function(data) {
  lichess.loadScript('vendor/echarts/echarts.min.js').done(function () {
    $.when(
        lichess.loadScript('vendor/echarts/theme/dark.js'),
        lichess.loadScript('vendor/echarts/theme/light.js')
    ).done(function(){

      let theme = $('body').hasClass('light') ? 'light' : 'dark';

      $('#rating_distribution_chart').each(function() {
        let chart = echarts.init(this, theme);

        let getOption = function(freq, myRating) {
          let xAxisData = freq.map(function(_, i) {
            return 500 + i * 25;
          });

          let arraySum = function(arr) {
            return arr.reduce(function(sum, b) {
              return sum + b;
            }, 0);
          };
          let sum = arraySum(freq);
          let cumul = [];
          for (let i = 0; i < freq.length; i++) {
            let v = 0;
            if(sum !== 0) {
              v = Math.round(arraySum(freq.slice(0, i)) / sum * 100);
            }
            let x = [xAxisData[i], v];
            cumul.push(x);
          }

          let freq2 = freq.map(function(_, i) {
            return [xAxisData[i], freq[i]];
          });

          let option = {
            grid: {
              top: 25,
              left: 25,
              right:35,
              bottom: 25
            },
            tooltip: {
              trigger: 'axis',
              formatter: function(params) {
                let name = params[0].axisValue;
                let tooltip = name + '<br/>';
                params.forEach(function (item, i) {
                  if(i < params.length - 1) {
                    tooltip += item.marker + item.seriesName + '：' + item.data[1] + '<br/>';
                  } else {
                    tooltip += item.marker + item.seriesName + '：' + item.data[1] + '%';
                  }
                });
                return tooltip;
              }
            },
            color: ['#7798bf', '#dddf0d'],
            xAxis:{
              type: 'value',
              scale: true,
              min: 500,
              max: 500 + (freq.length - 1) * 25,
              axisLine: {
                lineStyle: {
                  color: '#a0a0a0'
                }
              },
              splitLine: {
                show: true
              },
              axisPointer: {
                label: {
                  precision: 0
                }
              },
              data: xAxisData
            },
            yAxis: [
              {
                type: 'value',
                name: '棋手数量',
                nameLocation: 'center',
                min: 0,
                interval: 5,
                minInterval: 1,
                axisLine: {
                  lineStyle: {
                    color: '#a0a0a0'
                  }
                }
              },
              {
                type: 'value',
                name: '累计',
                nameLocation: 'center',
                min: 0,
                max: 100,
                axisLabel: {
                  formatter: '{value}%'
                },
                axisLine: {
                  lineStyle: {
                    color: '#a0a0a0'
                  }
                }
              }
            ],
            series: [
              {
                name: '棋手数量',
                type: 'line',
                yAxisIndex: 0,
                areaStyle: {},
                symbolSize: 8,
                lineStyle: {
                  width: 3
                },
                data: freq2
              },
              {
                name: '累计',
                type: 'line',
                yAxisIndex: 1,
                symbolSize: 4,
                lineStyle: {
                  width: 2,
                  shadowColor: 'rgba(0, 0, 0, 0.5)',
                  shadowBlur: 8
                },
                data: cumul
              }
            ]
          };

          if(data.showMyRating) {
            option.series[1].markLine = {
              animation: false,
              silent: true,
              symbol: ['none', 'none'],
              label: {
                fontSize: 12,
                formatter: '您的积分：{c}'
              },
              lineStyle: {
                width: 3,
                color: '#55bf3b'
              },
              data: [
                { xAxis: data.myRating }
              ]
            }
          }
          return option;
        };

        chart.setOption(getOption(data.freq, ));
        
        $('#form3-clazzId1').change(function () {
          $('#form3-clazzId').val($(this).val());
          $('.search_form').submit();
        });

        $('#form3-clazzId2').change(function () {
          $('#form3-clazzId').val($(this).val());
          $('.search_form').submit();
        });

        window.addEventListener("resize", () => {
          chart.resize();
        });
      });
    })
  })


};

