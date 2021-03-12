$(function () {
    $("#form3-province").change(function () {
        $("#form3-city").empty();
        let val = $(this).val();
        if (val) {
            $.get("/citys/" + val, function (res) {
                for (i = 0; i < res.length; i++) {
                    $("#form3-city").append("<option value = '" + res[i].key + "'>" + res[i].name + "</option>");
                }
            });
        }
    });
});