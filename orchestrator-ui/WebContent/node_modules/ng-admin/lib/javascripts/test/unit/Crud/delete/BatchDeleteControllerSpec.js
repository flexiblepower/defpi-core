'use strict';

describe('BatchDeleteController', function () {
    'use strict';

    var BatchDeleteController = require('../../../../ng-admin/Crud/delete/BatchDeleteController'),
        Entity = require('admin-config/lib/Entity/Entity'),
        humane = require('humane-js');

    var $scope = void 0;
    beforeEach(inject(function ($controller, $rootScope) {
        $scope = $rootScope.$new();
    }));

    describe('batchDelete', function () {
        var $translate = function $translate(text) {
            return text;
        };
        var $state = {
            current: {
                name: 'list',
                params: {}
            },
            go: jasmine.createSpy('$state.go'),
            get: jasmine.createSpy('$state.get').and.callFake(function (state) {
                return state;
            }),
            params: {}
        };
        var progression = {
            start: function start() {
                return true;
            },
            done: function done() {
                return true;
            }
        };
        var notification = humane;
        var params = {
            id: 3,
            entity: new Entity('post')
        };
        var HttpErrorService = {
            handleError: jasmine.createSpy('HttpErrorService.handleError')
        };
        var writeQueries = void 0;
        describe('on error', function () {
            writeQueries = {
                batchDelete: jasmine.createSpy('writeQueries.deleteOne').and.callFake(function () {
                    return Promise.reject("Here's a bad bad bad error");
                })
            };

            it('should call HttpErrorService handler', function (done) {
                // assume we are on post #3 deletion page
                var entity = new Entity('post');
                var deletedId = 3;
                var view = {
                    title: function title() {
                        return 'Deleting a post';
                    },
                    description: function description() {
                        return 'Remove a post';
                    },
                    actions: function actions() {
                        return [];
                    },
                    getEntity: function getEntity() {
                        return entity;
                    },
                    fields: function fields() {
                        return [];
                    }
                };

                var batchDeleteController = new BatchDeleteController($scope, $state, $translate, writeQueries, progression, notification, view, HttpErrorService);

                batchDeleteController.batchDelete(view, 3).then(function () {
                    assert.fail();
                    done();
                }).catch(function () {
                    expect(HttpErrorService.handleError.calls.argsFor(0)[5]).toBe("Here's a bad bad bad error");
                    done();
                });

                var fromStateParams = { entity: 'post', id: 3 };
                $scope.$emit('$stateChangeSuccess', {}, {}, {}, fromStateParams);

                $scope.$digest();
            });
        });
    });
});
//# sourceMappingURL=BatchDeleteControllerSpec.js.map