$(function() {

    $('a.member-rating').click(function(e) {
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