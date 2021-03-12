$(function() {

    let $page = $('.detail');
    $page.find('.header > div').click(function() {
        $page.find('.header > div').removeClass('active');
        $page.find('.panels div').removeClass('active');

        $(this).addClass('active');
        let activeTab = $(this).data('tab');
        $page.find('.panels').find('.' + activeTab).addClass('active');
    });

});