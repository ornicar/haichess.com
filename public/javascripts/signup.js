$(function() {
  var $form = $('.signup_box form');
  var $exists = $form.find('.username .exists');
  var runCheck = lichess.debounce(function() {
    var name = $username.val();
    if (name.length >= 3) $.ajax({
      method: 'GET',
      url: '/player/autocomplete',
      data: {
        term: name,
        exists: 1
      },
      success: function(res) {
        $exists.toggle(res);
      }
    });
  }, 300);
  $username = $form.find('input#username')
    .on('change keyup paste', function() {
      $exists.hide();
      runCheck();
    });

    $form.on('submit', function() {
      $form.find('button.submit')
        .attr('disabled', true)
        .removeAttr('data-icon')
        .addClass('frameless')
        .html(lichess.spinnerHtml);
    });

  $('#signup_form input[name=agree]').change(function () {
    if($(this).is(':checked')){
      $('#signup_form button[type=submit]').prop('disabled', false).removeClass('disabled');
    } else {
      $('#signup_form button[type=submit]').prop('disabled', true).addClass('disabled');
    }
  });
});

window.signupSubmit = function(token) {
  document.getElementById("signup_form").submit();
}
