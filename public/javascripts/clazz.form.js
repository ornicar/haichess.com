$(function() {
    applyControl();
    //registerListener();
    $('.flatpickr').flatpickr({disableMobile: 'true'});

    $('#form3-clazzType').change(function () {
        let v = $(this).val();
        if (v === 'week') {
            $('.week-clazz').removeClass('none');
            $('.train-clazz').addClass('none');
        }
        if (v === 'train') {
            $('.train-clazz').removeClass('none');
            $('.week-clazz').addClass('none');
        }
    }).trigger('change');
});

function applyInputName() {
    let clazzType = $('#form3-clazzType').val();
    if (clazzType === 'week') {
        applyWeekInputName();
    } else {
        applyTrainInputName();
    }
}

function applyWeekInputName() {
    let $cl = $('.week-clazz .course-list .course');
    $cl.each(function (i, e) {
        $(this).find('select[name$=\'week\']').attr('name', buildWeekName(i, 'week'));
        $(this).find('input[name$=\'timeBegin\']').attr('name', buildWeekName(i, 'timeBegin'));
        $(this).find('input[name$=\'timeEnd\']').attr('name', buildWeekName(i, 'timeEnd'));
    })
}

function buildWeekName(index, name) {
    return 'weekClazz.weekCourse[' + index + '].' + name;
}

function applyTrainInputName() {
    let $cl = $('.train-clazz .course-list .course');
    $cl.each(function (i, e) {
        $(this).find('input[name$=\'dateStart\']').attr('name', buildTrainName(i, 'dateStart'));
        $(this).find('input[name$=\'dateEnd\']').attr('name', buildTrainName(i, 'dateEnd'));
        $(this).find('input[name$=\'timeBegin\']').attr('name', buildTrainName(i, 'timeBegin'));
        $(this).find('input[name$=\'timeEnd\']').attr('name', buildTrainName(i, 'timeEnd'));
    })
}

function buildTrainName(index, name) {
    return 'trainClazz.trainCourse[' + index + '].' + name;
}

function applyControl() {
    let clazzType = $('#form3-clazzType').val();
    let $cl = $('.' + clazzType + '-clazz .course-list .course');
    let len = $cl.length;
    let rm = '<a class=\'rm\' title="移除">-</a>';
    let ad = '<a class=\'ad\' title="添加">+</a>';

    $cl.each(function (i, e) {
        let $ct = $(this).find('.control');
        $ct.empty();
        if (len == 1) {
            $ct.append(ad);
        } else {
            if (i == len - 1) {
                $ct.append(rm);
                if ($cl.length < 7) {
                    $ct.append(ad);
                }
            } else {
                $ct.append(rm);
            }
        }
    });
    registerListener()
}

function registerListener() {
    $('a.ad').click(function () {
        let $el = $(this).parents('.course').clone();
        $(this).parents('.course-list').append($el);
        applyControl();
        applyInputName();
        $el.find('.flatpickr').flatpickr({disableMobile: 'true'});
    });

    $('a.rm').click(function () {
        $(this).parents('.course').remove();
        applyControl();
        applyInputName();
    });
}
