$(function() {
  lichess.refreshInsightForm = function() {
    $('form.insight-refresh:not(.armed)').addClass('armed').submit(function() {
      $.modal($(this).find('.crunching'));
      $.post($(this).attr('action'), function() {
        lichess.reload();
      }).fail(function(err) {
        if(err.status === 406) {
          $.modal.close();
          setTimeout(function () {
            if($('body').data('user') === $('form.insight-refresh').data('user')) {
              window.lichess.memberIntro();
            } else alert('该用户未开通会员，无法使用');
          }, 200);
        }
      });
      return false;
    });
  };
  lichess.refreshInsightForm();
});
