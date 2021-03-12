$(function() {
  showQRCode()
});

function showQRCode() {
  let url = $('#alipayCertifyUrl').val();
  if(url) {
    new QRCode(document.getElementById('qrcode'), {
      text: url,
      width: 300,
      height: 300,
      colorDark : '#000000',
      colorLight : '#ffffff',
      correctLevel: QRCode.CorrectLevel.L
    });
  }

}
