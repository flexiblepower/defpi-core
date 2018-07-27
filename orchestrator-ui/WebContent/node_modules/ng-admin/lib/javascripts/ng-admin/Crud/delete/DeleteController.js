'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

var DeleteController = function () {
    function DeleteController($scope, $window, $state, $q, $translate, WriteQueries, Configuration, progression, notification, params, view, entry, HttpErrorService) {
        var _this = this;

        _classCallCheck(this, DeleteController);

        this.$scope = $scope;
        this.$window = $window;
        this.$state = $state;
        this.$translate = $translate;
        this.WriteQueries = WriteQueries;
        this.config = Configuration();
        this.entityLabel = params.entity;
        this.entityId = params.id;
        this.view = view;
        this.title = view.title();
        this.description = view.description();
        this.actions = view.actions();
        this.entity = view.getEntity();
        this.progression = progression;
        this.notification = notification;
        this.$scope.entry = entry;
        this.$scope.view = view;
        this.HttpErrorService = HttpErrorService;

        $scope.$on('$destroy', this.destroy.bind(this));

        this.previousStateParametersDeferred = $q.defer();
        $scope.$on('$stateChangeSuccess', function (event, to, toParams, from, fromParams) {
            _this.previousStateParametersDeferred.resolve(fromParams);
        });
    }

    _createClass(DeleteController, [{
        key: 'deleteOne',
        value: function deleteOne($event) {
            var _this2 = this;

            return new Promise(function (resolve, reject) {
                var entityName = _this2.entity.name();
                var $translate = _this2.$translate,
                    notification = _this2.notification,
                    progression = _this2.progression;

                progression.start();

                _this2.previousStateParametersDeferred.promise.then(function (previousStateParameters) {
                    var fromState = 'delete';
                    var fromParams = previousStateParameters;
                    var toState = void 0;
                    var toParams = void 0;

                    // if previous page was related to deleted entity,
                    // redirect to list
                    if (fromParams.entity === entityName && fromParams.id === _this2.entityId) {
                        toState = _this2.$state.get('list');
                        toParams = _extends({
                            entity: entityName
                        }, _this2.$state.params);
                    }

                    return _this2.WriteQueries.deleteOne(_this2.view, _this2.entityId).then(function () {
                        if (toState) {
                            return _this2.$state.go(toState, toParams);
                        }
                        return _this2.back();
                    }).then(function () {
                        return $translate('DELETE_SUCCESS');
                    }).then(function (text) {
                        return notification.log(text, { addnCls: 'humane-flatty-success' });
                    }).then(function () {
                        resolve();
                    }).catch(function (error) {
                        progression.done();
                        _this2.HttpErrorService.handleError($event, toState, toParams, fromState, fromParams, error);
                        reject();
                    });
                });
            });
        }
    }, {
        key: 'back',
        value: function back() {
            this.$window.history.back();
        }
    }, {
        key: 'destroy',
        value: function destroy() {
            this.$scope = undefined;
            this.$window = undefined;
            this.$state = undefined;
            this.$translate = undefined;
            this.WriteQueries = undefined;
            this.view = undefined;
            this.entity = undefined;
            this.progression = undefined;
            this.notification = undefined;
        }
    }]);

    return DeleteController;
}();

exports.default = DeleteController;


DeleteController.$inject = ['$scope', '$window', '$state', '$q', '$translate', 'WriteQueries', 'NgAdminConfiguration', 'progression', 'notification', 'params', 'view', 'entry', 'HttpErrorService'];
module.exports = exports['default'];
//# sourceMappingURL=DeleteController.js.map