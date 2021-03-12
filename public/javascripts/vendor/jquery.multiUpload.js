(function ($) {
    $.fn.ajaxMultiUpload = function (options) {
        let settings = $.extend({
            action: '/',
            fileName: 'file',
            formName: 'file',
            maxLength: 5
        }, options);

        this.each(function () {
            let $file = $(this);
            let $multiUploader = $file.parents('.multi-uploader');
            let $uploader = $multiUploader.find('.uploader');
            let $previewList = $multiUploader.find('.preview-list');
            let $loader = $uploader.find('.loading');

            $file.click(function (event) {
                event.stopPropagation();
            });

            $previewList.find('.remove').click(function () {
                $(this).parent('.preview').remove();
            });

            $file.change(function () {
                if ($previewList.find('.preview').length > settings.maxLength - 1) {
                    alert('最多上传数量：' + settings.maxLength);
                    return;
                }

                let file = this.files[0];
                let maxFileSize = 3 * 1024 * 1024;
                if (file.size >= maxFileSize) {
                    alert('图片过大');
                    $file.val('');
                    return;
                }
                upload(file);
            });

            function upload(file) {
                let formData = new FormData();
                formData.append(settings.fileName, file);
                $.ajax({
                    url: settings.action,
                    type: 'POST',
                    data: formData,
                    processData: false,
                    contentType: false,
                    beforeSend: function () {
                        beginUpload();
                    },
                    complete: function () {
                        afterUpload()
                    },
                    success: function (response) {
                        if (response.ok) {
                            appendPreview(response.path);
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

            function appendPreview(path) {
                let maxIndex = 0;
                $previewList.find('.preview').each(function (i, e) {
                    let id = $(this).attr('data-id');
                    if (parseInt(id) > maxIndex) {
                        maxIndex = id;
                    }
                });
                let next = parseInt(maxIndex) + 1;
                let $preview =
                    $('<div class="preview" data-id="' + next + '">' +
                        '<a class="remove" data-icon="L" title="删除"></a>' +
                        '<img src="/image/' + path + '">' +
                        '<input type="hidden" name="' + settings.formName + '[' + next + ']" value="' + path + '">' +
                     '</div>');
                $previewList.append($preview);
                $preview.find('.remove').click(function () {
                    $(this).parent('.preview').remove();
                });
            }

            $uploader.on('click', trigger);
        });
    };
})(jQuery);
