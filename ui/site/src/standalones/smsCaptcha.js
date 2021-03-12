$(function() {

  let $captcha = $('form .smsCaptcha');
  let $cellphone = $captcha.find('#form3-cellphone');
  let $send = $captcha.find('.send');
  const cellphoneReg = /^(13[0-9]|14[5-9]|15[0-3,5-9]|16[2,5,6,7]|17[0-8]|18[0-9]|19[1,3,5,8,9])\d{8}$/;

  let __sms_sending__ = false;

  changeState();
  $cellphone.on('input propertychange', function(){
    changeState();
  });

  $send.click(function () {
    $cellphone.next('.error').empty();
    let repeatValid = $captcha.parents('form').data('sms-repeat-valid');
    $.ajax({
      method: 'POST',
      url: '/sendSmsCode?repeatValid=' + repeatValid,
      data: {
        'cellphone': $('#form3-cellphone').val(),
        'template': $('#form3-template').val()
      },
      success: function (response) {
        if(response.ok) {
          __sms_sending__ = true;
          startSendClock();
          waitInput();
        }
      }
    }).fail(function(error) {
      if(error.status === 400) {
        let messages = error.responseJSON.error.cellphone;
        if ($cellphone.next('.error').length == 1) {
          $cellphone.next('.error').text(messages[0]);
        } else {
          $cellphone.after('<p class="error">' + messages[0] + '</p>')
        }
      } else if(error.status == 429) {
        if ($cellphone.next('.error').length == 1) {
          $cellphone.next('.error').text('发送频率超过限制，请稍后再试！');
        } else {
          $cellphone.after('<p class="error">发送频率超过限制，请稍后再试！</p>')
        }
      } else {
        alert(JSON.stringify(error.responseJSON.error))
      }
    });
  });

  function changeState() {
    if(cellphoneReg.test($cellphone.val())){
      if(!__sms_sending__) {
        $send.prop('disabled', false).removeClass('disabled');
      }
    } else {
      $send.prop('disabled', true).addClass('disabled');
    }
  }

  function startSendClock() {
    $captcha.find('.clock').removeData('clock');
    $captcha.find('.clock').clock({
      time: 60,
      stopped: function (clock) {
        __sms_sending__ = false;
        clock = null;
        reSend();
      }
    });
  }
  
  function waitInput() {
    $send.prop('disabled', true).addClass('disabled');
    $send.find('.dosend').addClass('none');
    $send.find('.clock').removeClass('none');
    $send.find('.resend').removeClass('none');
  }

  function reSend() {
    $send.prop('disabled', false).removeClass('disabled');
    $send.find('.clock').addClass('none');
    $send.find('.resend').addClass('none');
    $send.find('.dosend').removeClass('none');
  }

});

