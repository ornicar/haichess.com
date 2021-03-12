$(function () {

    let theme = $('body').hasClass('light') ? 'light' : 'dark';

    $('.homework-report').find('.moves move span:not(.disabled)').click(function() {
        let fen = $(this).data('fen');
        let $board = $(this).parents('tr').find('.td-board .mini-board');
        Chessground($board[0], {
            coordinates: false,
            resizable: false,
            drawable: { enabled: false, visible: false },
            viewOnly: true,
            fen: fen
        });
        $board.attr('data-fen', fen);

        $('.homework-report').find('.moves move span.active').removeClass('active');
        $(this).addClass('active');
    });

    let puzzleAllChart;
    let puzzleAllChartOption;
    $('.puzzle-all-chart').each(function() {
        const lineDataMap = {
            '完成率': {
                symbol: 'circle',
                color: '#7cb5ec'
            },
            '正确率': {
                symbol: 'rect',
                color: '#90ed7d'
            },
            '首次正确率': {
                symbol: 'triangle',
                color: '#e4d354'
            }
        };
        let $this = $(this);
        let $pzs = $('.puzzles>li');
        let xData = $this.data('xaxis').map(x => `第${x}题`);
        let currentDataIndex = 0;
        puzzleAllChart = echarts.init(this, theme);
        puzzleAllChartOption = {
            title: {
                left: 'center',
                text: '完成情况',
            },
            legend: {
                bottom: 20,
                data: [
                    {
                        name: '完成率',
                        icon: 'circle'
                    },
                    {
                        name: '正确率',
                        icon: 'rect'
                    },
                    {
                        name: '首次正确率',
                        icon: 'triangle'
                    }
                ]
            },
            tooltip: {
                trigger: 'axis',
                textStyle: {
                    fontSize: 10
                },
                formatter: function(params) {
                    let name = params[0].name;
                    let dataIndex = params[0].dataIndex;
                    let tooltip = name + '<br/>';
                    params.forEach(function (item, i) {
                        if(i < params.length - 1) {
                            tooltip += item.marker + item.seriesName + '：' + item.value + '%<br/>';
                        } else {
                            tooltip += item.marker + item.seriesName + '：' + item.value + '%';
                        }
                    });

                    if (dataIndex != currentDataIndex) {
                        chartPointChange(dataIndex);
                        currentDataIndex = dataIndex;
                    }
                    return tooltip;
                }
            },
            grid: {
                top: '40',
                left: '40',
                right: '20',
                bottom: '70'
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: xData
            },
            yAxis: {
                show: true,
                type: 'value',
                axisLabel: {
                    fontSize: 9,
                    formatter: '{value}%'
                },
                axisLine: {
                    show: false
                },
                axisTick: {
                    show: false
                }
            },
            series: $this.data('series').map(function(item, i) {
                item.data = item.data.map(function(d) {
                    return { value: d, symbolSize: 4 };
                });
                return { ...item, ...lineDataMap[item.name] }
            })
        };
        puzzleAllChart.setOption(puzzleAllChartOption);

        puzzleAllChart.on('click', function (params) {
            chartPointChange(params.dataIndex);
        });

        function chartPointChange(dataIndex) {
            $pzs.not('.none').addClass('none');
            $pzs.eq(dataIndex).removeClass('none');
            setPuzzleChartsSelect(dataIndex);
        }
    });

    $('.puzzle-chart').each(function() {
        let $this = $(this);
        let puzzleChart = echarts.init(this, theme);
        let puzzleChartOption = {
            tooltip: {
                formatter: '{b}：{c}%',
                textStyle: {
                    fontSize: 10
                }
            },
            grid: {
                left: '40',
                right: '10',
                top: '10',
                bottom: '25',
            },
            xAxis: {
                data: ["完成率", "正确率", "首次正确率"]
            },
            yAxis: {
                show: true,
                type: 'value',
                axisLabel: {
                    fontSize: 9,
                    formatter: '{value}%'
                },
                axisLine: {
                    show: false
                },
                axisTick: {
                    show: false
                }
            },
            series: [{
                type: 'bar',
                barWidth: 40,
                label: {
                    show: true,
                    fontSize: 9,
                    formatter: '{c}%'
                },
                data: $this.data('series')
            }]
        };

        puzzleChart.setOption(puzzleChartOption);
    });

    $('.puzzles .prev').click(function() {
        let $puzzle = $(this).parents('li');
        let $prevPuzzle = $puzzle.prev('li');
        if ($prevPuzzle.length > 0) {
            $puzzle.addClass('none');
            $prevPuzzle.removeClass('none');
        }
        let index = $('.puzzles>li').not('.none').index();
        setPuzzleChartsSelect(index);
    });

    $('.puzzles .next').click(function() {
        let $puzzle = $(this).parents('li');
        let $nextPuzzle = $puzzle.next('li');
        if ($nextPuzzle.length > 0) {
            $puzzle.addClass('none');
            $nextPuzzle.removeClass('none');
        }
        let index = $('.puzzles>li').not('.none').index();
        setPuzzleChartsSelect(index);
    });

    $('.replayGame-chart').each(function() {
        let $this = $(this);
        let replayGameChart = echarts.init(this, theme);
        let replayGameChartOption = {
            grid: {
                left: '40',
                right: '10',
                top: '10',
                bottom: '25',
            },
            tooltip: {
                trigger: 'item',
                formatter: '{b}：{c}人（{d}%）',
                textStyle: {
                    fontSize: 10
                }
            },
            color : ['#90ed7d', '#f7a35c'],
            series: [
                {
                    name: '完成率',
                    type: 'pie',
                    radius: '65%',
                    center: ['50%', '50%'],
                    data: $this.data('series')
                }
            ]
        };

        replayGameChart.setOption(replayGameChartOption);
    });

    $('.recallGame-chart').each(function() {
        let $this = $(this);
        let recallGameChart = echarts.init(this, theme);
        let recallGameChartOption = {
            tooltip: {
                textStyle: {
                    fontSize: 10
                }
            },
            grid: {
                left: '40',
                right: '10',
                top: '10',
                bottom: '25',
            },
            xAxis: {
                data: $this.data('xaxis'),
                axisLabel: {
                    formatter: '{value}回合'
                }
            },
            yAxis: {
                show: true,
                type: 'value',
                axisLabel: {
                    fontSize: 9,
                    formatter: '{value}人'
                },
                axisLine: {
                    show: false
                },
                axisTick: {
                    show: false
                }
            },
            tooltip: {
                formatter: '{b}回合：{c}人',
                textStyle: {
                    fontSize: 10
                }
            },
            series: [{
                type: 'bar',
                barWidth: 40,
                label: {
                    show: true,
                    fontSize: 9,
                    formatter: '{c}人'
                },
                data: $this.data('series'),
            }]
        };

        recallGameChart.setOption(recallGameChartOption);
    });

    $('.fromPosition-chart').each(function() {
        let $this = $(this);
        let fromPositionChart = echarts.init(this, theme);
        let fromPositionChartOption = {
            tooltip: {
                textStyle: {
                    fontSize: 10
                }
            },
            grid: {
                left: '40',
                right: '10',
                top: '10',
                bottom: '25',
            },
            xAxis: {
                data: $this.data('xaxis'),
                axisLabel: {
                    formatter: '{value}局'
                }
            },
            yAxis: {
                show: true,
                type: 'value',
                axisLabel: {
                    fontSize: 9,
                    formatter: '{value}人'
                },
                axisLine: {
                    show: false
                },
                axisTick: {
                    show: false
                }
            },
            tooltip: {
                formatter: '{b}局：{c}人',
                textStyle: {
                    fontSize: 10
                }
            },
            series: [{
                type: 'bar',
                barWidth: 40,
                label: {
                    show: true,
                    fontSize: 9,
                    formatter: '{c}人'
                },
                data: $this.data('series'),
            }]
        };

        fromPositionChart.setOption(fromPositionChartOption);
    });

    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) {
            refresh();
        }
    });
    
    function refresh() {
        let $main = $('main');
        let updateAt = $main.data('updateat');
        let isUpdate = (new Date().getTime() - updateAt) > (1000 * 60 * 5);
        if($main.data('available') && isUpdate) {
            location.href = '/homework/refreshReport?id=' + $main.data('id')
        }
    }
    refresh();

    function setPuzzleChartsSelect(index) {
        let series = puzzleAllChartOption.series.map(function(serie) {
            serie.data.forEach(function (d, i) {
                if(i === index) {
                    d.symbolSize = 10;
                } else {
                    d.symbolSize = 4;
                }
            });
            return serie;
        });
        puzzleAllChartOption.series = series;
        puzzleAllChart.setOption(puzzleAllChartOption);
    }

    setPuzzleChartsSelect(0);
});