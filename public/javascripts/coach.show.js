$(function() {

  $('.apply').click(function () {
    if(confirm('教练审核通过后，您将成为教练的学员，有权访问教练设置了学员权限的资源；同时，教练可以看到您的个人信息、动态、数据洞察和课后练等信息。')) {
      let coachId = $(this).data('id');
      $.post(`/coach/student/apply?coachId=${coachId}`).done(function () {
        location.reload();
      })
    }
  });


});
