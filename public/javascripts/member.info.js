$(function() {

    let $component = $('.component');
    $component.find('.tabs-horiz span').click(function () {
        let $this = $(this);
        $component.find('.tabs-horiz span').removeClass("active");
        $component.find('.tabs-content div').removeClass("active");

        let cls = $this.data('tab');
        $this.addClass('active');
        $component.find('.tabs-content div.' + cls).addClass('active');
    });
    setTabActive();

    function setTabActive() {
        let hash = location.hash;
        if(hash) {
            hash = hash.replace('#', '');
            $component.find('.tabs-horiz > span').removeClass('active');
            $component.find('.tabs-horiz > span[data-tab="' + hash + '"]').addClass('active');
            $component.find('.tabs-content > div').removeClass('active');
            $component.find('.tabs-content > div.' + hash).addClass('active');
        }
    }

    $('a.member-intro').click(function(e) {
        e.preventDefault();
        lichess.memberIntro();
        return false;
    });
});
