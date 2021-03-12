$(function() {

  $('.single-uploader').find('input[type=\'file\']').ajaxSingleUpload({
    'action': '/team/upload'
  });

  $('.multi-uploader').find('input[type=\'file\']').ajaxMultiUpload({
    'action': '/team/upload',
    'fileName': 'file',
    'formName': 'envPicture'
  });


});