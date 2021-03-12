$(function() {
    let $form = $('.orderForm');
    let defaultItem = lichess.memberBuy.default;
    let vmData = {
        productId: 'vmcGold',
        itemCode: defaultItem['vmcGold'],
        item: {},
        count: 1,
        price: 0.00,
        totalPrice: 0.00,
        priceAfterSilver: 0.00,
        afterPromotionPrice: 0.00,
        points: 0,
        maxPoints: 0,
        pointsAmount: 0.00,
        inviteUserAmount: 0.00,
        inviteUser: '',
        payPrice: 0.00
    };

    $form.find('input[name=productId]').change(function() {
        let $product = $(this);
        if($product.is(':checked')){
            vmData.productId = $product.val();
            renderItems();

            vmData.itemCode = defaultItem[vmData.productId];
            $(`#productItemCode_${vmData.itemCode}`.replace('@', '\\@'))
                .prop('checked',true)
                .trigger('change');
        }
    });
    registerItemChange();

    function registerItemChange() {
        $form.find('input[name=itemCode]').change(function() {
            let $item = $(this);
            if($item.is(':checked')){
                vmData.itemCode = $item.val();
                requestPrice();
            }
        });
    }

    $form.find('input[name=count]').blur(function() {
        if($form.find('input[name=count]').val() != vmData.count) {
            requestPrice();
        }
    });

    $form.find('input[name=points]').blur(function() {
        if($form.find('input[name=points]').val() != vmData.points) {
            requestPrice(true);
        }
    });

    $form.find('select[name=inviteUser]').blur(function() {
        if($form.find('select[name=inviteUser]').val() != vmData.inviteUser) {
            requestPrice();
        }
    });

    $form.keypress(function (e) {
        if(e.keyCode === 13) {
            return false;
        }
    });

    function renderItems() {
        let data = lichess.memberBuy;
        let products = data.products;
        let product = products[vmData.productId];
        let items = product.items;
        let html = `<div class="radio-group">`;
        Object.keys(items).forEach(function(itemCode) {
            let item = items[itemCode];
            html += `<span class="radio"><input id="productItemCode_${item.code}" type="radio" name="itemCode" value="${item.code}"><label class="radio-label" for="productItemCode_${item.code}">${item.name}</label></span>`;
        });
        html += `</div>`;
        $form.find('.items').html(html);

        registerItemChange();
    }

    function renderPrice() {
        $form.find('.price .number').text(vmData.afterPromotionPrice);
        $form.find('.price .del-price').text(vmData.priceAfterSilver);
        if(vmData.priceAfterSilver === vmData.afterPromotionPrice) {
            $form.find('.price del').addClass('none');
        } else {
            $form.find('.price del').removeClass('none');
        }
        $form.find('input[name=points]').val(vmData.points);
        $form.find('.inviteUser .minusAmount .number').text(vmData.inviteUserAmount ? vmData.inviteUserAmount : "0.00");
        $form.find('.payPrice .number').text(vmData.payPrice);
    }

    function requestPrice(isPointsChange = false) {
        $form.find(`div.formError`).text('');
        $form.find('.topay')
            .addClass('disabled')
            .prop('disabled', true);

        $.ajax({
            url: '/member/buy/calcPrice',
            type: 'post',
            data: {
                "productTyp": $form.find('input[name=productTyp]').val(),
                "productId": vmData.productId,
                "itemCode": vmData.itemCode,
                "count": vmData.count,
                "points": $form.find('input[name=points]').val(),
                "isPointsChange": isPointsChange,
                "inviteUser": $form.find('select[name=inviteUser]').val()
            },
            success: function(res) {
                vmData.count = res.count;
                vmData.price = res.price;
                vmData.totalPrice = res.totalPrice;
                vmData.priceAfterSilver = res.priceAfterSilver;
                vmData.afterPromotionPrice = res.afterPromotionPrice;
                vmData.points = res.points;
                vmData.maxPoints = res.maxPoints;
                vmData.pointsAmount = res.pointsAmount;
                vmData.inviteUserAmount = res.inviteUserAmount;
                vmData.inviteUser = res.inviteUser;
                vmData.payPrice = res.payPrice;
                renderPrice();

                $form.find('.topay')
                    .removeClass('disabled')
                    .prop('disabled', false);
            },
            error: function(err) {
                handleError(err);
            }
        });
    }

    function handleError(res) {
        let json = res.responseJSON;
        if (json) {
            if (json.error) {
                if(typeof json.error === 'string') {
                    alert(json.error);
                } else if(res.status === 400) {
                    for(let key in json.error) {
                        $(`.${key}Error`).text(json.error[key][0]);
                    }
                } else alert(JSON.stringify(json.error));
            } else alert(res.responseText);
        } else alert('发生错误');
    }

    $form.find('input[name=productId]').trigger('change');
});
