import _ from 'underscore';
import * as contentApi from 'modules/content-api';
import MockSearch from 'mock/search';
import mockjax from 'test/utils/mockjax';

describe('Latest articles', function() {
    beforeEach(function () {
        this.mock = new MockSearch();
    });
    afterEach(function () {
        this.mock.dispose();
    });

    it('whole list should filter out articles', function (done) {
        this.mock.latest([{
            fields: {
                headline: 'This has an header'
            }
        }, {
            not: 'This doesn\'t, filter out'
        }]);

        contentApi.fetchLatest()
        .then(assert(this.mock, function (request, list) {
            expect(request.urlParams.page).toBe('1');
            expect(request.urlParams['content-set']).toBe('-web-live');
            expect(request.urlParams['order-by']).toBe('oldest');
            expect(request.urlParams['page-size']).toBe('50');
            expect(request.urlParams.q).toBeUndefined();

            expect(list.results).toEqual([{
                fields: {
                    headline: 'This has an header'
                }
            }]);

            done();
        }));
    });

    it('filter out based on a search term', function (done) {
        this.mock.latest([{
            fields: {
                headline: 'Filtering results'
            }
        }]);

        contentApi.fetchLatest({
            isDraft: false,
            term: 'one',
            filterType: 'author',
            filter: 'me'
        })
        .then(assert(this.mock, function (request, list) {
            expect(request.urlParams.page).toBe('1');
            expect(request.urlParams['content-set']).toBe('web-live');
            expect(request.urlParams['order-by']).toBe('newest');
            expect(request.urlParams['page-size']).toBe('50');
            expect(request.urlParams.q).toBe('one');
            expect(request.urlParams.author).toBe('me');

            expect(list.results).toEqual([{
                fields: {
                    headline: 'Filtering results'
                }
            }]);

            done();
        }));
    });

    it('all results empty response', function (done) {
        this.mock.latest([]);

        contentApi.fetchLatest()
        .then(done.fail, assert(this.mock, function (request, list) {
            expect(list instanceof Error).toBe(true);
            expect(list.message).toMatch(/not currently returning content/i);

            done();
        }));
    });

    it('searches an article', function (done) {
        var mockId = mockjax({
            url: /\/api\/proxy\/uk-news\/important\/stuff\?(.+)/,
            responseText: {
                status: 'ok',
                response: {
                    content: [{
                        fields: {
                            headline: 'Single article'
                        }
                    }]
                }
            }
        });

        contentApi.fetchLatest({
            article: 'uk-news/important/stuff'
        })
        .then(function (list) {
            expect(list.results).toEqual([{
                fields: {
                    headline: 'Single article'
                }
            }]);

            mockjax.clear(mockId);
            done();
        });
    });

    it('network fail', function (done) {
        var mockId = mockjax({
            url: /\/api\/proxy\/uk-news\/less\/important.*/,
            responseText: 'Server error',
            status: 500
        });

        contentApi.fetchLatest({
            article: 'uk-news/less/important',
            term: 'ignored'
        })
        .then(done.fail, function (error) {
            expect(error instanceof Error).toBe(true);
            expect(error.message).toMatch(/Content API error/i);

            mockjax.clear(mockId);
            done();
        });
    });

    function assert (mock, what) {
        var promisedValue, lastRequest;
        var wait = _.after(2, function () {
            what(lastRequest, promisedValue);
        });

        mock.once('complete', function (request) {
            lastRequest = request;
            wait();
        });

        return function (value) {
            promisedValue = value;
            wait();
        };
    }
});
