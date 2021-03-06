import _ from 'underscore';
import Promise from 'Promise';
import sinon from 'sinon';
import mockjax from 'test/utils/mockjax';
import MockLastModified from 'mock/lastmodified';
import {CONST} from 'modules/vars';
import mediator from 'utils/mediator';
import presser from 'utils/presser';

describe('Presser', function () {
    var ajax, mockPress, events,
        originalSetTimeout = setTimeout,
        tick = function (ms) {
            jasmine.clock().tick(ms);
            return new Promise(function (resolve) {
                originalSetTimeout(function () {
                    resolve();
                }, 10);
            });
        };

    beforeEach(function () {
        jasmine.clock().install();
        ajax = sinon.spy();
        events = sinon.spy();

        mockPress = mockjax({
            url: /\/press\/([a-z]+)\/(.+)/,
            urlParams: ['env', 'front'],
            response: function (request) {
                ajax(request.urlParams.env, request.urlParams.front);
                this.responseText = {};
            }
        });
        this.mockLastModified = new MockLastModified();

        mediator.on('presser:lastupdate', events);
    });

    afterEach(function () {
        ajax = null;
        mockjax.clear(mockPress);
        jasmine.clock().uninstall();
        mediator.off('presser:lastupdate', events);
        this.mockLastModified.dispose();
        events = null;
    });

    it('presses draft', function () {
        presser('draft', 'front/name');
        jasmine.clock().tick(100);
        expect(ajax.getCall(0).args).toEqual(['draft', 'front/name']);

        jasmine.clock().tick(CONST.detectPressFailureMs + 10);
        expect(events.called).toBe(false);
    });

    it('presses live successfully', function (done) {
        // debounce uses the actual now() method, mock that one too
        var originalNow = _.now,
            currentTime = originalNow();

        _.now = function () {
            return currentTime;
        };
        presser('live', 'cool/front').
        then(() => {
            expect(ajax.getCall(0).args).toEqual(['live', 'cool/front']);

            var now = new Date();
            this.mockLastModified.set({
                'cool/front': now.toISOString()
            });
            currentTime += CONST.detectPressFailureMs + 10;
            tick(CONST.detectPressFailureMs)
            .then(() => tick(100))
            .then(() => {
                _.now = originalNow;
                expect(events.getCall(0).args).toEqual(['cool/front', now]);
                done();
            });
        });
        jasmine.clock().tick(100);
    });
});
