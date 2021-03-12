$(function() {
    
    $('.clazz').find('a.stop').click(function(e) {
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
                if (res.status == 400) alert(res.responseText);
            }
        });
        return false;
    });

});