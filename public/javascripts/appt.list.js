$(function () {

    $('.apptlist a.accept').click(function(e) {
        e.preventDefault();
        if (confirm('确认接受？')) {
            $.ajax({
                method: 'POST',
                url: $(this).attr('href'),
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    alert(res.responseText);
                }
            });
        }
        return false;
    });

    $('.apptlist a.decline').click(function(e) {
        e.preventDefault();
        if (confirm('确认拒绝？')) {
            $.ajax({
                method: 'POST',
                url: $(this).attr('href'),
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    alert(res.responseText);
                }
            });
        }
        return false;
    });

    $('.apptlist a.cancel').click(function(e) {
        e.preventDefault();
        if (confirm('确认取消？')) {
            $.ajax({
                method: 'POST',
                url: $(this).attr('href'),
                success: function() {
                    location.reload();
                },
                error: function(res) {
                    alert(res.responseText);
                }
            });
        }
        return false;
    });

});


