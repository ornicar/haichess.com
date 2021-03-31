lichess.teamRatingHistoryChart = function(data) {
  lichess.loadScript('vendor/echarts/echarts.min.js').done(function () {
    $.when(
        lichess.loadScript('vendor/echarts/theme/dark.js'),
        lichess.loadScript('vendor/echarts/theme/light.js')
    ).done(function(){

      let dates = new Set();
      data.forEach(function (d) {
        dates.add(d.date);
      });

      let xAxisData = Array.from(dates).sort(sortDate);

      function dateToNumber(d) {
        let format = d.split('/').map(function(v) {
          if(v < 10) {
            return '0' + v
          } else {
            return v
          }
        }).join('');
        return parseInt(format)
      }

      function sortDate(a, b) {
        return dateToNumber(a) - dateToNumber(b);
      }

      let theme = $('body').hasClass('light') ? 'light' : 'dark';

      $('#rating_history_chart').each(function() {
        let chart = echarts.init(this, theme);

        let option = {
          tooltip: {
            trigger: 'axis'
          },
          grid: {
            top: 20,
            left: 14,
            right:10,
            bottom: 45
          },
          dataZoom: [
            {
              show: true,
              bottom: 15
            }
          ],
          xAxis: [
            {
              type: 'category',
              data: xAxisData,
              axisLabel : {
                inside: true,
                formatter: function(v) {
                  return v;
                }
              },
              axisLine: {
                show: false
              },
              axisTick: {
                show: false
              }
            }
          ],
          yAxis: [
            {
              type: 'value',
              min: 600,
              position: 'right',
              axisLine: {
                show: false
              },
              axisTick: {
                show: false
              },
              axisLabel: {
                inside: true,
                fontSize: 8,
                verticalAlign: 'bottom'
              }
            }
          ],
          series: [
            {
              name: '俱乐部等级分',
              type: 'line',
              connectNulls: true,
              data: data.map(d => d.rating)
            }
          ]
        };
        chart.setOption(option);

        window.addEventListener("resize", () => {
          chart.resize();
        });

        let setZoomRange = function(type, n) {
          let date = new Date();
          let endDate = date.toLocaleDateString();

          if(type === 'all') {
            chart.dispatchAction({
              type: 'dataZoom',
              start: 0,
              end: 100
            });
          } else {
            if(type === 'month') { // n月内
              date.setMonth(date.getMonth() - n);
            } else if(type === 'ytd') { // 今年
              date.setMonth(0);
            } else if(type === 'year') { // n年内
              date.setFullYear(date.getFullYear() - n);
            }
            let startDate = date.toLocaleDateString();

            let startValue = xAxisData.findIndex((item) => dateToNumber(item) >= dateToNumber(startDate));
            if(startValue === -1) {
              startValue = xAxisData.length - 1;
            }

            let reverseXAxisData = Array.of(...xAxisData).reverse();
            let endValue = reverseXAxisData.findIndex((item) => dateToNumber(item) > dateToNumber(endDate));
            if(endValue === -1) {
              endValue = xAxisData.length - 1;
            }

            chart.dispatchAction({
              type: 'dataZoom',
              startValue: startValue,
              endValue: endValue
            });
          }
        };

        $('div.search-bar span').click(function() {
          let $this = $(this);
          let type = $this.data('type');
          let count = $this.data('count');
          setZoomRange(type, count);

          $('div.search-bar span').removeClass('active');
          $this.addClass('active');
        });

        setZoomRange('month', 3);
      });
    });
  });
};
