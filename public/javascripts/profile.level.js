$(function() {
    $('.account input[type=\'file\']').ajaxSingleUpload({
        'action': '/account/head'
    });

    $('a.level').click(function(e) {
        e.preventDefault();
        $.ajax({
            url: $(this).attr('href'),
            success: function(html) {
                $.modal($(html));
                $('.cancel').click(function () {
                    $.modal.close();
                });

                $('.flatpickr').flatpickr();
            },
            error: function(res) {
                if (res.status == 400) alert(res.responseText);
            }
        });
        return false;
    });
});
