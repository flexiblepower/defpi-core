'use strict';

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

describe('FormController', function () {
    'use strict';

    var FormController = require('../../../../ng-admin/Crud/form/FormController'),
        Field = require('admin-config/lib/Field/Field'),
        Entity = require('admin-config/lib/Entity/Entity'),
        humane = require('humane-js');

    var $scope = void 0,
        $injector = void 0;
    beforeEach(inject(function ($controller, $rootScope, _$injector_) {
        $scope = $rootScope.$new();
        $injector = _$injector_;
    }));

    var entity = new Entity('post').identifier(new Field('id'));
    var $translate = function $translate(text) {
        return text;
    };
    var Configuration = function Configuration() {
        return {
            getErrorMessageFor: function getErrorMessageFor() {
                return '';
            }
        };
    };
    var $state = {
        go: jasmine.createSpy('$state.go'),
        get: jasmine.createSpy('$state.get').and.callFake(function (state) {
            return state;
        }),
        params: {},
        current: {
            name: 'list',
            params: {}
        }
    };
    var writeQueries = {
        deleteOne: jasmine.createSpy('writeQueries.deleteOne').and.callFake(function () {
            return $q.when();
        })
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
        entity: entity
    };
    var view = {
        title: function title() {
            return 'My view';
        },
        description: function description() {
            return 'Description';
        },
        actions: function actions() {
            return [];
        },
        getEntity: function getEntity() {
            return entity;
        },
        fields: function fields() {
            return [];
        },
        validate: function validate() {
            return true;
        },
        onSubmitError: function onSubmitError() {
            return function () {
                return true;
            };
        }
    };
    var entry = {
        transformToRest: function transformToRest() {}
    };
    var HttpErrorService = {
        handleError: jasmine.createSpy('HttpErrorService.handleError')
    };

    var previousState = {};

    var dataStore = {
        getFirstEntry: jasmine.createSpy('dataStore.getFirstEntry').and.callFake(function () {
            return entry;
        })
    };

    var $event = {
        preventDefault: function preventDefault() {}
    };

    describe('submitCreation', function () {
        describe('on error', function () {
            beforeEach(function () {
                entry = _extends({}, entry, {
                    values: {
                        id: 3
                    }
                });
                writeQueries = {
                    createOne: jasmine.createSpy('writeQueries.createOne').and.callFake(function () {
                        return Promise.reject("Here's a bad bad bad error");
                    })
                };
            });

            it('should call HttpErrorService handler', function (done) {
                // assume we are on post #3 deletion page
                var deletedId = 3;

                var formController = new FormController($scope, $state, $injector, $translate, previousState, writeQueries, Configuration, progression, notification, view, dataStore, HttpErrorService);

                formController.form = {
                    $valid: true
                };

                formController.submitCreation($event).then(function () {
                    assert.fail();
                    done();
                }).catch(function (error) {
                    expect(HttpErrorService.handleError.calls.argsFor(0)[5]).toBe("Here's a bad bad bad error");
                    done();
                });

                var fromStateParams = { entity: 'post', id: 3 };
                $scope.$emit('$stateChangeSuccess', {}, {}, {}, fromStateParams);

                $scope.$digest();
            });
        });
    });

    describe('submitEdition', function () {
        describe('on error', function () {

            beforeEach(function () {
                entry = _extends({}, entry, {
                    values: {
                        id: 3
                    }
                });
                writeQueries = {
                    updateOne: jasmine.createSpy('writeQueries.updateOne').and.callFake(function () {
                        return Promise.reject("Here's a bad bad bad error");
                    })
                };
            });

            it('should call HttpErrorService handler', function (done) {
                // assume we are on post #3 deletion page
                var deletedId = 3;

                var formController = new FormController($scope, $state, $injector, $translate, previousState, writeQueries, Configuration, progression, notification, view, dataStore, HttpErrorService);

                formController.form = {
                    $valid: true
                };

                formController.submitEdition($event).then(function () {
                    assert.fail();
                    done();
                }).catch(function (error) {
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
//# sourceMappingURL=FormControllerSpec.js.map