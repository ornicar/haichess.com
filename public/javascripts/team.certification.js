$(function() {

  $('.single-uploader').find('input[type=\'file\']').ajaxSingleUpload({
    'action': '/team/upload'
  });

});