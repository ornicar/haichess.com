(function ($) {
    $.fn.drawer = function (options) {
        var settings = $.extend({
            side: 'left',
            width: ''
        }, options);

        var drawer = this;
        var $cw = drawer.find('.drawer-content-wrapper');
        var $mk = drawer.find('.drawer-mask');
        var $ft = drawer.find('.drawer-footer');
        var $tg = drawer.find('.drawer-toggle');
        var width = settings.width == '' ? drawerWidth() : settings.width;
        var translate = 'translateX(' + (settings.side === 'right' ? 100 : 100 * -1) + '%)';
        var rside = reverseSide();

        drawer.isOpen = false;
        drawer.addClass('drawer-' + settings.side);
        $ft.css('display', 'none');
        $cw.css({
            'width': width,
            'transform': translate
        });
        $tg.css(rside, "-40px");
        $tg.css('border-' + rside, $tg.css('border-bottom'));
        drawer.css('display', '');

        function toggleMenu(e) {
            e.preventDefault();
            e.stopPropagation();

            if (drawer.isOpen) {
                drawer.close();
            } else {
                drawer.open();
            }
            return false;
        }

        function reverseSide() {
            if (settings.side == 'left') {
                return 'right';
            } else {
                return 'left';
            }
        }

        function drawerWidth() {
            const windowWidth = $(window).width();
            if (windowWidth >= 1440) {
                return '25%';
            }
            if (windowWidth >= 1260) {
                return '30%';
            }
            if (windowWidth >= 1120 && windowWidth < 1260) {
                return '35%';
            }
            if (windowWidth >= 980 && windowWidth < 1260) {
                return '40%';
            }
            if (windowWidth >= 800 && windowWidth < 980) {
                return '45%';
            }
            if (windowWidth >= 500 && windowWidth < 980) {
                return '60%';
            }
            if (windowWidth < 500) {
                return '80%';
            }
        }

        drawer.open = function () {
            $cw.css('transform', '');
            drawer.addClass('drawer-open');
            $ft.css('display', '');
            drawer.isOpen = true;

            $mk.on('click touchend', toggleMenu);
        };

        drawer.close = function () {
            drawer.isOpen = false;

            $ft.css('display', 'none');
            $cw.css('transform', translate);
            drawer.removeClass('drawer-open');
            $mk.off('click touchend', toggleMenu);
        };

        $('[drawer-hide=true]').on('click touchend', toggleMenu);
        return drawer;
    };
})(jQuery);
