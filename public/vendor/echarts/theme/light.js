(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define(['exports', 'echarts'], factory);
    } else if (typeof exports === 'object' && typeof exports.nodeName !== 'string') {
        // CommonJS
        factory(exports, require('echarts'));
    } else {
        // Browser globals
        factory({}, root.echarts);
    }
}(this, function (exports, echarts) {
    const fontSize = 10;
    let log = function (msg) {
        if (typeof console !== 'undefined') {
            console && console.error && console.error(msg);
        }
    };
    if (!echarts) {
        log('ECharts is not Loaded');
        return;
    }
    let axisCommon = function () {
        return {
            axisLabel: {
                textStyle: {
                    fontSize: fontSize
                }
            }
        };
    };

    //let colorPalette = ['#d87c7c', '#919e8b', '#d7ab82', '#6e7074', '#61a0a8', '#efa18d', '#787464', '#cc7e63', '#724e58', '#4b565b', '#f49f42'];
    let colorPalette = [
        '#7cb5ec', '#434348',
        '#90ed7d', '#f7a35c',
        '#8085e9', '#f15c80',
        '#e4d354', '#8085e8',
        '#8d4653', '#91e8e1',
        "#d87c7c"
    ];
    let theme = {
        color: colorPalette,
        tooltip: {
            textStyle: {
                fontSize: fontSize
            }
        },
        legend: {
            textStyle: {
                fontSize: fontSize
            }
        },
        textStyle: {
            fontSize: fontSize
        },
        timeAxis: axisCommon(),
        logAxis: axisCommon(),
        valueAxis: axisCommon(),
        categoryAxis: axisCommon(),
        line: {
            symbol: 'circle'
        },
        graph: {
            color: colorPalette
        },
        gauge: {
            title: {
                textStyle: {
                    fontSize: fontSize
                }
            }
        }
    };
    echarts.registerTheme('light', theme);
}));