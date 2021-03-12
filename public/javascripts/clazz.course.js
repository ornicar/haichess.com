$(function() {

    $('.course').not('.stopped').click(function () {
        let $this = $(this);
        $('.course').not($this).removeClass('active');
        if ($this.hasClass('active')) {
            $this.removeClass('active');
        } else {
            $this.addClass('active');
        }
        handleActionButton();
    });

    $('.course').dblclick(function () {
        let id = $(this).data('id');
        let clazz = $(this).data('clazz');
        let index = $(this).data('index');
        let homework = $(this).data('homework');
        if(homework) {
            location.href = `/homework/report?id=${clazz}@${id}@${index}`;
        } else {
            location.href = `/homework/create?clazzId=${clazz}&courseId=${id}`;
        }
    });

    $('button[class*=\'action\']').click(function(e) {
        e.preventDefault();
        let id = $('.course.active').data('id');
        $.ajax({
            url: $(this).attr('href').replace('#id#', id),
            success: function(html) {
                $.modal($(html));
                $('.flatpickr').flatpickr({disableMobile: 'true'});
                $('.cancel').click(function () {
                    $.modal.close();
                });

                courseUpdate();
            },
            error: function(res) {
                if (res.status === 400) alert(res.responseText);
            }
        });
        return false;
    });

});

function courseUpdate() {
    $('.course-update').find('form').submit(function(e) {
        e.preventDefault();
        let $form = $(this);
        $.ajax({
            method: 'POST',
            url: $(this).attr('action'),
            data: {
                'date': $form.find('#form3-date').val(),
                'timeBegin': $form.find('#form3-timeBegin').val(),
                'timeEnd': $form.find('#form3-timeEnd').val()
            },
            success: function() {
                location.reload();
            },
            error: function(res) {
                alert(res.responseText);
            }
        });
        return false;
    });
}

function handleActionButton() {
    let $active = $('.course.active');
    let editable = $active.hasClass('editable');

    if(editable) {
        let clazzType = $active.data('type');
        $active.addClass('disabled').prop('disabled', true);
        if ($('.course.active').length === 1) {
            if (clazzType === 'train') {
                $('.action .button').not('.action-postpone').removeClass('disabled').prop('disabled', false);
            } else {
                $('.action .button').removeClass('disabled').prop('disabled', false);
            }
        } else {
            $('.action .button').addClass('disabled').prop('disabled', true);
        }
    } else {
        $('.action .button').addClass('disabled').prop('disabled', true);
    }


}