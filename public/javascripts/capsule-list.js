$(function() {

  let $form = $(".search_form");

  $form.find(".tag-group input[type='checkbox']").click(function () {
    $form.submit();
  });

  $form.find("#form3-enabled").change(function () {
    $form.submit();
  });

});
