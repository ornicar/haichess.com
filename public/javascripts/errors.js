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
      if (confirm('删除 ' + ids.length + ' 个错题？')) {
        let url = $form.attr('action');
        url = url.substring(0, url.lastIndexOf("/")) + '/delete?ids=' + ids.join(',');
        $form.attr('action', url).attr('method', 'post');
        $form.submit();
      } else {
        $this.val('');
      }
    }

  });

  $form.find(".tag-group input[type='checkbox']").click(function () {
    $form.submit()
  });
  $form.find("#form3-order").change(function () {
    $form.submit()
  });

});

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
    window.open($(this).data('href'));
    return false;
  });

}

