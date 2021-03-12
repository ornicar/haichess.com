$(function () {
    chessMove();

    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) {
            location.href = '/homework/show?id=' +  $('main').data('id')
        }
    });
});

function chessMove() {
    $('.homework-show').find('.moves move span:not(.disabled)').click(function() {
        let fen = $(this).data('fen');
        let $board = $(this).parents('tr').find('.td-board .mini-board');
        Chessground($board[0], {
            coordinates: false,
            resizable: false,
            drawable: { enabled: false, visible: false },
            viewOnly: true,
            fen: fen
        });
        $board.attr('data-fen', fen);

        $('.homework-show').find('.moves move span.active').removeClass('active');
        $(this).addClass('active');
    });
}
