define([
    'common/modules/user-prefs',
    'common/utils/ajax',
    'common/utils/config',
    'common/utils/cookies'
], function (
    prefs,
    ajax,
    config,
    cookies
) {

    /**
     * Singleton to deal with Discussion API requests
     * @type {Object}
     */
    var Api = {
        root: (document.location.protocol === 'https:')
                ? config.page.secureDiscussionApiRoot
                : config.page.discussionApiRoot,
        proxyRoot: (prefs.isOn('discussion.useProxy') ? 'http://www.theguardian.com/guardianapis/discussion/discussion-api' : config.page.discussionApiRoot),
        clientHeader: config.page.discussionApiClientHeader
    };

    /**
     * @param {string} endpoint
     * @param {string} method
     * @param {Object.<string.*>} data
     * @param {boolean} proxy use a non cross domain proxy to avoid needing CORS which is blocked by http1 proxies
     * @return {Reqwest} a promise
     */
    Api.send = function (endpoint, method, data, proxy) {
        var root = (proxy || false) ? Api.proxyRoot : Api.root;
        data = data || {};
        if (cookies.get('GU_U')) {
            data.GU_U = cookies.get('GU_U');
        }

        var request = ajax({
            url: root + endpoint,
            type: (method === 'get') ? 'jsonp' : 'json',
            method: method,
            crossOrigin: true,
            data: data,
            headers: {
                'D2-X-UID': 'zHoBy6HNKsk',
                'GU-Client': Api.clientHeader
            }
        });

        return request;
    };

    /**
     * @param {string} discussionId
     * @param {Object.<string.*>} comment
     * @return {Reqwest} a promise
     */
    Api.postComment = function (discussionId, comment) {
        var endpoint = '/discussion/' + discussionId + '/comment' +
            (comment.replyTo ? '/' + comment.replyTo.commentId + '/reply' : '');

        return Api.send(endpoint, 'post', comment, true);
    };

    /**
     * @param {string} comment
     * @return {Reqwest} a promise
     */
    Api.previewComment = function (comment) {
        var endpoint = '/comment/preview';
        return Api.send(endpoint, 'post', comment);
    };

    /**
     * @param {number} id the comment ID
     * @return {Reqwest} a promise
     */
    Api.recommendComment = function (id) {
        var endpoint = '/comment/' + id + '/recommend';
        return Api.send(endpoint, 'post');
    };

    /**
     * @param {number} id the comment ID
     * @return {Reqwest} a promise
     */
    Api.pickComment = function (id) {
        var endpoint = '/comment/' + id + '/highlight';
        return Api.send(endpoint, 'post');
    };

    /**
     * @param {number} id the comment ID
     * @return {Reqwest} a promise
     */
    Api.unPickComment = function (id) {
        var endpoint = '/comment/' + id + '/unhighlight';
        return Api.send(endpoint, 'post');
    };

    /**
     * @param {number} id the comment ID
     * @param {Object.<string.string>} report the report info in the form of:
              { reason: string, emailAddress: string, categoryId: number }
     * @return {Reqwest} a promise
     */
    Api.reportComment = function (id, report) {
        var endpoint = '/comment/' + id + '/reportAbuse';
        return Api.send(endpoint, 'post', report);
    };

    /**
     * The id here is optional, but you shoudl try to specify it
     * If it isn't we use profile/me, which isn't as cachable
     * @param {number=} id (optional)
     */
    Api.getUser = function (id) {
        var endpoint = '/profile/' + (!id ? 'me' : id);
        return Api.send(endpoint, 'get');
    };

    return Api;

});
