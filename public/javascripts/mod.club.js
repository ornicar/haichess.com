$(function() {

    $('.club-detail .button').click(function(e) {
        e.preventDefault();
        $('.club-detail').attr('action', $(this).attr('data-href'));
        $('.club-detail').submit();
        return false;
    });

});
