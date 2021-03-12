$(function () {
    $('.record a.accept').click(function(e) {
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

});


