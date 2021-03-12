$(function() {

  let $root = $(".infinitescroll");
  let $form = $(".search_form");

  registerEvent();
  lichess.pubsub.on('content_loaded', registerEvent);

  $('select.select').change(function() {
    $root.find('.paginated').removeClass('selected');
    switch ($(this).val()) {
      case 'all':
        $root.find('.paginated').addClass('selected');
        break;
    }

  });
  $('select.action').change(function() {
    let $this = $(this);
    let action = $this.val();
    if (!action) return;

    let ids = [];
    $root.find('.selected').each(function() {
      return ids.push($(this).attr('data-id'));
    });

    if (ids.length === 0) return;
    if (action === 'delete') {
      if (confirm('删除 ' + ids.length + ' 个资源？')) {
        let url = $form.attr('action');
        url = url.substring(0, url.lastIndexOf("/")) + '/delete?ids=' + ids.join(',');
        $form.attr('action', url).attr('method', 'post');
        $form.submit();
      } else {
        $this.val('');
      }
    }

    else if (action === 'unlike') {
      if(confirm('取消关注 ' + ids.length + ' 个资源？')) {
        let url = $form.attr('action');
        url = url.substring(0, url.lastIndexOf("/")) + '/unlike?ids=' + ids.join(',');
        $form.attr('action', url).attr('method', 'post');
        $form.submit();
      }
    }

    else if (action === 'toCapsule') {
      showCapsule();
    }

  });

  $form.find(".tag-group input[type='checkbox']").click(function () {
    $form.submit()
  });
  $form.find("#form3-order").change(function () {
    $form.submit()
  });

});

function showCapsule() {
  let ids = [];
  $(".infinitescroll").find('.selected').each(function() {
    return ids.push($(this).attr('data-id'));
  });
  if(ids.length === 0) {
    alert('至少选择1项');
    return;
  }

  $.ajax({
    url: '/capsule/mine',
    success: function(html) {
      $.modal($(html));
      $('.cancel').click(function () {
        $('select.action').val('');
        $.modal.close();
      });

      $('.modal-capsule form').submit(function(e) {
        e.preventDefault();
        let $this = $(this);
        let capsuleId = $this.find('input[name="capsule"]:checked').val();
        if(!capsuleId) return false;

        $.ajax({
          method: 'POST',
          url: `/capsule/${capsuleId}/puzzle/add?ids=${ids.join(',')}`,
          success: function() {
            $.modal.close();
            location.reload();
          },
          error: function(res) {
            alert(res.responseText);
          }
        });
        return false;
      });
    },
    error: function(res) {
      alert(res.responseText);
    }
  });
}

function registerEvent() {
  let $board = $(".infinitescroll").find('.paginated');
  $board.off('click');
  $board.off('dblclick');
  $board.click(function(e) {
    e.preventDefault();
    $(this).toggleClass('selected');
    return false;
  });
  $board.dblclick(function(e) {
    e.preventDefault();
    window.open($(this).attr("data-href"));
    return false;
  });

}

$("#form3-province").change(function () {
  $("#form3-city").empty();
  let val = $(this).val();
  if (val) {
    $.get("/citys/" + val, function (res) {
      for (i = 0; i < res.length; i++) {
        $("#form3-city").append("<option value = '" + res[i].key + "'>" + res[i].name + "</option>");
      }
    });
  }
});
