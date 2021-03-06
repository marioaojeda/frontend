define(function () {

    return function () {
        this.id = 'Viewability';
        this.start = '2015-06-15';
        this.expiry = '2015-08-01';
        this.author = 'Steve Vadocz';
        this.description = 'Viewability - Includes whole viewability package: ads lazy loading, sticky header, sticky MPU, spacefinder 2.0, dynamic ads, ad next to comments';
        this.audience = 0;
        this.audienceOffset = 0;
        this.successMeasure = '';
        this.audienceCriteria = '';
        this.dataLinkNames = '';
        this.idealOutcome = '';

        this.canRun = function () {
            return true;
        };

        this.variants = [
            {
                id: 'control',
                test: function () {}
            },
            {
                id: 'variant',
                test: function () {}
            }
        ];
    };

});
