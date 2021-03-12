(function ($) {
    $.fn.ajaxSingleFileUpload = function (options) {
        let settings = $.extend({
            action: '/',
            name: 'file'
        }, options);

        this.each(function () {
            let $file = $(this);
            let $uploader = $file.parent('.single-file');
            let $choose = $uploader.find('.choose');
            let $preview = $uploader.find('.preview');
            let $loader = $uploader.find('.loader');

            $file.click(function (event) {
                event.stopPropagation();
            });

            $file.change(function () {
                let file = this.files[0];
                let maxFileSize = 5 * 1024 * 1024;
                if (file.size >= maxFileSize) {
                    alert('文件过大');
                    $file.val('');
                    return;
                }
                upload(file);
            });

            function upload( file) {
                let formData = new FormData();
                formData.append(settings.name, file);
                $.ajax({
                    url: settings.action,
                    type: 'POST',
                    data: formData,
                    processData: false,
                    contentType: false,
                    beforeSend: function() {
                        $loader.removeClass('none');
                    },
                    complete: function() {
                        $loader.addClass('none');
                    },
                    success: function (response) {
                        if(response.ok) {
                            let path = response.path;
                            let pathArr = path.split('/');
                            let fileName = pathArr[pathArr.length - 1];
                            $file.next('input[type=\'hidden\']').val(path);
                            $preview.find('.name').attr('href', path).text(fileName);
                            $preview.removeClass('none');
                        } else {
                            alert(response.message);
                        }
                    },
                    error: function (e) {
                        if(e.status === 429) {
                            alert('请求次数太多，请稍后再试！')
                        } else {
                            alert('上传失败');
                        }
                    }
                });
            }

            $choose.click('click', function () {
                $file.trigger('click');
            });

            $preview.find('.remove').click('click', function () {
                $file.val('');
                $file.next('input[type=\'hidden\']').val('');
                $preview.find('.name').text('');
                $preview.addClass('none');
            });
        });
    };
})(jQuery);
