import Promise from 'Promise';
import mockjax from 'test/utils/mockjax';
import tick from 'test/utils/tick';

export default function(mockCollection, action) {
    return new Promise(function (resolve) {
        var lastRequest, desiredAnswer;
        var interceptor = mockjax({
            url: '/edits',
            response: function (request) {
                lastRequest = request;
                lastRequest.data = JSON.parse(request.data);
                this.responseText = desiredAnswer;
            },
            onAfterComplete: function () {
                mockjax.clear(interceptor);
                tick(100).then(() => tick(100)).then(() => {
                    resolve(lastRequest);
                });
            }
        });
        desiredAnswer = action();

        // Soon after an edit, there'll be a call to refresh the collection
        for (var name in desiredAnswer) {
            desiredAnswer[name].lastUpdated = (new Date()).toISOString();
        }
        mockCollection.set(desiredAnswer);

        // This action triggers a network request, advance time
        tick(100).then(() => tick(100));
    });
}
