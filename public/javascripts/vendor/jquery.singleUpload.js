(function ($) {
    $.fn.ajaxSingleUpload = function (options) {
        let settings = $.extend({
            action: '/',
            name: 'file'
        }, options);

        this.each(function () {
            let $file = $(this);
            let $uploader = $file.parent('.single-uploader');
            let $preview = $uploader.find('.preview');
            let $loader = $preview.find('.loading');

            $file.click(function (event) {
                event.stopPropagation();
            });

            $file.change(function () {
                let file = this.files[0];
                let maxFileSize = 3 * 1024 * 1024;
                if (file.size >= maxFileSize) {
                    alert('图片过大');
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
                        beginUpload();
                    },
                    complete: function() {
                        afterUpload()
                    },
                    success: function (response) {
                        if(response.ok) {
                            $file.next('input[type=\'hidden\']').val(response.path);
                            $preview.find('img').attr('src', '/image/' + response.path);
                            $preview.removeClass('none');
                            $preview.prev('.uploader').addClass('none');
                        } else {
                            alert(response.message);
                        }
                    },
                    error: function (e) {
                        if(e.status === 429) {
                            alert('请求太多，请稍后再试！')
                        } else {
                            alert('上传失败');
                        }
                        $uploader.off('click');
                    }
                });
            }

            function beginUpload() {
                $loader.removeClass('none');
                $uploader.off('click');
            }

            function afterUpload() {
                $loader.addClass('none');
                $uploader.on('click', trigger);
            }

            function trigger() {
                $file.trigger('click');
            }

            $uploader.on('click', trigger);
        });
    };
})(jQuery);
