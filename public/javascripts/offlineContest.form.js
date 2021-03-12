$(function () {

    let $form = $('.contest__form');

    $form.find('.single-uploader').find('input[type=\'file\']').ajaxSingleUpload({
        'name': 'logo',
        'action': '/offContest/imageUpload'
    });

    $form.find('#form3-typ').change(function () {
        let o = $form.find('#form3-organizer');
        let v = $(this).val();
        let d = $form.data(v);
        let teamId = getQueryVariable("team");

        o.empty();
        for (let i = 0; i < d.length; i++) {
            let checked = teamId && teamId === d[i].id;
            o.append("<option value = '" + d[i].id + "' " + (checked ? 'checked' : '') + " >" + d[i].name + "</option>");
        }
        o.trigger('change');
    });

    $form.find('#form3-typ').trigger('change');

    function getQueryVariable(variable) {
        let query = window.location.search.substring(1);
        let vars = query.split("&");
        for (let i = 0; i < vars.length; i++) {
            let pair = vars[i].split("=");
            if (pair[0] === variable) {
                return pair[1];
            }
        }
        return false;
    }

});


