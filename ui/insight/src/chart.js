let m = require('mithril');

function dataFormat(dt, value) {
  if (dt === 'seconds') return value.toFixed(1);
  if (dt === 'average') return value.toFixed(1);
  if (dt === 'percent') return value.toFixed(1) + '%';
  return value.toFixed(1);
}

function dimensionDataTypeFormat(dt, value) {
  if (dt === 'date') {
    return new Date(parseInt(value)).toLocaleDateString();
  }
  return value;
}

function calMax(arr) {
  let max = 0;
  arr.forEach((el) => {
    if (!(el === undefined || el === '')) {
      if (max < el) {
        max = el;
      }
    }
  });
  return nearMultipleOf(max);
}

function calMin(arr) {
  let min = 0;
  arr.forEach((el) => {
    if (!(el === undefined || el === '')) {
      if (min > el) {
        min = el;
      }
    }
  });
  if (min < 0) {
    return -nearMultipleOf(Math.abs(min));
  }
  return min;
}

function nearMultipleOf(v) {
  if(v === 0) {
    return v;
  }
  let result = (v > 5) ? Math.ceil(v) : Math.ceil(v * 10);
  let stepAndDivisor = numStepAndDivisor(v);
  let step = stepAndDivisor[0];
  let divisor = stepAndDivisor[1];
  let i = 0;
  while (result % divisor != 0 && i < 1000) {
    result = result + step;
    i ++;
  }
  if(v <= 5) {
    return result / 10;
  }
  return result;
}

function numStepAndDivisor(v) {
  if(v <= 1) {
    return [1, 5];
  } else if(v > 1 && v <= 5) {
    return [1, 5];
  } else if(v > 5 && v <= 50) {
    return [1, 5];
  } else if(v > 50 && v <= 100) {
    return [1, 10];
  } else if(v > 100 && v <= 200) {
    return [1, 20];
  } else if(v > 200 && v <= 2000) {
    return [1, 50];
  } else if(v > 2000 && v <= 10000) {
    return [1, 100];
  } else if(v > 10000 && v <= 50000) {
    return [1, 200];
  } else if(v > 50000 && v <= 100000) {
    return [1, 500];
  } else {
    return [1, 1000];
  }
}

let colors = {
  green: '#759900',
  red: '#dc322f',
  orange: '#d59120',
  blue: '#007599'
};
let resultColors = {
  胜: colors.green,
  和: colors.blue,
  负: colors.red
};

let theme = (function() {
  let light = $('body').hasClass('light');
  let t = {
    light: light,
    text: {
      weak: light ? '#808080' : '#9a9a9a',
      strong: light ? '#505050' : '#c0c0c0'
    },
    line: {
      weak: light ? '#ccc' : '#404040',
      strong: light ? '#a0a0a0' : '#606060',
      fat: '#d85000' // light ? '#a0a0a0' : '#707070'
    }
  };
  if (!light) t.colors = [
    "#2b908f", "#90ee7e", "#f45b5b", "#7798bf", "#aaeeee", "#ff0066", "#eeaaee",
    "#55bf3b", "#df5353", "#7798bf", "#aaeeee"
  ];
  return t;
})();

function makeChart(el, data) {
  let sizeSerie = {
    name: data.sizeSerie.name,
    data: data.sizeSerie.data,
    yAxisIndex: 1,
    type: 'bar',
    stack: 'size',
    color: 'rgba(120,120,120,0.2)',
/*    label: {
      show: true,
      fontSize: 10,
      color: '#fff',
    },*/
    barMaxWidth: 40,
    itemStyle: {
      borderColor: theme.line.strong,
      borderWidth: 1
    }
  };
  let valueSeries = data.series.map(function(s) {
    let item = {
      name: s.name,
      data: s.data,
      yAxisIndex: 0,
      type: 'bar',
      stack: s.stack,
      label: {
        show: true,
        fontSize: 9,
        color: '#fff',
        formatter: function(param) {
          return s.stack ? param.data.toFixed(1) + '%' : dataFormat(s.dataType, param.data)
        }
      },
      barMaxWidth: 40,
      itemStyle: {
        borderColor: theme.line.strong,
        borderWidth: 1
      }
    };
    if (data.valueYaxis.name === '结果') item.color = resultColors[s.name];
    return item;
  });

  let sizeMin = calMin(data.sizeSerie.data);
  let sizeMax = calMax(data.sizeSerie.data);
  let sizeInterval = (sizeMax - sizeMin) / 5;

  let valueAllData = [];
  data.series.forEach(function(s) {
    valueAllData = valueAllData.concat(s.data);
  });
  let valueMin = calMin(valueAllData);
  let valueMax = calMax(valueAllData);
  let valueInterval = (valueMax - valueMin) / 5;

  let option = {
    grid: {
      top: 20,
      left: 40,
      right: 40,
      bottom: 50
    },
    tooltip: {
      trigger: 'axis',
      textStyle: {
        fontSize: 10
      },
      axisPointer: {
        type: 'shadow'
      },
      formatter: function(params) {
        let name = dimensionDataTypeFormat(data.xAxis.dataType, params[0].name);
        let tooltip = name + '<br/>';
        params.forEach(function (item, i) {
          if(i < params.length - 1) {
            tooltip += item.marker + item.seriesName + '：' + dataFormat(data.valueYaxis.dataType, item.data) + '<br/>';
          } else {
            tooltip += item.marker + item.seriesName + '：' + item.data;
          }
        });
        return tooltip;
      }
    },
    legend: {
      bottom: 0,
      textStyle: {
        color: theme.text.weak
      }
    },
    //color: ["#2b908f", "#90ee7e", "#f45b5b", "#7798bf", "#aaeeee", "#ff0066", "#eeaaee",  "#55bf3b", "#df5353", "#7798bf", "#aaeeee"],
    color: ['#7cb5ec', '#434348', '#90ed7d', '#f7a35c', '#8085e9', '#f15c80', '#e4d354', '#8085e8', '#8d4653', '#91e8e1', "#aaeeee"],
    xAxis: [
      {
        type: 'category',
        axisLabel: {
          color: theme.text.weak,
          fontSize: 10,
          formatter: function(value) {
            return dimensionDataTypeFormat(data.xAxis.dataType, value);
          }
        },
        data: data.xAxis.categories.map(function(v) {
          return data.xAxis.dataType === 'date' ? v * 1000 : v;
        }),
        splitLine: {
          lineStyle: {
            color: theme.line.weak
          }
        },
        axisLine: {
          lineStyle: {
            color: theme.line.strong
          }
        },
        axisTick: {
          lineStyle: {
            color: theme.line.strong
          }
        },
      }
    ],
    yAxis: [data.valueYaxis, data.sizeYaxis].map(function(a, i) {
      let isPercent = data.valueYaxis.dataType === 'percent';
      let isSize = i % 2 === 1;
      return {
        type: 'value',
        //name: a.name,
        nameLocation: 'center',
        min: isSize ? sizeMin : (isPercent ? 0 : valueMin),
        max: isSize ? sizeMax : (isPercent ? 100 : valueMax),
        interval: isSize ? sizeInterval : (isPercent ? 20 : valueInterval),
        splitNumber: 5,
        axisLine: {
          show: false
        },
        axisTick: {
          show: false
        },
        axisLabel: {
          color: theme.text.weak,
          fontSize: 8,
          formatter: function(data) {
            return dataFormat(a.dataType, data)
          }
        },
        splitLine: {
          lineStyle: {
            color: theme.line.weak
          }
        }
      };
    }),
    series: valueSeries.concat(sizeSerie)
  };

  let chart = echarts.init(el);
  chart.setOption(option, true);

  window.addEventListener("resize", () => {
    chart.resize();
  });
}

function empty(txt) {
  return m('div.chart.empty', [
    m('i[data-icon=7]'),
    txt
  ]);
}

module.exports = function(ctrl) {
  if (!ctrl.validCombinationCurrent()) return empty('不合理的度量指标与维度的组合');
  if (!ctrl.vm.answer.series.length) return empty('没有数据. 请尝试扩大搜索范围或清除过滤条件.');
  return [
    m('div.chart', {
      config: function(el) {
        if (ctrl.vm.loading) return;
        makeChart(el, ctrl.vm.answer);
      }
    }),
    ctrl.vm.loading ? m.trust(lichess.spinnerHtml) : null
  ];
};
