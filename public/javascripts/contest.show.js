$(function () {

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

    $('.toggle-show').click(function () {
        $('.requests .processed').toggleClass('none');
    });

    $('a.modal-alert').click(function(e) {
        e.preventDefault();
        $.ajax({
            url: $(this).attr('href'),
            success: function(html) {
                $.modal($(html));
                $('.cancel').click(function () {
                    $.modal.close();
                });

                let $md = $('.modal-content');
                inviteModal($md);
                transferModal($md);
                playerForbiddenModal();
                manualPairingModal($md);
                apptSetModal($md);
            },
            error: function(res) {
                handleError(res);
            }
        });
        return false;
    });

    $('#autoPairing').change(function() {
        let checked = $(this).prop("checked");
        let cfm = checked ? '是否自动编排和发布成绩？' : '是否取消自动编排和发布成绩？';
        if (confirm(cfm)) {
            $.ajax({
                method: 'POST',
                url: $(this).parents('form').attr('action'),
                data: { 'autoPairing': checked ? '1' : '0' },
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    handleError(res);
                }
            });
        } else $(this).prop("checked", !checked);
    });

    $('.pairing').parent('form').submit(function(e) {
        e.preventDefault();
        if (confirm('是否确认生成对战表？')) {
            let id = $(this).data('contest-id');
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

    $('.publish-pairing').parent('form').submit(function(e) {
        e.preventDefault();
        if (confirm('是否确认发布对战表？')) {
            let id = $(this).data('contest-id');
            $.ajax({
                method: 'POST',
                url: $(this).attr('action'),
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    handleError(res);
                }
            });
        }
        return false;
    });

    function inviteModal($md) {
        $md.find('.user-autocomplete').each(function() {
            let opts = {
                focus: 1,
                friend: $(this).data('friend'),
                tag: $(this).data('tag')
            };
            if ($(this).attr('autofocus')) lichess.userAutocomplete($(this), opts);
            else $(this).one('focus', function() {
                lichess.userAutocomplete($(this), opts);
            });
        });

        $('.contest-invite .form3').submit(function(e) {
            e.preventDefault();
            let username = $md.find('input[name="username"]').val();
            let id = $md.find('input[name="contestId"]').val();
            let url = $(this).attr('action');
            $.ajax({
                method: 'POST',
                url: url,
                data: { "username": username },
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

    function transferModal($md) {
        transfer();

        $('.contest-absent .form3').submit(function(e) {
            e.preventDefault();
            let joins = [];
            let absents = [];
            $md.find('.transfer .left .transfer-panel-list input').each(function() {
                joins.push($(this).val());
            });
            $md.find('.transfer .right .transfer-panel-list input').each(function() {
                absents.push($(this).val());
            });
            let id = $md.find('input[name="contestId"]').val();
            let url = $(this).attr('action');
            $.ajax({
                method: 'POST',
                url: url,
                data: { 'joins': joins, 'absents': absents },
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

    function transfer() {
        let $transfer = $('.transfer');
        let $leftPanel = $transfer.find('.left');
        let $rightPanel = $transfer.find('.right');
        let $leftList = $leftPanel.find('.transfer-panel-list ul');
        let $rightList = $rightPanel.find('.transfer-panel-list ul');
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
                    let $line = $(this).parent();
                    $line.find('input[type="checkbox"]').prop('checked', false);
                    $line.clone().appendTo($lst2);
                    $line.remove()
                });

                setBtn($lst1, $btn1);
                $lst2.find('input[type="checkbox"]').off('click');
                check($lst2, $btn2);
                return false;
            });
        }

        function search($panel, $lst) {
            $panel.find('.transfer-search').on('input propertychange', function() {
                let txt = $(this).val();
                if($.trim(txt) != ''){
                    $lst.find('li').not('.transfer-panel-item:contains("' + txt + '")').hide();
                    $lst.find('li').filter('.transfer-panel-item:contains("' + txt + '")').show();
                } else {
                    $lst.find('li').show();
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

    function manualPairingModal($md) {
        let $mp = $('.manual-pairing');
        let $lst = $mp.find('.manual-list .slist');
        $mp.find('.manual-filter-search').on('input propertychange', function() {
            let txt = $(this).val();
            if($.trim(txt) != ''){
                $lst.find('tbody tr').not('tr:contains("' + txt + '")').css('display', 'none');
                $lst.find('tbody tr').filter('tr:contains("' + txt + '")').css('display', 'table-row');
            } else {
                $lst.find('tbody tr').css('display', 'table-row');
            }
        });

        $mp.find('.form3').submit(function(e) {
            e.preventDefault();
            let id = $mp.find('input[name="contestId"]').val();
            let source = JSON.parse($mp.find('input[name="source"]').val());
            let target = JSON.parse($lst.find('input:checked').val());
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

    function apptSetModal($md) {
        $md.find('.flatpickr').flatpickr();
        $('.contest-appt .form3').submit(function(e) {
            e.preventDefault();
            let url = $(this).attr('action');
            $.ajax({
                method: 'POST',
                url: url,
                data: { 'startsAt': $md.find('#apptStartsAt').val() },
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

    $('input[name="roundStartsTime"]').flatpickr({
        minuteIncrement: 1,
        altFormat: 'Y-m-d H:i',
        onClose: function (d1, d2) {
            if (confirm('是否更新轮次开始时间？')) {
                let contestId = $('input[name="roundStartsTime"]').data('contest-id');
                let roundNo = $('input[name="roundStartsTime"]').data('round-no');
                let startsAt = $('input[name="roundStartsTime"]').val();
                $.ajax({
                    method: 'POST',
                    url: '/contest/' + contestId + '/round/startsTime?rno=' + roundNo,
                    data: { 'startsAt': startsAt },
                    success: function() {
                        location.reload();
                    },
                    error: function(res) {
                        handleError(res);
                    }
                });
            }
        }
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
                url: `/contest/${contestId}/reorderPlayer`,
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

    $(window).scrollTop($.cookie('haichess-contest-scrollTop'));
    $(window).scroll(function() {
        $.cookie('haichess-contest-scrollTop', document.documentElement.scrollTop);
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
