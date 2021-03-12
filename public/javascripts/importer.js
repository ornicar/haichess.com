$(function() {
  var $form = $("main.importer form");
  $form.submit(function() {
    setTimeout(function() { $form.html(lichess.spinnerHtml); }, 50);
  });

  if (window.FileReader) {
    $form.find('input[type=file]').on('change', function() {
      var file = this.files[0];
      if (!file) return;
      var reader = new FileReader();
      reader.onload = function(e) {
        $form.find('textarea').val(e.target.result);
      };
      reader.readAsText(file);
    });
  } else $form.find('.upload').remove();

  $(".importer").find('.tabs > div').click(function() {
    $(".importer").find('.tabs > div').removeClass('active');
    $(this).addClass('active');
    $(".importer").find('.panel').removeClass('active');
    $(".importer").find('.panel.' + $(this).data('tab')).addClass('active');
  });

  $("#form3-gameTag").tagsInput({
    "height": "40px",
    "width": "100%",
    "interactive": true,
    "defaultText": "添加",
    "removeWithBackspace": true,
    "minChars": 0,
    "maxChars": 10,
    "placeholderColor": "#666666"
  });

  $("#form3-puzzleTag").tagsInput({
    "height": "40px",
    "width": "100%",
    "interactive": true,
    "defaultText": "添加",
    "removeWithBackspace": true,
    "minChars": 0,
    "maxChars": 10,
    "placeholderColor": "#666666"
  });
});
