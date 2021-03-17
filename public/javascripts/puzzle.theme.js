$(function () {

    $('.themeHistory .continue').click(function (e) {
        e.preventDefault();
        $.ajax({
            url: $(this).data('href'),
            success: function(response) {
                location.href = '/training/theme/' + response.lastId + '?' + response.uri;
            },
            error: function(res) {
                handleError(res);
            }
        });
        return false;
    });

    $('.themeHistory .restart').click(function (e) {
        e.preventDefault();
        $.ajax({
            url: $(this).data('href'),
            success: function(response) {
                location.href = '/training/theme/' + response.minId + '?' + response.uri;
            },
            error: function(res) {
                handleError(res);
            }
        });
        return false;
    });

    $('.themeHistory .remove').click(function (e) {
        e.preventDefault();
        let $this = $(this);
        if(confirm('确认删除？')) {
            $.ajax({
                method: 'post',
                url: $(this).data('href'),
                success: function() {
                    $this.parents('tr').remove();
                },
                error: function(res) {
                    handleError(res);
                }
            });
        }
        return false;
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