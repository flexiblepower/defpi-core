'use strict';

var _ListLayoutController = require('../../../../ng-admin/Crud/list/ListLayoutController');

var _ListLayoutController2 = _interopRequireDefault(_ListLayoutController);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

describe('ListLayoutController', function () {
    describe('constructor', function () {
        it('should update filters if initialized with any', function () {
            spyOn(_ListLayoutController2.default.prototype, 'updateFilters');
            spyOn(_ListLayoutController2.default, 'getCurrentSearchParam').and.returnValue({});

            var $scope = {
                $watch: function $watch() {},
                $on: function $on() {}
            };

            var $location = {
                path: function path() {
                    return '/my_entity';
                },
                search: function search() {
                    return '';
                }
            };

            var view = {
                getEntity: function getEntity() {
                    return 'my_entity';
                },
                batchActions: function batchActions() {
                    return [];
                },
                actions: function actions() {
                    return [];
                },
                filters: function filters() {
                    return [{
                        my_column: 17,
                        pinned: function pinned() {
                            return true;
                        }
                    }];
                }
            };

            var listLayoutController = new _ListLayoutController2.default($scope, null, null, $location, null, view, null);

            expect(_ListLayoutController2.default.prototype.updateFilters).toHaveBeenCalled();
        });
    });

    describe('getCurrentSearchParam', function () {
        it('should return search url parameter mapped by filter', function () {
            var location = {
                search: function search() {
                    return { search: JSON.stringify({ name: 'doe' }) };
                }
            };

            var filters = [{ pinned: function pinned() {
                    return false;
                }, name: function name() {
                    return 'name';
                }, getMappedValue: function getMappedValue(value) {
                    return 'mapped name for ' + value;
                } }, { pinned: function pinned() {
                    return false;
                }, name: function name() {
                    return 'firstname';
                }, getMappedValue: function getMappedValue(value) {
                    return 'mapped firstname for ' + value;
                } }];

            expect((0, _ListLayoutController.getCurrentSearchParam)(location, filters)).toEqual({ name: 'mapped name for doe' });
        });

        it('should ignore pinned filter if location search has already a corresponding value', function () {
            var location = {
                search: function search() {
                    return { search: JSON.stringify({ name: 'doe', firstname: 'john' }) };
                }
            };

            var filters = [{
                pinned: function pinned() {
                    return false;
                },
                name: function name() {
                    return 'name';
                },
                getMappedValue: function getMappedValue(value) {
                    return 'mapped name for ' + value;
                }
            }, {
                pinned: function pinned() {
                    return true;
                },
                name: function name() {
                    return 'firstname';
                },
                getMappedValue: function getMappedValue(value) {
                    return 'mapped firstname for ' + value;
                },
                defaultValue: function defaultValue(value) {
                    return 'mapped firstname for default value for firstname';
                }
            }];

            expect((0, _ListLayoutController.getCurrentSearchParam)(location, filters)).toEqual({ name: 'mapped name for doe', firstname: 'mapped firstname for john' });
        });
    });
}); /*global describe,it,expect,beforeEach*/
//# sourceMappingURL=ListLayoutControllerSpec.js.map