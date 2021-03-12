$(function() {

    $('a.modal-alert').click(function(e) {
        e.preventDefault();
        $.ajax({
            url: $(this).attr('href'),
            success: function(html) {
                $.modal($(html));
                $('.cancel').click(function () {
                    $.modal.close();
                });

                let $form = $('.givingForm');
                $form.find('input[name=username]').blur(function() {
                    validUsername($form);
                });
            },
            error: function(res) {
                handleError(res);
            }
        });
        return false;
    });

    function validUsername($form) {
        $form.find(`div.formError`).text('');
        $form.find('.giving')
            .addClass('disabled')
            .prop('disabled', true);

        $.ajax({
            url: '/member/card/giving/validUsername',
            type: 'post',
            data: {
                "username": $form.find('input[name=username]').val(),
            },
            success: function(res) {
                $form.find('.giving')
                    .removeClass('disabled')
                    .prop('disabled', false);
            },
            error: function(err) {
                handleError(err);
            }
        });
    }

    function handleError(res) {
        let json = res.responseJSON;
        if (json) {
            if (json.error) {
                if(typeof json.error === 'string') {
                    alert(json.error);
                } else if(res.status === 400) {
                    for(let key in json.error) {
                        $(`.${key}Error`).text(json.error[key][0]);
                    }
                } else alert(JSON.stringify(json.error));
            } else alert(res.responseText);
        } else alert('发生错误');
    }
});
