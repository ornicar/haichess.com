$(function () {
    $('.banner').unslider({
        speed: 500,
        delay: 5000,
        keys: false,
        dots: true,
        arrows: true,
        fluid: false
    });


    $('a.member-accept').click(function(e) {
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
                alert(res.responseText);
            }
        });
        return false;
    });
});