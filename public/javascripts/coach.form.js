$(function() {

  let $editor = $('.coach-edit');

  $editor.find('.tabs > div').click(function() {
    $editor.find('.tabs > div').removeClass('active');
    $(this).addClass('active');
    $editor.find('.panel').removeClass('active');
    let tab = $(this).data('tab');
    $editor.find('.panel.' + tab).addClass('active');
    location.hash = tab
  });
  setTabActive();

  function setTabActive() {
    let hash = location.hash;
    if(hash) {
      hash = hash.replace('#', '');
      $editor.find('.tabs > div').removeClass('active');
      $editor.find('.tabs > div[data-tab="' + hash + '"]').addClass('active');
      $editor.find('.panel').removeClass('active');
      $editor.find('.panel.' + hash).addClass('active');
    }
  }

  $editor.find('input[type=\'file\']').ajaxSingleUpload({
    'action': '/coach/upload'
  });

});
