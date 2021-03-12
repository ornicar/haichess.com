$(function () {

    let $page = $('.homework-form');
    registerItems();
    registerCapsules();
    registerReplayGame();
    registerRecallGame();
    registerFromPosition();

    $page.find('input').on('input propertychange', function () {
        $(this).removeClass('invalid');
    });

    $('a.modal-alert').click(function(e) {
        e.preventDefault();
        $.ajax({
            url: $(this).attr('href'),
            success: function(html) {
                $.modal($(html));
                $('.cancel').click(function () {
                    $.modal.close();
                });

                registerItems();
                registerCapsules();
                registerReplayGame();
                registerRecallGame();
            },
            error: function(res) {
                alert(res.responseText);
            }
        });
        return false;
    });

    function registerItems() {
        $('.modal-item .form3').submit(function (e) {
            e.preventDefault();

            let $form = $(this);
            let items = [];
            $form.find('.items input:checked').each(function () {
                let attr = $(this).data('attr');
                items.push(attr);
            });

            if(items.length > 4) {
                alert('最多选择4项');
                return false;
            }

            let itemsHtml = items.map(function (item, index) {
                let html = '<tbody><tr><td class="item-name"><label>' + item.name + '</label></td>';
                if (!item.isNumber) {
                    html += '<td class="bit"><strong>+</strong></td>';
                } else html += '<td></td>';
                html +=
                    '<td><input type="number" name="common[' + index + '].num"><input type="hidden" name="common[' + index + '].item" value="' + item.id + '"></td>' +
                    '<td><a class="remove">移除</a></td>' +
                    '</tr></tbody>';
                return html;
            });

            $page.find('.items table').empty();
            $page.find('.items table').html(itemsHtml);
            remove();
            resetName();
            setLink();
            $.modal.close();
            return false;
        });

        function remove() {
            $page.find('.items table').find('.remove').click(function () {
                $(this).parents('tr').remove();
                resetName();
                setLink()
            });
        }

        function resetName() {
            $page.find('.items table tr').each(function (i, e) {
                let $item = $(this);
                $item.find('input').each(function() {
                    let name = $(this).attr('name');
                    $(this).attr('name', name.replace(/common\[\d+\]/g, 'common[' + i + ']'));
                });
            });
        }

        function setLink() {
            let ids = [];
            $page.find('.items input[name$=item]').each(function() {
                ids.push($(this).val());
            });
            let href = $('.btn-item-add').attr('href');
            href = href.split('=')[0] + '=' + ids.join(',');
            $('.btn-item-add').attr('href', href);
        }

        remove();
    }

    function registerCapsules() {
        $('.modal-capsule .form3').submit(function (e) {
            e.preventDefault();
            let $form = $(this);
            let ids = [];
            $form.find('.capsule-list input:checked').each(function () {
                ids.push($(this).val());
            });

            if($page.find('.capsules .capsule').length + ids.length > 5) {
                alert('最多添加5项');
                return false;
            }

            $.get('/capsule/infos?ids=' + ids.join(','), function (response) {
                let html = response.map(function(capsule) {
                    let puzzles = capsule.puzzles;
                    let puzzlesHtml = '';
                    if(puzzles && puzzles.length > 0) {
                        puzzles.forEach(function(puzzle) {
                            puzzlesHtml += puzzleHtml(puzzle);
                        });
                    }
                    return capsuleHtml(capsule, puzzlesHtml);
                });

                $page.find('.capsules').append(html);
                lichess.pubsub.emit('content_loaded');

/*                setTimeout(function () {
                    $page.find('.capsules .mini-board').each(function () {
                        let $board = $(this);
                        Chessground(this, {
                            coordinates: false,
                            resizable: false,
                            drawable: { enabled: false, visible: false },
                            viewOnly: true,
                            fen: $board.data('fen'),
                            lastMove: $board.data('lastmove'),
                            orientation: $board.data('color')
                        });
                    });
                }, 500);*/

                remove();
                resetName();
                $.modal.close();
            });
            return false;
        });

        function remove() {
            $page.find('.capsules .remove').click(function () {
                $(this).parents('.capsule').remove();
                resetName();
            });
        }

        function resetName() {
            $page.find('.capsules .capsule').each(function (i, e) {
                let $capsule = $(this);

                $capsule.find('.capsule-head input').each(function() {
                    let name = $(this).attr('name');
                    $(this).attr('name', name.replace(/capsules\[\d+\]/g, 'capsules[' + i + ']'));
                });

                $capsule.find('.capsule-puzzles .puzzle').each(function(j, e) {
                    $(this).find('input').each(function() {
                        let name = $(this).attr('name');
                        $(this).attr('name', name.replace(/capsules\[\d+\]/g, 'capsules[' + i + ']').replace(/puzzles\[\d+\]/g, 'puzzles[' + j + ']'));
                    });
                });
            });
        }

        function capsuleHtml(capsule, puzzles) {
          return `<div class="capsule">
                   <div class="capsule-head">
                        <label>${capsule.name}</label>
                        <a class="remove">移除</a>
                        <input type="hidden" name="practice.capsules[0].id" value="${capsule.id}">
                        <input type="hidden" name="practice.capsules[0].name" value="${capsule.name}">
                    </div>
                    <div class="capsule-puzzles">${puzzles}</div> 
                </div>`;
        }

        function puzzleHtml(puzzle) {
            let lines = encodeURI(puzzle.lines);
            return `<div class="puzzle">
                        <span class="mini-board cg-wrap parse-fen is2d" data-color="${puzzle.color}" data-fen="${puzzle.fen}" data-lastmove="${puzzle.lastMove}"></span>
                        <input type="hidden" name="practice.capsules[0].puzzles[0].id" value="${puzzle.id}">
                        <input type="hidden" name="practice.capsules[0].puzzles[0].fen" value="${puzzle.fen}">
                        <input type="hidden" name="practice.capsules[0].puzzles[0].color" value="${puzzle.color}">
                        <input type="hidden" name="practice.capsules[0].puzzles[0].lastMove" value="${puzzle.lastMove}">
                        <input type="hidden" name="practice.capsules[0].puzzles[0].lines" value="${lines}">
                    </div>`;
        }

        remove();

        let $lst = $('.capsule-list');
        $('.modal-capsule .form3').find('.capsule-filter-search').on('input propertychange', function() {
            let txt = $(this).val();
            if($.trim(txt) != ''){
                $lst.find('tbody tr').not('tr:contains("' + txt + '")').css('display', 'none');
                $lst.find('tbody tr').filter('tr:contains("' + txt + '")').css('display', 'table-row');
            } else {
                $lst.find('tbody tr').css('display', 'table-row');
            }
        });
    }

    function registerReplayGame() {
        $('.modal-replay .form3').submit(function (e) {
            e.preventDefault();
            if($page.find('.replayGames table>tbody>tr').length >= 5) {
                alert('最多添加5项');
                return false;
            }

            let $form = $(this);
            let chapterLink = $form.find('#chapterLink').val();
            if(chapterLink) {
                $.get(chapterLink + '/info', function (response) {
                    let moves = moveTable(response);
                    let $html = $(replayHtml(response, moves, chapterLink));
                    $page.find('.replayGames table>tbody').append($html);
                    $html.find('.mini-board').each(function () {
                        let $board = $(this);
                        Chessground(this, {
                            coordinates: false,
                            resizable: false,
                            drawable: { enabled: false, visible: false },
                            viewOnly: true,
                            fen: $board.data('fen')
                        });
                    });
                    //lichess.pubsub.emit('content_loaded');

                    remove();
                    resetName();
                    chessMove();
                    $.modal.close();
                }).fail(function(err) {
                    if(err.responseJSON.error) {
                        alert(err.responseJSON.error);
                    } else alert('加载失败');
                });
            }
            return false;
        });

        function remove() {
            $page.find('.replayGames table>tbody .remove').click(function () {
                $(this).parents('tr').remove();
                resetName();
            });
        }

        function replayHtml(chapter, moves, chapterLink) {
            return `<tr>
                       <td class="td-board">
                            <span class="mini-board cg-wrap parse-fen is2d" data-fen="${chapter.root}"></span>
                       </td>
                       <td>
                            <a href="${chapterLink}"><label>${chapter.name}</label></a>
                            <div class="moves">${moves}</div>
                       </td>
                       <td>
                            <input type="hidden" name="practice.replayGames[0].chapterLink" value="${chapterLink}">
                            <input type="hidden" name="practice.replayGames[0].name" value="${chapter.name}">
                            <input type="hidden" name="practice.replayGames[0].root" value="${chapter.root}">
                            <a class="remove">移除</a>
                        </td>
                    </tr>`;
        }

        function resetName() {
            $page.find('.replayGames table>tbody>tr').each(function (i, e) {
                $(this).find('input').each(function() {
                    let name = $(this).attr('name');
                    $(this).attr('name', name.replace(/replayGames\[\d+\]/g, 'replayGames[' + i + ']'));
                });

                $(this).find('move').each(function (j, e) {
                    $(this).find('input').each(function() {
                        let name = $(this).attr('name');
                        $(this).attr('name', name.replace(/moves\[\d+\]/g, 'moves[' + j + ']'));
                    });
                });
            });
        }

        function moveTable(chapter) {
            let html = '';
            let moves = chapter.moves;
            if(moves && moves.length > 0) {
                moves.forEach(function(move) {
                    let white = move.white ? move.white.san : '...';
                    let black = move.black ? move.black.san : '';
                    let whiteFen = move.white ? move.white.fen : '';
                    let blackFen = move.black ? move.black.fen : '';
                    let whiteClass = move.white ? '' : 'disabled';
                    let blackClass = move.black ? '' : 'disabled';

                    let whiteHidden = move.white ?
                        `
                        <input type="hidden" name="practice.replayGames[0].moves[0].white.san" value="${move.white.san}">
                        <input type="hidden" name="practice.replayGames[0].moves[0].white.uci" value="${move.white.uci}">
                        <input type="hidden" name="practice.replayGames[0].moves[0].white.fen" value="${move.white.fen}">
                        ` : '';

                    let blackHidden = move.black ?
                        `
                        <input type="hidden" name="practice.replayGames[0].moves[0].black.san" value="${move.black.san}">
                        <input type="hidden" name="practice.replayGames[0].moves[0].black.uci" value="${move.black.uci}">
                        <input type="hidden" name="practice.replayGames[0].moves[0].black.fen" value="${move.black.fen}">
                        ` : '';

                    html += `<move>
                                <index>${move.index}. <input type="hidden" name="practice.replayGames[0].moves[0].index" value="${move.index}"></index>
                                <span class = "${whiteClass}" data-fen="${whiteFen}">${white}${whiteHidden}</span>
                                <span class = "${blackClass}" data-fen="${blackFen}">${black}${blackHidden}</span>
                            </move>`;
                });
            }
            return html;
        }

        function chessMove() {
            $page.find('.moves move span:not(.disabled)').click(function() {
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

                $page.find('.moves move span.active').removeClass('active');
                $(this).addClass('active');
            });
        }

        remove();
        chessMove();
    }

    function registerFromPosition() {
        let register = function() {
            $page.find('.fromPositions .tb>tbody').find('.mini-board').each(function () {
                let $board = $(this);
                Chessground(this, {
                    coordinates: false,
                    resizable: false,
                    drawable: { enabled: false, visible: false },
                    viewOnly: true,
                    fen: $board.data('fen')
                });
            });
            //lichess.pubsub.emit('content_loaded');

            $page.find('.fromPositions .tb>tbody').find('input[name$=fen]').on('input propertychange', function () {
                let fen = $(this).val();
                let $board = $(this).parents('.form').parents('tr').find('td:first');
                validateFen(fen, $board);
            });
            remove();
        };

        let remove = function() {
            $page.find('.fromPositions .tb>tbody').find('.remove').click(function () {
                $(this).parents('tr').remove();
                resetName();
            });
        };

        let resetName = function() {
            $page.find('.fromPositions .tb>tbody>tr').each(function (i, e) {
                $(this).find('input,select').each(function() {
                    let name = $(this).attr('name');
                    $(this).attr('name', name.replace(/fromPositions\[\d+\]/g, 'fromPositions[' + i + ']'));
                });
            });
        };

        let validateFen = function(fen, $board) {
            if (fen) {
                $.ajax({
                    url: '/setup/validate-fen?strict=0',
                    data: {
                        fen: fen
                    },
                    success: function(data) {
                        $board.html(data);
                        lichess.pubsub.emit('content_loaded');
                    },
                    error: function() {
                        alert('FEN 格式错误');
                        $board.empty();
                    }
                });
            }
        };

        register();
        resetName();
        remove();
    }

    function registerRecallGame() {
        let $md = $('.modal-recall');
        $md.find('.tabs-horiz span').click(function () {
            let $this = $(this);
            $md.find('.tabs-horiz span').removeClass("active");
            $md.find('.tabs-content div').removeClass("active");

            let cls = $this.attr('class');
            $this.addClass('active');
            $md.find('.tabs-content div.' + cls).addClass('active');
        });

        $md.find('form').submit(function(e) {
            e.preventDefault();

            let $form = $md.find('.form3');
            if(!$form.find('#form3-game').val() && !$form.find('#form3-pgn').val() && !$form.find('#form3-chapter').val()) {
                alert('输入一种PGN获取方式');
                return false;
            }

            $.ajax({
                method: 'POST',
                url: '/recall/pgn',
                data: $form.serialize()
            }).then(function(res) {
                let color = $form.find('#form3-color').val();
                let turns = $form.find('#form3-turns').val();
                let courseName = $('input[name=courseName]').val();
                let $html = $(recallHtml(res.fen, res.pgn, color, turns, courseName));
                $page.find('.recallGames table>tbody').append($html);
                $html.find('.mini-board').each(function () {
                    let $board = $(this);
                    Chessground(this, {
                        coordinates: false,
                        resizable: false,
                        drawable: { enabled: false, visible: false },
                        viewOnly: true,
                        fen: $board.data('fen')
                    });
                });
                //lichess.pubsub.emit('content_loaded');

                remove();
                resetName();
                $.modal.close();
            }, function (err) {
                handleError(err)
            });
            return false;
        });

        $md.find('input[type=file]').on('change', function() {
            let file = this.files[0];
            if (!file) return;
            let reader = new FileReader();
            reader.onload = function(e) {
                $md.find('textarea').val(e.target.result);
            };
            reader.readAsText(file);
        });

        function remove() {
            $page.find('.recallGames table>tbody .remove').click(function () {
                $(this).parents('tr').remove();
                resetName();
            });
        }

        function resetName() {
            $page.find('.recallGames table>tbody>tr').each(function (i, e) {
                $(this).find('input,textarea').each(function() {
                    let name = $(this).attr('name');
                    $(this).attr('name', name.replace(/recallGames\[\d+\]/g, 'recallGames[' + i + ']'));
                });
            });
        }

        function recallHtml(fen, pgn, color, turns, courseName) {

            let colorName = function () {
                if(color === 'white') return '白方';
                else if(color === 'black') return '黑方';
                else return '双方'
            };

            let turnsName = function () {
                if(!turns) return '所有';
                else return turns;
            };

            let colorHtml = function () {
                if(color !== 'all') return `<input type="hidden" name="practice.recallGames[0].color" value="${color}">`;
                else return ``;
            };

            let turnsHtml = function () {
                if(turns) return `<input type="hidden" name="practice.recallGames[0].turns" value="${turns}">`;
                else return ``;
            };

            let richPgn = function () {
               return pgn.replace(new RegExp('\n',"gm"), '<br/>');
            };

            return `<tr>
                       <td class="td-board">
                            <span class="mini-board cg-wrap parse-fen is2d" data-fen="${fen}"></span>
                       </td>
                       <td>
                            <div class="meta">
                                <span>棋色：${colorName()}</span>
                                &nbsp;&nbsp;
                                <span>回合数：${turnsName()}</span>
                            </div>
                            <div class="pgn">${richPgn()}</div>
                       </td>
                       <td>
                            <input type="hidden" name="practice.recallGames[0].root" value="${fen}">
                            <textarea class="none" name="practice.recallGames[0].pgn">${pgn}</textarea>
                            ${colorHtml()}
                            ${turnsHtml()}
                            <input type="hidden" name="practice.recallGames[0].title" value="${courseName}">
                            <a class="remove">移除</a>
                        </td>
                    </tr>`;
        }

        resetName();
        remove();
    }

    $('a.btn-position-add').click(function(e) {
        e.preventDefault();
        if($page.find('.fromPositions .tb>tbody>tr').length >= 5) {
            return false;
        }

        let addPosition = function() {
            let html = positionHtml();
            $page.find('.fromPositions .tb>tbody').append(html);
        };

        let positionHtml = function() {
            const initialFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
            return `<tr>
                       <td class="td-board"><span class="mini-board cg-wrap parse-fen is2d" data-fen="${initialFen}"></span></td>
                       <td>
                            <table class="form">
                                <tbody>
                                    <tr>
                                        <td>FEN</td>
                                        <td><input type="text" name="practice.fromPositions[0].fen" value="${initialFen}"></td>
                                    </tr>
                                    <tr>
                                        <td>初始时间</td>
                                        <td>
                                            <select name="practice.fromPositions[0].clockTime">
                                                <option value="0.0">0 分钟</option>
                                                <option value="0.25">¼ 分钟</option>
                                                <option value="0.5">½ 分钟</option>
                                                <option value="0.75">¾ 分钟</option>
                                                <option value="1.0">1 分钟</option>
                                                <option value="1.5">1.5 分钟</option>
                                                <option value="2.0">2 分钟</option>
                                                <option value="3.0">3 分钟</option>
                                                <option value="4.0">4 分钟</option>
                                                <option value="5.0" selected="selected">5 分钟</option>
                                                <option value="6.0">6 分钟</option>
                                                <option value="7.0">7 分钟</option>
                                                <option value="10.0">10 分钟</option>
                                                <option value="15.0">15 分钟</option>
                                                <option value="20.0">20 分钟</option>
                                                <option value="25.0">25 分钟</option>
                                                <option value="30.0">30 分钟</option>
                                                <option value="40.0">40 分钟</option>
                                                <option value="50.0">50 分钟</option>
                                                <option value="60.0">60 分钟</option>
                                            </select>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>时间增量</td>
                                        <td>
                                            <select name="practice.fromPositions[0].clockIncrement">
                                                <option value="0" selected="selected">0 秒</option>
                                                <option value="1">1 秒</option>
                                                <option value="2">2 秒</option>
                                                <option value="3">3 秒</option>
                                                <option value="4">4 秒</option>
                                                <option value="5">5 秒</option>
                                                <option value="6">6 秒</option>
                                                <option value="7">7 秒</option>
                                                <option value="10">10 秒</option>
                                                <option value="15">15 秒</option>
                                                <option value="20">20 秒</option>
                                                <option value="25">25 秒</option>
                                                <option value="30">30 秒</option>
                                                <option value="40">40 秒</option>
                                                <option value="50">50 秒</option>
                                                <option value="60">60 秒</option>
                                            </select>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>对局数</td>
                                        <td><input type="number" name="practice.fromPositions[0].num" value="1"></td>
                                    </tr>
                                </tbody>
                            </table>
                       </td>
                       <td><a class="remove">移除</a></td>
                    </tr>`;
        };

        addPosition();
        registerFromPosition();
        return false;
    });

    $('.goBack').click(function() {
        window.history.back();
    });

});

function handleError(res) {
    let json = res.responseJSON;
    if (json) {
        if (json.error) {
            if(typeof json.error === 'string') {
                alert(json.error);
            } else alert(JSON.stringify(json.error));
        } else alert(res.responseText);
    } else alert('发生错误');
}



