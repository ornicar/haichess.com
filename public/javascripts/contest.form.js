$(function () {

    let $form = $('.contest__form');

    $form.find('.single-uploader').find('input[type=\'file\']').ajaxSingleUpload({
        'name': 'logo',
        'action': '/contest/imageUpload'
    });

    $form.find('.single-file').find('input[type=\'file\']').ajaxSingleFileUpload({
        'name': 'attachments',
        'action': '/contest/fileUpload'
    });

    $form.find('#form3-basics_typ').change(function () {
        let o = $form.find('#form3-basics_organizer');
        let v = $(this).val();
        let d = $form.data(v);
        let teamId = getQueryVariable("team");
        let selectedId = $('input[name=organizerSelected]').val();
        selectedId = selectedId ? selectedId : teamId;

        o.empty();
        for (let i = 0; i < d.length; i++) {
            let selected = selectedId && selectedId == d[i].id;
            o.append("<option value = '" + d[i].id + "' " + (selected ? 'selected' : '') + " >" + d[i].name + "</option>");
        }
        o.trigger('change');
    });

    $form.find('#form3-basics_organizer').change(function () {
        let tpy = $form.find('#form3-basics_typ').val();
        let val = $(this).val();
        let dataArr = $form.data(tpy);
        let teamRatedArr = dataArr.filter(n => n.id == val && n.teamRated);
        if(teamRatedArr.length > 0) {
            $('#form3-basics_teamRated').parents('.form-check').removeClass('none');
        } else {
            $('#form3-basics_teamRated').parents('.form-check').addClass('none');
        }

        if(tpy == 'public') {
            $('#form3-conditions_all_teamMember_teamId').val('');
            $('#form3-conditions_all_clazzMember_clazzId').val('')
        }

        if(tpy == 'clazz-inner') {
            $('#form3-conditions_all_teamMember_teamId').val('');
            $('#form3-conditions_all_clazzMember_clazzId').val(val)
        }

        if(tpy == 'team-inner') {
            $('#form3-conditions_all_teamMember_teamId').val(val);
            $('#form3-conditions_all_clazzMember_clazzId').val('')
        }
    });
    $form.find('#form3-basics_typ').trigger('change');

    function getQueryVariable(variable) {
        let query = window.location.search.substring(1);
        let vars = query.split("&");
        for (let i = 0; i < vars.length; i++) {
            let pair = vars[i].split("=");
            if (pair[0] == variable) {
                return pair[1];
            }
        }
        return false;
    }

    $form.find('#form3-rounds_appt').change(function () {
        let val = $(this).val();
        if(val == 1) {
            $form.find('#form3-rounds_apptDeadline').parents('.form-group').removeClass('none');
        } else $form.find('#form3-rounds_apptDeadline').parents('.form-group').addClass('none');
    });
    $form.find('#form3-rounds_appt').trigger('change');

    $form.find('.tabs > div').click(function () {
        $form.find('.tabs > div').removeClass('active');
        $form.find('.panel').removeClass('active');
        $form.find('.panel.' + $(this).data('tab')).addClass('active');
        $(this).addClass('active');
    });
    $form.find('.flatpickr').flatpickr(datePickOption);

    // 根据 比赛开始时间、比赛轮次、间隔时间 生成轮次
    $('.button-generate').click(function() {
        let $rounds = $form.find('.round-generate');
        let number = $form.find('#form3-rounds_rounds').val();

        if(number > 16) {
            alert('轮次数量不大于16轮');
            return;
        }
        if($rounds.find('div').length > 0) {
            if (!window.confirm('该操作将重置已有配置，是否继续？')) return;
        }

        let startsAt = $form.find('#form3-basics_startsAt').val();
        let spaceDay = $form.find('#form3-rounds_spaceDay').val();
        let spaceHour = $form.find('#form3-rounds_spaceHour').val();
        let spaceMinute = $form.find('#form3-rounds_spaceMinute').val();


        let spaceMi = spaceDay * 24 * 60 * 60 * 1000 + spaceHour * 60 * 60 * 1000 + spaceMinute* 60 * 1000;
        let startsAtMi = new Date(startsAt).getTime();

        $rounds.empty();
        for (let i = 0; i < number; i++) {
            let sd = startsAtMi + (spaceMi * i);
            let el = buildElement(i, new Date(sd).format("yyyy-MM-dd hh:mm"));
            $rounds.append(el);
        }
        $rounds.find('.flatpickr').flatpickr(datePickOption);
    });

    function buildElement(index, sd) {
        return `<div class="form-split">
                    <div class="form-group form-half">
                        <label class="form-label" for="form3-rounds_list_${index}_startsAt">第 ${index + 1} 轮</label>
                        <input id="form3-rounds_list_${index}_startsAt" 
                               name="rounds.list[${index}].startsAt"
                               class="form-control flatpickr flatpickr-input" 
                               value="${sd}"
                               data-enable-time="true" 
                               data-time_24h="true"
                               type="hidden">
                    </div>
                </div>`
    }

    function datePickOption() {
        return {
            minDate: new Date(Date.now() + 1000 * 60 * 3),
            maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 30 * 365),
            altFormat: 'Y-m-d H:i'
        }
    }

    Date.prototype.format = function (fmt) {
        let o = {
            "M+": this.getMonth() + 1,                 //月份
            "d+": this.getDate(),                    //日
            "h+": this.getHours(),                   //小时
            "m+": this.getMinutes(),                 //分
            "s+": this.getSeconds(),                 //秒
            "q+": Math.floor((this.getMonth() + 3) / 3), //季度
            "S": this.getMilliseconds()             //毫秒
        };
        if (/(y+)/.test(fmt)) {
            fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
        }
        for (var k in o) {
            if (new RegExp("(" + k + ")").test(fmt)) {
                fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
            }
        }
        return fmt;
    };

    $('#form3-basics_position').change(function() {
        let text = $(this).find("option:selected").text();
        if(text === '初始局面') {
            $('.board-link').addClass('none');
            $('.position-paste').addClass('none');
        } else if(text === '载入局面') {
            $('.board-link').removeClass('none');
            $('.position-paste').removeClass('none');
            $('.position-paste').val('');
            $('.starts-position').find('.preview').empty();
        } else {
            $('.board-link').removeClass('none');
            $('.position-paste').addClass('none');
            validateFen($(this).val());
        }
    });

    function validateFen(fen) {
        let $position = $('.starts-position');
        let $board = $position.find('.preview');
        if (fen) {
            $.ajax({
                url: '/setup/validate-fen?strict=0',
                data: {
                    fen: fen
                },
                success: function(data) {
                    $board.html(data);
                    $position.removeClass('is-invalid');
                    $('a.board-link').attr('href', $('a.board-link').attr('href').replace(/fen=.+$/, "fen=" + fen));
                    let $option = $('#form3-basics_position').find('option:selected');
                    if($option.data('id') === 'option-load-fen') {
                        $option.val(fen);
                    }
                    lichess.pubsub.emit('content_loaded');
                },
                error: function() {
                    $board.empty();
                    $position.addClass('is-invalid');
                }
            });
        }
    }

    $('.position-paste').on('input propertychange', function () {
        validateFen($(this).val())
    });

});


