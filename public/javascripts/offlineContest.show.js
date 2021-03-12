$(function () {

    $('head').append('<style media="print">@page{size:auto; margin:50px 100px;}</style>');
    $('.contest-show').find('.head-line').addClass('no-print');

    let $flow = $('.flow');
    $flow.find('.tabs > div').not('.disabled').click(function () {
        let active = $(this).data('tab');
        $flow.find('.tabs > div').removeClass('active');
        $flow.find('.panels .panel').removeClass('active');
        $flow.find('.panels .panel.' + active).addClass('active');
        $(this).addClass('active');
        location.hash = active;
    });

    setTabActive();
    function setTabActive() {
        let hash = location.hash;
        if(!$flow.find('.tabs > div[data-tab="' + hash + '"]').hasClass('disabled')) {
            if(hash) {
                hash = hash.replace('#', '');
                $flow.find('.tabs > div').removeClass('active');
                $flow.find('.tabs > div[data-tab="' + hash + '"]').addClass('active');
                $flow.find('.panels .panel').removeClass('active');
                $flow.find('.panels .panel.' + hash).addClass('active');
            }
        }
    }

    $('a.modal-alert').click(function(e) {
        e.preventDefault();
        $.ajax({
            url: $(this).attr('href'),
            success: function(html) {
                $.modal($(html));
                $('.cancel').click(function () {
                    $.modal.close();
                });

                playerChooseModal();
                playerExternalModal();
                playerForbiddenModal();
                manualPairingModal();
                manualAbsentModal();
            },
            error: function(res) {
                handleError(res);
            }
        });
        return false;
    });

    function playerChooseModal() {
        let $modal = $('.player-choose');
        let $transfer = $modal.find('.transfer');
        let $leftPanel = $transfer.find('.left');
        let $rightPanel = $transfer.find('.right');
        let $leftList = $leftPanel.find('.transfer-panel-list table tbody');
        let $rightList = $rightPanel.find('.transfer-panel-list table tbody');
        let $leftButton = $transfer.find('.arrow-left');
        let $rightButton = $transfer.find('.arrow-right');

        function check($lst, $btn) {
            $lst.find('input[type="checkbox"]').click(function () {
                setBtn($lst, $btn);
            });
        }

        function setBtn($lst, $btn) {
            if ($lst.find('input:checked').length > 0) {
                $btn.removeClass('disabled').prop('disabled', false);
            } else {
                $btn.addClass("disabled").prop('disabled', true);
            }
        }

        function tsf($lst1, $lst2, $btn1, $btn2) {
            $btn1.click(function (e) {
                e.preventDefault();
                $lst1.find('input:checked').each(function () {
                    let $this = $(this);
                    let $line = $this.parents('tr');
                    if(!$line.hasClass('none')) {
                        $this.prop('checked', false);
                        $line.clone().appendTo($lst2);
                        $line.remove()
                    }
                });

                setBtn($lst1, $btn1);
                $lst2.find('input[type="checkbox"]').off('click');
                check($lst2, $btn2);

                $leftPanel.find('.transfer-panel-head').text('备选('+ $leftList.find('tr').length +')');
                $rightPanel.find('.transfer-panel-head').text('已选('+ $rightList.find('tr').length +')');

                let players = [];
                $rightList.find('input').each(function () {
                    players.push($(this).val());
                });
                $modal.find('input[name=players]').val(players.join(','));
                return false;
            });
        }

        function search() {
            $modal.find('.search').click(function() {
                let clazz = $('#member_clazz').val();
                let minScore = $('#member_minScore').val();
                let maxScore = $('#member_maxScore').val();
                let level = $('#member_level').val();
                let sex = $('#member_sex').val();

                let arr = [];
                $leftList.find('input').each(function () {
                    arr.push($(this).data('attr'));
                });

                let filterIds = arr.filter(function (n) {
                    return (!clazz || (n.clazz && n.clazz.includes(clazz))) /*&& (!minScore || n.score >= minScore) && (!maxScore || n.score <= maxScore)*/ && (!level || n.level === level) && (!sex || n.sex === sex)
                }).map(n => n.id);

                $leftList.find('tr').addClass('none');
                filterIds.forEach(function (id) {
                    $leftList.find('#chk_' + id).parents('tr').removeClass('none');
                });
            });
        }

        check($leftList, $rightButton);
        check($rightList, $leftButton);
        tsf($leftList, $rightList, $rightButton, $leftButton);
        tsf($rightList, $leftList, $leftButton, $rightButton);
        search();
    }

    function playerExternalModal() {
        let $form = $('.player-external').find('form')
        $form.submit(function(e) {
            e.preventDefault();
            $.ajax({
                method: 'POST',
                url: $(this).attr('action'),
                data: $form.serialize(),
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    handleError(res);
                    location.reload();
                }
            });
            return false;
        });
    }

    function manualAbsentModal() {
        let $modal = $('.contest-absent');
        let $transfer = $modal.find('.transfer');
        let $leftPanel = $transfer.find('.left');
        let $rightPanel = $transfer.find('.right');
        let $leftList = $leftPanel.find('.transfer-panel-list table tbody');
        let $rightList = $rightPanel.find('.transfer-panel-list table tbody');
        let $leftButton = $transfer.find('.arrow-left');
        let $rightButton = $transfer.find('.arrow-right');

        function check($lst, $btn) {
            $lst.find('input[type="checkbox"]').click(function () {
                setBtn($lst, $btn);
            });
        }

        function setBtn($lst, $btn) {
            if ($lst.find('input:checked').length > 0) {
                $btn.removeClass('disabled').prop('disabled', false);
            } else {
                $btn.addClass("disabled").prop('disabled', true);
            }
        }

        function tsf($lst1, $lst2, $btn1, $btn2) {
            $btn1.click(function (e) {
                e.preventDefault();
                $lst1.find('input:checked').each(function () {
                    let $this = $(this);
                    let $line = $this.parents('tr');
                    if(!$line.hasClass('none')) {
                        $this.prop('checked', false);
                        $line.clone().appendTo($lst2);
                        $line.remove()
                    }
                });

                setBtn($lst1, $btn1);
                $lst2.find('input[type="checkbox"]').off('click');
                check($lst2, $btn2);

                let joins = [];
                $leftList.find('input').each(function () {
                    joins.push($(this).val());
                });
                $modal.find('input[name=joins]').val(joins.join(','));

                let absents = [];
                $rightList.find('input').each(function () {
                    absents.push($(this).val());
                });
                $modal.find('input[name=absents]').val(absents.join(','));
                return false;
            });
        }

        function search($panel, $lst) {
            $panel.find('.transfer-search').on('input propertychange', function() {
                let txt = $(this).val();
                if($.trim(txt) !== ''){
                    let arr = [];
                    $lst.find('input').each(function () {
                        arr.push($(this).data('attr'));
                    });

                    let filterIds = arr.filter(function (n) {
                        return n.id.startsWith(txt) || n.name.startsWith(txt)
                    }).map(n => n.id);

                    $lst.find('tr').addClass('none');
                    filterIds.forEach(function (id) {
                        $lst.find('#chk_' + id).parents('tr').removeClass('none');
                    });
                } else {
                    $lst.find('tr').removeClass('none');
                }
            });
        }

        check($leftList, $rightButton);
        check($rightList, $leftButton);
        tsf($leftList, $rightList, $rightButton, $leftButton);
        tsf($rightList, $leftList, $leftButton, $rightButton);
        search($leftPanel, $leftList);
        search($rightPanel, $rightList);
    }

    function playerForbiddenModal() {
        let $modal = $('.contest-forbidden');
        let $transfer = $modal.find('.transfer');
        let $leftPanel = $transfer.find('.left');
        let $rightPanel = $transfer.find('.right');
        let $leftList = $leftPanel.find('.transfer-panel-list table tbody');
        let $rightList = $rightPanel.find('.transfer-panel-list table tbody');
        let $leftButton = $transfer.find('.arrow-left');
        let $rightButton = $transfer.find('.arrow-right');

        function check($lst, $btn) {
            $lst.find('input[type="checkbox"]').click(function () {
                setBtn($lst, $btn);
            });
        }

        function setBtn($lst, $btn) {
            if ($lst.find('input:checked').length > 0) {
                $btn.removeClass('disabled').prop('disabled', false);
            } else {
                $btn.addClass("disabled").prop('disabled', true);
            }
        }

        function tsf($lst1, $lst2, $btn1, $btn2) {
            $btn1.click(function (e) {
                e.preventDefault();
                $lst1.find('input:checked').each(function () {
                    let $this = $(this);
                    let $line = $this.parents('tr');
                    if(!$line.hasClass('none')) {
                        $this.prop('checked', false);
                        $line.clone().appendTo($lst2);
                        $line.remove()
                    }
                });

                setBtn($lst1, $btn1);
                $lst2.find('input[type="checkbox"]').off('click');
                check($lst2, $btn2);

                let playerIds = [];
                $rightList.find('input').each(function () {
                    playerIds.push($(this).val());
                });
                $modal.find('input[name=playerIds]').val(playerIds.join(','));
                return false;
            });
        }

        function search($panel, $lst) {
            $panel.find('.transfer-search').on('input propertychange', function() {
                let txt = $(this).val();
                if($.trim(txt) !== ''){
                    let arr = [];
                    $lst.find('input').each(function () {
                        arr.push($(this).data('attr'));
                    });

                    let filterIds = arr.filter(function (n) {
                        return n.id.startsWith(txt) || n.name.startsWith(txt)
                    }).map(n => n.id);

                    $lst.find('tr').addClass('none');
                    filterIds.forEach(function (id) {
                        $lst.find('#chk_' + id).parents('tr').removeClass('none');
                    });
                } else {
                    $lst.find('tr').removeClass('none');
                }
            });
        }

        check($leftList, $rightButton);
        check($rightList, $leftButton);
        tsf($leftList, $rightList, $rightButton, $leftButton);
        tsf($rightList, $leftList, $leftButton, $rightButton);
        search($leftPanel, $leftList);
        search($rightPanel, $rightList);

        let $form = $modal.find('form');
        $form.submit(function(e) {
            e.preventDefault();
            let players = $modal.find('input[name=playerIds]').val();
            if(!players) {
                alert('请选择回避棋手');
                return false;
            }
            $.ajax({
                method: 'POST',
                url: $(this).attr('action'),
                data: $form.serialize(),
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    handleError(res);
                }
            });
            return false;
        });
    }

    function manualPairingModal() {
        let $mp = $('.manual-pairing');
        let $lst = $mp.find('.manual-list .slist');
        $mp.find('.manual-filter-search').on('input propertychange', function() {
            let txt = $(this).val();
            if($.trim(txt) !== ''){
                $lst.find('tbody tr').not('tr:contains("' + txt + '")').css('display', 'none');
                $lst.find('tbody tr').filter('tr:contains("' + txt + '")').css('display', 'table-row');
            } else {
                $lst.find('tbody tr').css('display', 'table-row');
            }
        });

        $mp.find('.form3').submit(function(e) {
            e.preventDefault();
            let t = $lst.find('input:checked').val();
            if(!t) {
                alert('请选择交换棋手');
                return false;
            }

            let source = JSON.parse($mp.find('input[name="source"]').val());
            let target = JSON.parse(t);
            let url = $(this).attr('action');
            $.ajax({
                method: 'POST',
                url: url,
                data: { 'source.isBye': source.isBye, 'source.board': source.board, 'source.color': source.color, 'source.player': source.player,
                    'target.isBye': target.isBye, 'target.board': target.board, 'target.color': target.color, 'target.player': target.player },
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    handleError(res);
                }
            });
            return false;
        });
    }

    $('.pairing').parent('form').submit(function(e) {
        e.preventDefault();
        if (confirm('确认生成对战表？')) {
            $.ajax({
                method: 'POST',
                url: $(this).attr('action'),
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    handleError(res);
                    location.reload();
                }
            });
        }
        return false;
    });

    $('.publish-result').parent('form').submit(function(e) {
        e.preventDefault();
        if (confirm('确认发布成绩？')) {
            $.ajax({
                method: 'POST',
                url: $(this).attr('action'),
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    handleError(res);
                    location.reload();
                }
            });
        }
        return false;
    });
    
    $('.print-players').click(function () {
        let $printArea = $(this).parents('.enter').find('.print-area');
        $printArea.find('.printTitle').removeClass('none');
        $printArea.find('.slist').addClass('print-list');
        setTimeout(function() {
            $printArea.find('.printTitle').addClass('none');
            $printArea.find('.slist').removeClass('print-list');
        }, 1000);
        $printArea.print();
    });

    $('.print-board-sheet').click(function () {
        let $printArea = $('.print-area.round' + $(this).data('id'));
        $printArea.find('.printTitle .cName .subName').text('对战表');
        $printArea.find('.printTitle').removeClass('none');
        $printArea.find('.slist').addClass('print-list');
        $printArea.find('.slist .result b').addClass('no-print');
        setTimeout(function() {
            $printArea.find('.printTitle').addClass('none');
            $printArea.find('.slist').removeClass('print-list');
            $printArea.find('.slist .result b').removeClass('no-print');
        }, 1000);
        $printArea.print();
    });

    $('.print-round-score').click(function () {
        let $printArea = $('.print-area.round' + $(this).data('id'));
        $printArea.find('.printTitle .cName .subName').text('轮次成绩');
        $printArea.find('.printTitle').removeClass('none');
        $printArea.find('.slist').addClass('print-list');
        setTimeout(function() {
            $printArea.find('.printTitle').addClass('none');
            $printArea.find('.slist').removeClass('print-list');
        }, 1000);
        $printArea.print();
    });

    $('.print-score').click(function () {
        let $printArea = $(this).parents('.score').find('.print-area');
        $printArea.find('.printTitle').removeClass('none');
        $printArea.find('.slist').addClass('print-list');
        setTimeout(function() {
            $printArea.find('.printTitle').addClass('none');
            $printArea.find('.slist').removeClass('print-list');
        }, 1000);
        $printArea.print();
    });

    let $enterTable = $('.contest-show .panels .enter .slist').not('.unsortable');
    let $waiting = $('.contest-show .waiting');
    $enterTable.tableDnD({
        onDragClass: 'dragClass',
        onDrop: function(t, r) {
            let playerIds = [];
            $enterTable.find('tbody tr').each(function() {
                playerIds.push($(this).data('id'));
            });
            let contestId = $('main').data('id');

            $waiting.removeClass('none');
            $.ajax({
                method: 'POST',
                url: `/offContest/${contestId}/reorderPlayer`,
                data: {'playerIds': playerIds.join(',')},
                success: function() {
                    setTimeout(function () {
                        $enterTable.find('tbody tr').each(function(i) {
                            $(this).find('td').first().text(i + 1);
                        });
                        $waiting.addClass('none');
                    }, 500);
                },
                error: function(res) {
                    handleError(res);
                }
            });
        }
    });

    $(window).scrollTop($.cookie('haichess-offContest-scrollTop'));
    $(window).scroll(function() {
        $.cookie('haichess-offContest-scrollTop', document.documentElement.scrollTop);
    });
});

function handleError(res) {
    let json = res.responseJSON;
    if (json) {
        if (json.error) {
            if(typeof json.error === 'string') {
                alert(json.error);
            } else alert(JSON.stringify(json.error));
        } else alert(res.responseText);
    } else alert('发生错误');
}
