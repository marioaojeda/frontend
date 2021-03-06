define([
    'common/utils/$',
    'qwery',
    'bonzo',
    'bean',
    'common/utils/_',
    'fastdom',
    'common/utils/config',
    'common/utils/mediator',
    'common/utils/template',
    'common/modules/identity/api',
    'common/views/svgs',
    'text!common/views/save-for-later/delete-all-button.html'
], function (
    $,
    qwery,
    bonzo,
    bean,
    _,
    fastdom,
    config,
    mediator,
    template,
    identity,
    svgs,
    deleteButtonAllTmp
) {
    function SavedForLater() {

        this.init = function () {
            var self = this,
                deleteAll = $('.js-save-for-later__delete-all')[0];

            if (deleteAll) {
                this.renderDeleteButton('delete');
                bean.one(deleteAll, 'click', '.js-save-for-later__button', function (event) {
                    event.preventDefault();
                    self.renderDeleteButton('confirm');
                });
            }
        };

        this.renderDeleteButton = function (state) {
            fastdom.read(function () {
                var $button = bonzo(qwery('.js-save-for-later__delete-all')[0]);

                fastdom.write(function () {
                    $button.html(template(deleteButtonAllTmp, {
                        icon: svgs('crossIcon'),
                        state: state
                    }));
                });
            });
            if (state === 'confirm') {
                setTimeout(this.init.bind(this), 2000);
            }
        };
    }

    return SavedForLater;
});
