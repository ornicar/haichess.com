$(function() {

  $('.stu-approve').click(function () {
    let url = $(this).data('href');
    $.post(url).done(function () {
      location.reload();
    })
  });

  $('.stu-decline').click(function () {
    let url = $(this).data('href');
    $.post(url).done(function () {
      location.reload();
    })
  });

  $('.stu-remove').click(function () {
    if(confirm('确认删除？')) {
      let url = $(this).data('href');
      $.post(url).done(function () {
        location.reload();
      })
    }
  });

});
