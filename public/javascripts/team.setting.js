$(function() {
    $('a.tag-add').click(function(e) {
        e.preventDefault();
        $.ajax({
            url: $(this).attr('href'),
            success: function(html) {
                $.modal($(html));
                $('.cancel').click(function () {
                    $.modal.close();
                });

                let $form = $('.tag');
                $form.find('#form3-typ').change(function () {
                    let v = $(this).val();
                    if(v === 'single_choice' || v === 'multiple_choice') {
                        $('#form3-value').prop('required', true).parent().removeClass('none')
                    } else {
                        $('#form3-value').prop('required', false).parent().addClass('none')
                    }
                });
            },
            error: function(res) {
                alert(res.responseText);
            }
        });
        return false;
    });

    $('a.tag-edit').click(function(e) {
        e.preventDefault();
        $.ajax({
            url: $(this).attr('href'),
            success: function(html) {
                $.modal($(html));
                $('.cancel').click(function () {
                    $.modal.close();
                });
            },
            error: function(res) {
                alert(res.responseText);
            }
        });
        return false;
    });

});